"""
This handles the 
"""

import uuid
from datetime import date

from fastapi import APIRouter, Depends, Query
from sqlalchemy.orm import Session

from app.database import get_db
from app.schemas.productivity import ProductivityScoreResponse
from app.services.productivity import calculate_productivity_score, save_productivity_insight

router = APIRouter(prefix = "/insights/productivity", tags = ["Productivity"])

@router.post("/{workspace_member_id}/calculate", response_model=ProductivityScoreResponse)
def calculate_and_save(
    workspace_member_id = uuid.UUID,
    period_start: date = Query(..., description = "Start of the period, inclusive"),
    period_end: date = Query(..., description = "End of the period, inclusive"),
    db: Session  = Depends(get_db),

):
    result = calculate_productivity_score(db, workspace_member_id, period_start, period_end)
    insight_id = None
    if result["score"] is not None:
        insight = save_productivity_insight(db, workspace_member_id, result)
        insight_id = insight.id

    return ProductivityScoreResponse(
        workspace_member_id = workspace_member_id,
        period_start = period_start,
        period_end = period_end,
        task_count = result["task_count"],
        total_actual_hours = result["total_actual_hours"],
        total_estimated_hours = result["total_estimated_hours"],
        score = result["score"],
        recommendation = result["recommendation"],
        insight_id = insight_id,
    )