"""
This file handles the router for anomaly detection, called on new/edited time entry, not on a
schedule, so this takes a list of specific entry ids

Author: Zamokuhle Zwane
Date: 27/08/2026
"""

import uuid

from fastapi import APIRouter, Depends
from sqlalchemy.orm import Session

from app.database import get_db
from app.schemas.anomaly import AnomalyDetectionRequest, AnomalyDetectionResponse, AnomalyResult
from app.services.anomaly import detect_anomalies, save_anomaly_insights

router = APIRouter(prefix="/insights/anomaly", tags=["Anomaly"])


@router.post("/{workspace_member_id}/detect", response_model=AnomalyDetectionResponse)
def detect_and_save(
    workspace_member_id: uuid.UUID,
    request: AnomalyDetectionRequest,
    db: Session = Depends(get_db),
):
    anomalies = detect_anomalies(db, workspace_member_id, request.time_entry_ids)
    saved_insights = save_anomaly_insights(db, workspace_member_id, anomalies)

    result = [
        AnomalyResult(
            entry_id=anomaly["entry_id"],
            reason=anomaly["reason"],
            confidence=anomaly["confidence"],
            insight_id=insight.id,
        )
        for anomaly, insight in zip(anomalies, saved_insights)
    ]
    return AnomalyDetectionResponse(workspace_member_id=workspace_member_id, anomalies=result)
