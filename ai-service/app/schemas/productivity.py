"""
This handles the request and response shapes for the productivity
score endpoint, once again it replicates the existing db 
Please review my draft documentation once again. 
Author: Zamokuhle Zwane
Date: 12/07/2026
"""

import uuid
from datetime import date

from pydantic import BaseModel

class ProductivityScoreRequest(BaseModel):
    workspace_member_id: uuid.UUID
    period_start: date
    period_end: date
    task_count: int
    total_actual_hours: float
    total_estimated_hours: float
    score: float | None
    recommendation: str | None
    insight_id: uuid.UUID | None

    class Config:
        from_attributes = True