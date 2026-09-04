"""
This handles the core logic for the task-switching frequency endpoint

A switch s counted whenever two consecutive time entries for the same member,
its ordered by the start time, has different task ids. its a simple sequential scan definition.

Author: Zamokuhle Zwane
Date: 21/08/2026
"""

import uuid
from datetime import date, datetime

from sqlalchemy import and_
from sqlalchemy.orm import Session

from app.models.ai_insight import AIInsight
from app.models.time_entry import TimeEntry


def calculate_task_switching(
    db: Session,
    workspace_member_id: uuid.UUID,
    period_start: date,
    period_end: date,
    project_id: uuid.UUID | None = None,
) -> dict:
    # project_id computes across every project, it scopes to just that project
    filters = [
        TimeEntry.workspace_member_id == workspace_member_id,
        TimeEntry.start_time >= datetime.combine(period_start, datetime.min.time()),
        TimeEntry.start_time <= datetime.combine(period_end, datetime.max.time()),
        TimeEntry.is_deleted.is_(False),
        TimeEntry.task_id.isnot(None),
    ]
    if project_id is not None:
        filters.append(TimeEntry.project_id == project_id)

    entries = db.query(TimeEntry).filter(and_(*filters)).order_by(TimeEntry.start_time.asc()).all()

    if not entries:
        return {
            "switches_per_day": 0.0,
            "team_average": None,
            "description": "No qualifying time entries were found for this period.",
        }
    switches_per_day: dict[date, int] = {}
    previous_task_id = None
    previous_day = None

    for entry in entries:
        entry_day = entry.start_time.date()
        if (
            previous_task_id is not None
            and entry_day == previous_day
            and entry.task_id != previous_task_id
        ):
            switches_per_day[entry_day] = switches_per_day.get(entry_day, 0) + 1
        previous_task_id = entry.task_id
        previous_day = entry_day

    days_logged = (period_end - period_start).days + 1
    total_switches = sum(switches_per_day.values())
    switches_per_day = round(total_switches / days_logged, 2)

    team_average = _get_team_switches_per_day(
        db, workspace_member_id, period_start, period_end, project_id
    )

    return {
        "switches_per_day": switches_per_day,
        "team_average": team_average,
        "description": f"Averaged {switches_per_day} task switches per day over the period.",
    }


# Average switches per day across every other member of this person's workspace for the same period
def _get_team_switches_per_day(
    db: Session,
    workspace_member_id: uuid.UUID,
    period_start: date,
    period_end: date,
    project_id: uuid.UUID | None,
) -> float | None:
    from app.models.workspace_member import WorkspaceMember

    this_member = (
        db.query(WorkspaceMember).filter(WorkspaceMember.id == workspace_member_id).first()
    )
    if this_member is None:
        return None

    teammates = (
        db.query(WorkspaceMember.id)
        .filter(
            WorkspaceMember.workspace_id == this_member.workspace_id,
            WorkspaceMember.id != workspace_member_id,
        )
        .all()
    )
    teammate_ids = [row[0] for row in teammates]

    if not teammate_ids:
        return None

    totals = []
    for teammate_id in teammate_ids:
        result = calculate_task_switching(
            db, teammate_id, period_start, period_end, project_id=project_id
        )
        totals.append(result["switches_per_day"])

    return round(sum(totals) / len(totals), 2)


def save_task_switching_insight(
    db: Session,
    workspace_member_id: uuid.UUID,
    result: dict,
    project_id: uuid.UUID | None = None,
) -> AIInsight:
    insight = AIInsight(
        workspace_member_id=workspace_member_id,
        insight_type="TASK_SWITCHING",
        scope="USER",
        project_id=project_id,
        score=result["switches_per_day"],
        description=result["description"],
    )
    db.add(insight)
    db.commit()
    db.refresh(insight)
    return insight
