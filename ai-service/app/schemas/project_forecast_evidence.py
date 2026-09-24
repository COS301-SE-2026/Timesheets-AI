"""
- the schemas for evidence collected by the backend
- the evidence comes from GitHub and Jira

Author: Nyasha
Date: 24/09/2026
"""

import uuid
from datetime import datetime
from pydantic import BaseModel


class ActivityCount(BaseModel):
    """
    - shows how many activities were found for one activity type
    - for example GitHub commits or Jira issues
    """

    activity_type: str
    count: int


class ExternalEvidenceSummary(BaseModel):
    """
    - summarises the evidence found from one external source
    - this avoids sending every individual evidence event to the forecast service
    """

    available: bool
    activity_count: int
    latest_activity: datetime | None
    activities: list[ActivityCount]


class ProjectForecastEvidenceResponse(BaseModel):
    """
    - contains the external evidence available for the project
    - GitHub and Jira are seperate so I know which source is contributing to the support evidence
    """

    github: ExternalEvidenceSummary
    jira: ExternalEvidenceSummary