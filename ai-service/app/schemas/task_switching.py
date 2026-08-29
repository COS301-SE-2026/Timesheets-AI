"""
Resquest/response shapes for the task swiching endpoint
Author: Zamokuhle Zwane
"""

import uuid
from datetime import date

from pydantic import BaseModel


class TaskSwitchingResponse(BaseModel):
    workspace_member_id: uuid.UUID
    period_start: date
    period_end: date
    project_id: uuid.UUID | None
    switches_per_day: float
    team_average: float | None
    insight_id: uuid.UUID | None

    model_config = {"from_attributes": True}
