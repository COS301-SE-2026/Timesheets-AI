"""
- exposes endpoint for project forecasting

Author: Nyasha
Date: 22/09/2026
"""

import uuid
from typing import Annotated

from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy.orm import Session

from app.database import get_db
from app.schemas.project_forecast import ProjectForecastResponse
from app.services.project_forecast import calculate_project_forecast

router = APIRouter(
    prefix="/insights/project-forecast",
    tags=["Project Forecast"],
)


# keeping in mind that I am going to pass the project URL
@router.post(
    "/{project_id}/calculate",
    response_model=ProjectForecastResponse,
    responses={404: {"description": "Project not found or deleted"}},
)
def calculate_project(
    project_id: uuid.UUID,
    db: Annotated[Session, Depends(get_db)],
):
    result = calculate_project_forecast(db, project_id)

    if result is None:
        raise HTTPException(
            status_code=404,
            detail="Project not found or deleted.",
        )

    return ProjectForecastResponse(**result)
