"""
This file handles the router for the delivery forecast endpoint.
It'll take the task_id not workspace_member_id, since a forecast is inherently task-scopefd

Author: Zamokuhle Zwane
Date: 26/08/2026
"""

import uuid
from typing import Annotated

from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy.orm import Session

from app.database import get_db
from app.models.task import Task
from app.schemas.delivery_forecast import DeliveryForecastResponse
from app.services.delivery_forecast import (
    calculate_delivery_forecast,
    save_delivery_forecast_insight,
)

router = APIRouter(prefix="/insights/delivery-forecast", tags=["Delivery Forecast"])


@router.post(
    "/{task_id}/calculate",
    response_model=DeliveryForecastResponse,
    responses={404: {"description": "Task not found or deleted"}},
)
def calculate_and_save(task_id: uuid.UUID, db: Annotated[Session, Depends(get_db)]):
    result = calculate_delivery_forecast(db, task_id)

    if result["forecast_date"] is None and result["planned_date"] is None:
        raise HTTPException(status_code=404, detail=result["description"])

    task = db.query(Task).filter(Task.id == task_id).first()
    insight_id = None

    if task is not None:
        insight = save_delivery_forecast_insight(
            db, task.assigned_workspace_member_id, task.project_id, result
        )
        insight_id = insight.id

    return DeliveryForecastResponse(
        task_id=task_id,
        forecast_date=result["forecast_date"],
        planned_date=result["planned_date"],
        insight_id=insight_id,
    )
