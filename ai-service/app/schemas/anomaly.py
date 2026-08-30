"""
This handles the resquest / response shapes for the anomaly detection endpoint

Author: Zamokuhle Zwane
Date: 27/08/2027
"""

import uuid

from pydantic import BaseModel


class AnomalyDetectionRequest(BaseModel):
    time_entry_ids: list[uuid.UUID]


class AnomalyResult(BaseModel):
    entry_id: uuid.UUID
    reason: str
    confidence: float
    insight_id: uuid.UUID | None

    model_config = {"from_attributes": True}


class AnomalyDetectionResponse(BaseModel):
    workspace_member_id: uuid.UUID
    anomalies: list[AnomalyResult]
