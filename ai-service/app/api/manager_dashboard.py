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
    rows = (
        db.execute(
            text(
                """
            SELECT project_id FROM project_members
            WHERE workspace_member_id = :member_id AND is_project_manager = true AND is_active = true
            """
            ),
            {"member_id": workspace_member_id},
        )
        .mappings()
        .all()
    )
    return [row["project_id"] for row in rows]


def _get_project_member_ids(db: Session, project_id: UUID) -> list[UUID]:
    rows = (
        db.execute(
            text(
                "SELECT workspace_member_id FROM project_members WHERE project_id = :project_id AND is_active = true"
            ),
            {"project_id": project_id},
        )
        .mappings()
        .all()
    )
    return [row["workspace_member_id"] for row in rows]


def _member_display_name(db: Session, user_id: UUID) -> str:
    user_row = (
        db.execute(
            text("SELECT first_name, last_name FROM users WHERE id = :user_id"),
            {"user_id": user_id},
        )
        .mappings()
        .first()
    )
    return f"{user_row['first_name']} {user_row['last_name']}" if user_row else "Unknown"


def _add_github_activity(
    db: Session,
    project_id: UUID,
    combined: dict[UUID, dict],
    start: datetime,
    end: datetime,
) -> None:
    # combined is keyed by member_id, so a member on several projects is only fetched once
    for member_id in _get_project_member_ids(db, project_id):
        if member_id in combined:
            continue
        member = db.query(WorkspaceMember).filter(WorkspaceMember.id == member_id).first()
        if member is None:
            continue
        activity = get_github_activity(db, member_id, start, end)
        combined[member_id] = {
            "workspace_member_id": member_id,
            "member_name": _member_display_name(db, member.user_id),
            "hours_logged": activity["hours_logged"],
            "commit_count": activity["commit_count"],
        }


def _add_hours(combined_hours: dict[str, float], hours_result: dict) -> None:
    for member in hours_result["members"]:
        name = member["member_name"]
        combined_hours[name] = combined_hours.get(name, 0.0) + member["hours"]


def _add_jira_types(combined_by_type: dict[str, int], jira_result: dict) -> None:
    for entry in jira_result["by_type"]:
        key = entry["issue_type"]
        combined_by_type[key] = combined_by_type.get(key, 0) + entry["count"]


def _build_hours_result(combined_hours: dict[str, float]) -> dict:
    members = [{"member_name": n, "hours": round(h, 1)} for n, h in combined_hours.items()]
    return {"members": sorted(members, key=lambda m: m["hours"], reverse=True)}


def _build_jira_combined(total: int, by_type: dict[str, int]) -> dict:
    return {
        "total": total,
        "by_type": [
            {
                "issue_type": issue_type,
                "count": count,
                "percentage": round((count / total) * 100, 1) if total > 0 else 0.0,
            }
            for issue_type, count in sorted(by_type.items(), key=lambda i: i[1], reverse=True)
        ],
    }


@router.get(
    "/manager-dashboard",
    response_model=ManagerDashboardResponse,
    responses={
        403: {
            "description": "Caller does not manage any projects, or does not manage the requested project."
        }
    },
)
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

    managed_rows = db.query(Project.id, Project.name).filter(Project.id.in_(managed_ids)).all()
    managed_projects = [
        ManagedProject(project_id=p_id, project_name=name) for p_id, name in managed_rows
    ]

    if project_id is not None:
        chart_project_id = project_id
        scoped_project_id = project_id
        all_project_ids = [project_id]
    else:
        chart_project_id = min(managed_projects, key=lambda p: p.project_name).project_id
        scoped_project_id = None  # None signals "All My Projects" to the frontend pill row
        all_project_ids = managed_ids

    period_end = date.today()
    period_start = period_end - timedelta(weeks=4)
    github_end = datetime.now(timezone.utc)
    github_start = github_end - timedelta(days=7)

    # team_logged_hours, burnout, github, jira genuinely aggregate across every managed project
    combined_hours: dict[str, float] = {}
    combined_burnout: list[FlaggedBurnoutMember] = []
    combined_github: dict[UUID, dict] = {}
    combined_jira_total = 0
    combined_jira_by_type: dict[str, int] = {}

    for p_id in all_project_ids:
        _add_hours(combined_hours, team_logged_hours(db, p_id, period_start, period_end))
        combined_burnout.extend(
            FlaggedBurnoutMember(**flagged) for flagged in flagged_burnout_members(db, p_id)
        )
        jira_result = jira_ticket_breakdown(db, p_id)
        combined_jira_total += jira_result["total"]
        _add_jira_types(combined_jira_by_type, jira_result)
        _add_github_activity(db, p_id, combined_github, github_start, github_end)

    return ManagerDashboardResponse(
        project_completion_forecast=project_completion_forecast(db, chart_project_id),
        team_logged_hours=_build_hours_result(combined_hours),
        flagged_burnout_members=combined_burnout,
        task_overview=task_overview(db, chart_project_id),
        velocity=velocity(db, chart_project_id),
        project_health=project_health(db, chart_project_id),
        github_activity_by_member=[GithubActivityByMember(**v) for v in combined_github.values()],
        jira_ticket_breakdown=_build_jira_combined(combined_jira_total, combined_jira_by_type),
        managed_projects=managed_projects,
        scoped_project_id=scoped_project_id,
        period=period,
    )
