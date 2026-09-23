"""
Project-level delivery forecasting service.

This service calculates a project forecast from existing Momently data:
- project budget and planned dates
- task estimates and completion
- actual logged time
- recent team velocity

This is a heuristic forecast, not a trained prediction model.

- the delivery forecast service

the service calculates:
    - how much time has been logged?
    - how much estimated work remains?
    - with the pace of the team, when will that work finish?
    - is the schedule at risk?

Author: Nyasha
Date: 22/09/2026
"""

import uuid
from datetime import date, datetime, timedelta, timezone

from sqlalchemy import func
from sqlalchemy.orm import Session

from app.models.project import Project
from app.models.project_member import ProjectMember
from app.models.task import Task
from app.models.time_entry import TimeEntry

# I am using the last 14 days of the teams activity
# choosing this window so we see how a team is doing for an extended period of time
LOOKBACK_DAYS = 14
MIN_WEEKLY_VELOCITY_HOURS = 1.0


def calculate_project_forecast(db: Session, project_id: uuid.UUID) -> dict | None:
    """
    - this is the main function, it is what will bring the project, its tasks, and calculate the different forecast sections
    """

    project = db.query(Project).filter(Project.id == project_id, Project.is_deleted.is_(False)).first()

    if project is None:
        return None

    tasks = db.query(Task).filter(Task.project_id == project_id, Task.is_deleted.is_(False)).all()

    used_hours = _get_used_hours(db, project_id)
    task_progress = _calculate_task_progress(tasks)
    weekly_velocity = _get_recent_team_velocity(db, project_id)

    forecast_end_date = _calculate_forecast_end_date(task_progress["estimated_remaining_hours"], weekly_velocity)

    budget = _calculate_budget_forecast(project, used_hours, task_progress["estimated_remaining_hours"])

    schedule = _calculate_schedule_forecast(project, forecast_end_date)

    risk = _calculate_risk(budget, schedule, task_progress)

    return {
        "project_id": project.id,
        "project_name": project.name,
        "budget": budget,
        "schedule": schedule,
        "tasks": task_progress,
        "velocity": {
            "recent_hours_per_week": round(weekly_velocity, 2),
            "lookback_days": LOOKBACK_DAYS,
        },
        "risk": risk,
    }


def _get_used_hours(db: Session, project_id: uuid.UUID) -> float:
    """
    - calculates the total number of hours logged for the project
    - using the time entries are used cause they have the actual time
    - I am making sure that time entries and entries without a duration should not be included
    """

    # adds together the duration of all valid time entries belonging to the project
    total_seconds = db.query(func.coalesce(func.sum(TimeEntry.duration_seconds), 0)).filter(
        TimeEntry.project_id == project_id,
        TimeEntry.is_deleted.is_(False),
        TimeEntry.duration_seconds.isnot(None),
    ).scalar()

    # duration stored in seconds so we divide by 3600, 
    return round(float(total_seconds) / 3600, 2)

