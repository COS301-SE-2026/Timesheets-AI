"""
This file handles the response aka request shapes for the delivery forecast endpoint
Author: Zamokuhle Zwane
Date: 26/08/2026
"""

import uuid
from datetime import date

from pydantic import BaseModel


class DeliveryForecastResponse(BaseModel):
    task_id: uuid.UUID
    forecast_date: date | None
    planned_date: date | None
    insight_id: uuid.UUID | None

    model_config = {"from_attributes": True}
