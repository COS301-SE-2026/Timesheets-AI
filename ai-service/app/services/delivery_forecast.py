"""
This file handes the delivery forecast, it predicts a forecast date against the tasks plannedDate(due_date) it uses
linear velocity projection: hiw many estimated hours worth of work this person has actually closed per day
recentky appplied the remaining estimated hours on the task in question

also, this is a hueristic not a trained model
Author: Zamokuhle Zwane
Date: 26/08/2026
"""

import uuid
from datetime import date, datetime, timedelta

from sqlalchemy import and_
from sqlalchemy.orm import Session

from app.models.ai_insight import AIInsight
from app.models.task import Task

LOOKBACK_DAYS = 14  # 2 weeks
MIN_VELOCITY_HOURS_PER_DAY = 0.5  # floored, avoids divide-by-near-zero blowing up the forecast


def calculate_delivery_forecast(db: Session, task_id: uuid.UUID) -> dict:
    task = db.query(Task).filter(Task.id == task_id, Task.is_deleted.is_(False)).first()

    if task is None:
        return {
            "forecast_date": None,
            "planned_date": None,
            "description": "Task not found or deleted, forecast is not calculate.",
        }
    if task.estimated_hours is None or task.status == "DONE":
        return {
            "forecast_date": task.completed_at.date() if task.completed_at else None,
            "planned_date": task.due_date,
            "description": "Tasks already complete or has no estimate, no forecast needed.",
        }
    remaining_hours = float(task.estimated_hours) - float(task.actual_hours or 0)
    remaining_hours = max(remaining_hours, 0.0)

    velocity = _get_recent_velocity(db, task.assigned_workspace_member_id)
    velocity = max(velocity, MIN_VELOCITY_HOURS_PER_DAY)

    days_remaining = remaining_hours / velocity
    forecast_date = date.today() + timedelta(days=round(days_remaining))

    return {
        "forecast_date": forecast_date,
        "planned_date": task.due_date,
        "description": (
            f"At current pace ({velocity:.1f}h/day), this task is projected to finish "
            f"around {forecast_date.isoformat()}."
        ),
    }


def _get_recent_velocity(db: Session, workspace_member_id: uuid.UUID) -> float:
    since = datetime.utcnow() - timedelta(days=LOOKBACK_DAYS)

    complete_tasks = (
        db.query(Task)
        .filter(
            and_(
                Task.assigned_workspace_member_id == workspace_member_id,
                Task.status == "DONE",
                Task.completed_at.isnot(None),
                Task.completed_at >= since,
                Task.estimated_hours.isnot(None),
            )
        )
        .all()
    )

    if not complete_tasks:
        return MIN_VELOCITY_HOURS_PER_DAY
    total_hours = sum(float(task.estimated_hours) for task in complete_tasks)
    return total_hours / LOOKBACK_DAYS


def save_delivery_forecast_insight(
    db: Session,
    workspace_member_id: uuid.UUID,
    project_id: uuid.UUID,
    result: dict,
) -> AIInsight:
    insight = AIInsight(
        workspace_member_id=workspace_member_id,
        project_id=project_id,
        insight_type="DELIVERY_FORECAST",
        scope="USER",
        description=result["description"],
    )
    db.add(insight)
    db.commit()
    db.refresh(insight)
    return insight
