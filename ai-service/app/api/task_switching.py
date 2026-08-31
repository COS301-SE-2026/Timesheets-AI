"""
Router for the task-switching endpoint, kept the shape consisten

Author: Zamokuhle Zwane
Date: 22/08/2026
"""

import uuid
from datetime import date

from fastapi import APIRouter, Depends, Query
from sqlalchemy.orm import Session

from app.database import get_db
from app.schemas.task_switching import TaskSwitchingResponse
from app.services.task_switching import calculate_task_switching, save_task_switching_insight

router = APIRouter(prefix="/insights/task-switching", tags=["Task Switching"])


@router.post("/{workspace_member_id}/calculate", response_model=TaskSwitchingResponse)
def calculate_and_save(
    workspace_member_id: uuid.UUID,
    period_start: date = Query(..., description="Start of the period, inclusive"),
    period_end: date = Query(..., description="End of the period, inclusive"),
    project_id: uuid.UUID | None = Query(
        None, description="Scopes the result to one project, omit for overall."
    ),
    db: Session = Depends(get_db),
):
    result = calculate_task_switching(
        db, workspace_member_id, period_start, period_end, project_id=project_id
    )
    insight = save_task_switching_insight(db, workspace_member_id, result, project_id=project_id)

    return TaskSwitchingResponse(
        workspace_member_id=workspace_member_id,
        period_start=period_start,
        period_end=period_end,
        project_id=project_id,
        switches_per_day=result["switches_per_day"],
        team_average=result["team_average"],
        insight_id=insight.id,
    )
