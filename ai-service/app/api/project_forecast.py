"""
- exposes endpoint for project forecasting

Author: Nyasha
Date: 22/09/2026
"""

import uuid
from typing import Annotated

from fastapi import APIRouter, Depends, Header, HTTPException
from sqlalchemy.orm import Session

from app.database import get_db
from app.schemas.project_forecast import ProjectForecastResponse
from app.schemas.project_forecast_persistence import SavedProjectForecastResponse
from app.schemas.project_forecast_scenario import ProjectForecastScenarioResponse
from app.services.project_forecast import calculate_project_forecast
from app.services.project_forecast_persistence import (
    get_saved_project_forecast,
    save_project_forecast,
)
from app.services.project_forecast_scenario import calculate_project_forecast_scenarios

router = APIRouter(
    prefix="/insights/project-forecast",
    tags=["Project Forecast"],
)


@router.get(
    "/{project_id}",
    response_model=SavedProjectForecastResponse,
    responses={404: {"description": "Saved project forecast not found"}},
)
def get_project_forecast(
    project_id: uuid.UUID,
    db: Annotated[Session, Depends(get_db)],
):
    """
    - gets the last saved project forecast from ai_insights
    - does not recalculate the forecast
    - allows the UI to display stale forecast data until the manager syncs it
    """

    saved_insight = get_saved_project_forecast(
        db=db,
        project_id=project_id,
    )

    if saved_insight is None:
        raise HTTPException(
            status_code=404,
            detail="No saved project forecast found.",
        )

    return SavedProjectForecastResponse(
        project_id=project_id,
        last_synced_at=saved_insight.last_synced_at,
        forecast=ProjectForecastResponse(**saved_insight.data),
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
    authorization: Annotated[str | None, Header()] = None,
):
    access_token = None

    # forward the authenticated user's token when external evidence is available
    if authorization:
        access_token = authorization.removeprefix("Bearer ").strip()

    result = calculate_project_forecast(
        db,
        project_id,
        access_token=access_token,
    )

    if result is None:
        raise HTTPException(
            status_code=404,
            detail="Project not found or deleted.",
        )

    return ProjectForecastResponse(**result)


@router.post(
    "/{project_id}/scenarios",
    response_model=ProjectForecastScenarioResponse,
    responses={404: {"description": "Project not found or deleted"}},
)
def calculate_project_scenarios(
    project_id: uuid.UUID,
    db: Annotated[Session, Depends(get_db)],
):
    forecast = calculate_project_forecast(
        db=db,
        project_id=project_id,
    )

    if forecast is None:
        raise HTTPException(
            status_code=404,
            detail="Project not found or deleted.",
        )

    # just pass the forecast calues to the service
    return calculate_project_forecast_scenarios(
        project_id=project_id,
        project_name=forecast["project_name"],
        used_hours=forecast["budget"]["used_hours"],
        remaining_hours=forecast["tasks"]["estimated_remaining_hours"],
        weekly_velocity=forecast["velocity"]["recent_hours_per_week"],
    )


@router.post(
    "/{project_id}/sync",
    response_model=ProjectForecastResponse,
    responses={404: {"description": "Project not found or deleted"}},
)
def sync_project_forecast(
    project_id: uuid.UUID,
    db: Annotated[Session, Depends(get_db)],
    authorization: Annotated[str | None, Header()] = None,
):
    """
    - recalculates the project latest project forecast
    - gets the external evidence when auth is there
    - saves the forecast to ai_insights, and replaces the old forecast
    """

    access_token = None

    if authorization:
        access_token = authorization.removeprefix("Bearer ").strip()

    result = calculate_project_forecast(
        db=db,
        project_id=project_id,
        access_token=access_token,
    )

    if result is None:
        raise HTTPException(
            status_code=404,
            detail="Project not found or deleted.",
        )

    saved_insight = save_project_forecast(
        db=db,
        project_id=project_id,
        forecast=result,
    )

    if saved_insight is None:
        raise HTTPException(
            status_code=404,
            detail="Project not found or deleted.",
        )

    return ProjectForecastResponse(**result)
