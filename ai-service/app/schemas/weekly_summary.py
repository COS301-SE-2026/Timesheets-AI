"""
This file handles the request and response shapes for the weekly summary endpoints

Author: Zamokuhle Zwane
Date: 28/08/2026
"""

import uuid
from datetime import date

from pydantic import BaseModel


class WeeklySummaryResponse(BaseModel):
    subject_id: uuid.UUID
    subject_type: str
    week_start: date
    narrative: str
    insight_id: uuid.UUID | None

    model_config = {"from_attributes": True}
