"""
This file handles router for the weekly summary endpoints

Author: Zamokuhle Zwane
Date: 31/08/2026
"""

import uuid
from datetime import date
from typing import Annotated

from fastapi import APIRouter, Depends, HTTPException, Query
from sqlalchemy.orm import Session

from app.database import get_db
from app.schemas.weekly_summary import WeeklySummaryResponse
from app.services.weekly_summary import generate_weekly_summary, save_weekly_summary_insight

router = APIRouter(prefix="/insights/weekly-summary", tags=["Weekly Summary"])


@router.post(
    "/{subject_id}/generate",
    response_model=WeeklySummaryResponse,
    responses={
        400: {"description": "subject_type must be USER or TEAM"},
        404: {"description": "Task not found or deleted"},
        502: {"description": "Gemini call failed after all retries"},
    },
)
def generate_and_save(
    subject_id: uuid.UUID,
    db: Annotated[Session, Depends(get_db)],
    subject_type: Annotated[str, Query(..., description="USER or TEAM")],
    week_start: Annotated[date, Query(..., description="Start of the week (Monday)")],
):
    if subject_type not in ("USER", "TEAM"):
        raise HTTPException(status_code=400, detail="subject_type must be USER or TEAM")

    try:
        result = generate_weekly_summary(db, subject_id, subject_type, week_start)
    except RuntimeError as e:
        raise HTTPException(status_code=502, detail=str(e)) from e

    insight = save_weekly_summary_insight(db, subject_id, subject_type, result)

    return WeeklySummaryResponse(
        subject_id=subject_id,
        subject_type=subject_type,
        week_start=week_start,
        narrative=result["narrative"],
        insight_id=insight.id,
    )
