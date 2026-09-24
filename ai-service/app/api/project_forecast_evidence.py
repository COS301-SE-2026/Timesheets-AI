"""
- this endpoint is used to retrieve the external evidence that supports the project forecast
- the evidence itself is collected by the Spring backend Evidence Engine

Author: Nyasha
Date: 24/09/2026
"""

import uuid
from datetime import datetime

from fastapi import APIRouter, Header, HTTPException

from app.schemas.project_forecast_evidence import ProjectForecastEvidenceResponse
from app.services.project_forecast_evidence import get_project_forecast_evidence

router = APIRouter()


@router.get(
    "/insights/project-forecast/{project_id}/evidence",
    response_model=ProjectForecastEvidenceResponse,
)
def get_external_project_evidence(
    project_id: uuid.UUID,
    start_time: datetime,
    end_time: datetime,
    authorization: str = Header(...),
) -> ProjectForecastEvidenceResponse:
    """
    - gets the Jira and GitHub evidence for a project
    - takes the users auth token to the backend
    """

    access_token = authorization.removeprefix("Bearer ").strip()

    evidence = get_project_forecast_evidence(
        project_id=project_id,
        start_time=start_time,
        end_time=end_time,
        access_token=access_token,
    )

    # i want there to be a proper error response if the si-service cannot get info from backend
    if evidence is None:
        raise HTTPException(
            status_code=502,
            detail="Could not retrieve project forecast evidence",
        )

    return evidence
