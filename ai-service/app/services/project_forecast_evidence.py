"""
- this service gets the external evidence for the project forecast
- the actual evidence collection is handled by the Spring backend Evidence Engine
- this service only calls the backend endpoint and converts the response
  into the Python evidence schema

Author: Nyasha
Date: 24/09/2026
"""

import logging
import uuid
from datetime import datetime

import httpx

from app.config import settings
from app.schemas.project_forecast_evidence import ProjectForecastEvidenceResponse

# the evidence logger
logger = logging.getLogger(__name__)


def get_project_forecast_evidence(
    project_id: uuid.UUID,
    start_time: datetime,
    end_time: datetime,
    access_token: str,
) -> ProjectForecastEvidenceResponse | None:
    """
    - this will pass the GitHub and Jira evidence
    - this will pass the users access token since the backend endpoint is protected
    """

    url = f"{settings.backend_url}/api/project-forecast/{project_id}/evidence"

    params = {
        "startTime": start_time.isoformat(),
        "endTime": end_time.isoformat(),
    }

    headers = {
        "Authorization": f"Bearer {access_token}",
    }

    try:
        # this is how I call the backend endpoint, has a timeout so if not there does not block the forecast generation
        response = httpx.get(
            url,
            params=params,
            headers=headers,
            timeout=10.0,
        )

        response.raise_for_status()

        return ProjectForecastEvidenceResponse.model_validate(response.json())

    # if there is no external evidence then the forecast does not stop
    except (httpx.HTTPError, ValueError):
        logger.exception(
            "Failed to retrieve external evidence for project %s",
            project_id,
        )

        return None
