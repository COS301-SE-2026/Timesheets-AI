"""
This file handles the Router for the burnout risk endpoints
Author: Zamokuhle Zwane
Date: 23/08/2026
"""

import uuid
from datetime import date

from fastapi import APIRouter, Depends, Query
from sqlalchemy.orm import Session

from app.database import get_db
from app.schemas.burnout import BurnoutRiskResponse
from app.services.burnout import calculate_burnout_risk, save_burnout_insight

router = APIRouter(prefix="/insights/burnout", tags=["burnout"])


@router.post("/{workspace_member_id}/calculate", response_model=BurnoutRiskResponse)
def calculate_and_save(
    workspace_member_id: uuid.UUID,
    period_start: date = Query(..., description="Start of the period, inclusive"),
    period_end: date = Query(..., description="End of the period, inclusive"),
    db: Session = Depends(get_db),
):
    result = calculate_burnout_risk(db, workspace_member_id, period_start, period_end)
    insight = save_burnout_insight(db, workspace_member_id, result)

    return BurnoutRiskResponse(
        workspace_member_id=workspace_member_id,
        period_start=period_start,
        period_end=period_end,
        risk_level=result["risk_level"],
        reason=result["reason"],
        insight_id=insight.id,
    )
