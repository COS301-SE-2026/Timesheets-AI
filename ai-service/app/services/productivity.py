"""
This handles the core logic for the productivity score endpoint,
Its plain math, no actual model here. The score is sum(actual_hours)/sum(estimated_hours) *100
Author: Zamokuhle Zwane
Date: 13/07/2026
"""

import uuid
from datetime import date

from sqlalchemy import and_, or_
from sqlalchemy.orm import Session

from app.models.ai_insight import AIInsight
from app.models.task import Task

# Assumption based on schema from Nyasha, filter tasks by due_date landing with date range, since tasks
# don't have a date for a week concept. So i'll reconfirm with Nyasha if this is the right approach, or if we should use created_at instead.

# These can be modified later.
LOW_SCORE_THRESHOLD = 60.0
HIGH_SCORE_THRESHOLD = 150.0
PERCENT = 100
SCORE_DECIMALS = 2
# ai_insights.score is Numeric(5,2), anything above this fails the insert
MAX_SCORE = 999.99


def calculate_productivity_score(
    db: Session,
    workspace_member_id: uuid.UUID,
    period_start: date,
    period_end: date,
    project_id: uuid.UUID | None = None,
) -> dict:
    filters = [
        Task.assigned_workspace_member_id == workspace_member_id,
        Task.due_date >= period_start,
        Task.due_date <= period_end,
        Task.is_deleted.is_(False),
        # a TODO task with logged hours has clearly been started, so it still counts
        or_(Task.status != "TODO", Task.actual_hours > 0),
        Task.estimated_hours.isnot(None),
        Task.estimated_hours > 0,
    ]
    # scope to one project only when the job asks for it
    if project_id is not None:
        filters.append(Task.project_id == project_id)

    tasks = db.query(Task).filter(and_(*filters)).all()

    task_count = len(tasks)
    # numeric columns come back as Decimal, cast once so the math below is all float
    total_estimated_hours = float(sum(task.estimated_hours for task in tasks))
    total_actual_hours = float(sum(task.actual_hours or 0 for task in tasks))

    if total_estimated_hours == 0:
        return {
            "task_count": task_count,
            "total_actual_hours": round(total_actual_hours, SCORE_DECIMALS),
            "total_estimated_hours": 0.0,
            "score": None,
            "recommendation": None,
            "description": "Productivity score cannot be calculated due to lack of estimated hours.",
        }

    # cap so an extreme ratio cannot overflow the numeric(5,2) column
    score = min(
        round((total_actual_hours / total_estimated_hours) * PERCENT, SCORE_DECIMALS),
        MAX_SCORE,
    )
    recommendation = None
    if score < LOW_SCORE_THRESHOLD:
        recommendation = "Your productivity is below the expected level. Consider reviewing your work habits and time management."
    elif score > HIGH_SCORE_THRESHOLD:
        recommendation = "Your productivity is above the expected level. Ensure that you are maintaining a healthy work-life balance."
    return {
        "task_count": task_count,
        "total_actual_hours": round(total_actual_hours, SCORE_DECIMALS),
        "total_estimated_hours": round(total_estimated_hours, SCORE_DECIMALS),
        "score": score,
        "recommendation": recommendation,
        "description": (
            f"Logged {round(total_actual_hours, SCORE_DECIMALS)}h against "
            f"{round(total_estimated_hours, SCORE_DECIMALS)}h estimated across "
            f"{task_count} task(s), a score of {score}%."
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
    db: Session,
    workspace_member_id: uuid.UUID,
    result: dict,
    project_id: uuid.UUID | None = None,
) -> AIInsight:
    insight = AIInsight(
        workspace_member_id=workspace_member_id,
        insight_type="PRODUCTIVITY",
        scope="USER",
        project_id=project_id,  # None means the overall score across every project
        score=result["score"],
        confidence=100.0,  # full confidence, this is plain arithmetic
        description=result["description"],
        recommendation=result["recommendation"],
    )
    db.add(insight)
    db.commit()
    db.refresh(insight)
    return insight
