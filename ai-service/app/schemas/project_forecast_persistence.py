"""
- making this file so that the UI will show what is in the DB
- then I want the manager to then sync, to update the changes

Author: Nyasha
Date: 25/09/2026
"""

import uuid
from datetime import datetime

from pydantic import BaseModel

from app.schemas.project_forecast import ProjectForecastResponse


class SavedProjectForecastResponse(BaseModel):
    project_id: uuid.UUID
    last_synced_at: datetime
    forecast: ProjectForecastResponse
