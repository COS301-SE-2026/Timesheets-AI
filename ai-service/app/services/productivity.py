"""
This handles the core logic for the productivity score endpoint,
Its plain math, no actual model here. The score is sum(actual_hours)/sum(estimated_hours) *100
Author: Zamokuhle Zwane
Date: 13/07/2026
"""

import uuid
from datetime import date

from sqlalchemy import and_
from sqlalchemy.orm import Session

from app.models.ai_insight import AIInsight
from app.models.task import Task

# Assumption based on schema from Nyasha, filter tasks by due_date landing with date range, since tasks
# don't have a date for a week concept. So i'll reconfirm with Nyasha if this is the right approach, or if we should use created_at instead.

# These can be modified later.
LOW_SCORE_THRESHOLD = 60.0
HIGH_SCORE_THRESHOLD = 150.0


def calculate_productivity_score(
    db: Session, workspace_member_id: uuid.UUID, period_start: date, period_end: date
) -> dict:
    tasks = (
        db.query(Task)
        .filter(
            and_(
                Task.assigned_workspace_member_id == workspace_member_id,
                Task.due_date >= period_start,
                Task.due_date <= period_end,
                Task.is_deleted.is_(False),
                Task.status != "TODO",  # Exclude tasks with status "TODO"
                Task.estimated_hours.isnot(None),  # Exclude tasks with null estimated_hours,
                Task.estimated_hours > 0,  # Exclude tasks with estimated_hours <= 0
            )
        )
        .all()
    )

    task_count = len(tasks)
    total_estimated_hours = sum(task.estimated_hours for task in tasks)
    total_actual_hours = sum(task.actual_hours or 0 for task in tasks)

    if total_estimated_hours == 0:
        return {
            "task_count": task_count,
            "total_actual_hours": round(total_actual_hours, 2),
            "total_estimated_hours": 0.0,
            "score": None,
            "recommendation": None,
            "description": "Productivity score cannot be calculated due to lack of estimated hours.",
        }

    score = round((total_actual_hours / total_estimated_hours) * 100, 2)
    recommendation = None
    if score < LOW_SCORE_THRESHOLD:
        recommendation = "Your productivity is below the expected level. Consider reviewing your work habits and time management."
    elif score > HIGH_SCORE_THRESHOLD:
        recommendation = "Your productivity is above the expected level. Ensure that you are maintaining a healthy work-life balance."
    return {
        "task_count": task_count,
        "total_actual_hours": round(total_actual_hours, 2),
        "total_estimated_hours": round(total_estimated_hours, 2),
        "score": score,
        "recommendation": recommendation,
        "description": f"Productivity score calculated successfully. You have completed {score}% tasks during the specified period.".format(
            task_count
        ),
    }


def get_projects_with_tasks_in_period(
    db: Session, workspace_member_id: uuid.UUID, period_start: date, period_end: date
) -> list[uuid.UUID]:
    rows = (
        db.query(Task.project_id)
        .filter(
            and_(
                Task.assigned_workspace_member_id == workspace_member_id,
                Task.due_date >= period_start,
                Task.due_date <= period_end,
                Task.is_deleted.is_(False),
            )
        )
        .distinct()
        .all()
    )
    return [row[0] for row in rows]


def save_productivity_insight(
    db: Session, workspace_member_id: uuid.UUID, result: dict
) -> AIInsight:
    insight = AIInsight(
        workspace_member_id=workspace_member_id,
        insight_type="PRODUCTIVITY",
        project_id=None,  # Assuming no specific project is associated with this insight
        score=result["score"],
        confidence=100.0,  # Assuming full confidence for this calculation
        description=result["description"],
        recommendation=result["recommendation"],
    )
    db.add(insight)
    db.commit()
    db.refresh(insight)
    return insight
