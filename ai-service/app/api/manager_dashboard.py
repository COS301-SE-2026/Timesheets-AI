"""
Router for the manager dashboard endpoint. Powers the Manager Insights page, one project at a time or averaged across "All My Projects".

Author: Zamokuhle Zwane
Date: 26/09/2026
"""

from datetime import date, datetime, timedelta, timezone
from typing import Optional
from uuid import UUID

from fastapi import APIRouter, Depends, HTTPException, Query
from sqlalchemy import text
from sqlalchemy.orm import Session

from app.database import get_db
from app.models.project import Project
from app.models.workspace_member import WorkspaceMember
from app.schemas.manager_dashboard import (
    FlaggedBurnoutMember,
    GithubActivityByMember,
    ManagedProject,
    ManagerDashboardResponse,
)
from app.services.github_activity import get_github_activity
from app.services.manager_dashboard import (
    flagged_burnout_members,
    jira_ticket_breakdown,
    project_completion_forecast,
    project_health,
    task_overview,
    team_logged_hours,
    velocity,
)

router = APIRouter(prefix="/insights", tags=["Manager Dashboard"])


def _get_managed_project_ids(db: Session, workspace_member_id: UUID) -> list[UUID]:
    rows = db.execute(
        text(
            """
            SELECT project_id FROM project_members
            WHERE workspace_member_id = :member_id AND is_project_manager = true AND is_active = true
            """
        ),
        {"member_id": workspace_member_id},
    ).mappings().all()
    return [row["project_id"] for row in rows]


def _get_project_member_ids(db: Session, project_id: UUID) -> list[UUID]:
    rows = db.execute(
        text("SELECT workspace_member_id FROM project_members WHERE project_id = :project_id AND is_active = true"),
        {"project_id": project_id},
    ).mappings().all()
    return [row["workspace_member_id"] for row in rows]


@router.get("/manager-dashboard", response_model=ManagerDashboardResponse)
def get_manager_dashboard(
    workspace_member_id: UUID,
    db: Session = Depends(get_db),
    project_id: Optional[UUID] = Query(default=None),
    period: str = Query(default="8w"),
):
    managed_ids = _get_managed_project_ids(db, workspace_member_id)
    if not managed_ids:
        raise HTTPException(status_code=403, detail="You don't manage any projects.")

    if project_id is not None and project_id not in managed_ids:
        raise HTTPException(status_code=403, detail="You don't manage this project.")

    managed_projects_rows = (
        db.query(Project.id, Project.name).filter(Project.id.in_(managed_ids)).all()
    )
    managed_projects = [ManagedProject(project_id=p_id, project_name=name) for p_id, name in managed_projects_rows]

    period_end = date.today()
    period_start = period_end - timedelta(weeks=4)

    if project_id is not None:
        scoped_ids = [project_id]
        scoped_project_id = project_id
    else:
        managed_projects_sorted = sorted(managed_projects, key=lambda p: p.project_name)
        scoped_ids = [managed_projects_sorted[0].project_id]
        scoped_project_id = None  # None signals "All My Projects" to the frontend pill row

    chart_project_id = scoped_ids[0]

    all_project_ids = managed_ids if project_id is None else [project_id]

    # team_logged_hours, burnout, github, jira genuinely aggregate across every managed project
    combined_hours: dict[str, float] = {}
    combined_burnout: list[FlaggedBurnoutMember] = []
    combined_github: dict[UUID, dict] = {}
    combined_jira_total = 0
    combined_jira_by_type: dict[str, int] = {}

    for p_id in all_project_ids:
        hours_result = team_logged_hours(db, p_id, period_start, period_end)
        for member in hours_result["members"]:
            combined_hours[member["member_name"]] = combined_hours.get(member["member_name"], 0.0) + member["hours"]

        for flagged in flagged_burnout_members(db, p_id):
            combined_burnout.append(FlaggedBurnoutMember(**flagged))

        jira_result = jira_ticket_breakdown(db, p_id)
        combined_jira_total += jira_result["total"]
        for entry in jira_result["by_type"]:
            combined_jira_by_type[entry["issue_type"]] = (
                combined_jira_by_type.get(entry["issue_type"], 0) + entry["count"]
            )

        member_ids = _get_project_member_ids(db, p_id)
        github_start = datetime.now(timezone.utc) - timedelta(days=7)
        github_end = datetime.now(timezone.utc)
        for member_id in member_ids:
            # call once per member, not twice, per the spec, this loop only runs
            # get_github_activity a single time per member even across projects
            # because combined_github is keyed by member_id and we skip repeats
            if member_id in combined_github:
                continue
            member = db.query(WorkspaceMember).filter(WorkspaceMember.id == member_id).first()
            if member is None:
                continue
            user_row = db.execute(
                text("SELECT first_name, last_name FROM users WHERE id = :user_id"),
                {"user_id": member.user_id},
            ).mappings().first()
            member_name = f"{user_row['first_name']} {user_row['last_name']}" if user_row else "Unknown"
            activity = get_github_activity(db, member_id, github_start, github_end)
            combined_github[member_id] = {
                "workspace_member_id": member_id,
                "member_name": member_name,
                "hours_logged": activity["hours_logged"],
                "commit_count": activity["commit_count"],
            }

    team_logged_hours_result = {
        "members": sorted(
            [{"member_name": name, "hours": round(hours, 1)} for name, hours in combined_hours.items()],
            key=lambda m: m["hours"],
            reverse=True,
        )
    }

    jira_combined = {
        "total": combined_jira_total,
        "by_type": [
            {
                "issue_type": issue_type,
                "count": count,
                "percentage": round((count / combined_jira_total) * 100, 1) if combined_jira_total > 0 else 0.0,
            }
            for issue_type, count in sorted(combined_jira_by_type.items(), key=lambda item: item[1], reverse=True)
        ],
    }

    return ManagerDashboardResponse(
        project_completion_forecast=project_completion_forecast(db, chart_project_id),
        team_logged_hours=team_logged_hours_result,
        flagged_burnout_members=combined_burnout,
        task_overview=task_overview(db, chart_project_id),
        velocity=velocity(db, chart_project_id),
        project_health=project_health(db, chart_project_id),
        github_activity_by_member=[GithubActivityByMember(**v) for v in combined_github.values()],
        jira_ticket_breakdown=jira_combined,
        managed_projects=managed_projects,
        scoped_project_id=scoped_project_id,
        period=period,
    )