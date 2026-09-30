"""
This file handles the request/response shapes for the burnout risk endpoint
Author: Zamokuhle Zwane
Date: 23/08/2026

Patch: added risk_score (numeric 0-100), so the manager dashboard's flagged
burnout card doesn't have to guess a score off the text reason anymore
"""

import uuid
from datetime import date

from pydantic import BaseModel


class BurnoutRiskResponse(BaseModel):
    workspace_member_id: uuid.UUID
    period_start: date
    period_end: date
    risk_level: str
    risk_score: float
    reason: str
    insight_id: uuid.UUID

    model_config = {"from_attributes": True}
