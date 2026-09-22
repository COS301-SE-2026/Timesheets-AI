"""
This handles to router for the manager assistant narrative endpoint, called by ManagerAssistantNarrativeClient in the spring backend. thin router, all
logic lives in app/services/manager_assistant.py, matches how app/api/weekly_summary.py delegates to app/services/weekly_summary.py

Author: Zamokuhle Zwane
Date: 20 September 2026
"""

from fastapi import APIRouter, HTTPException

from app.schemas.manager_assistant import (
    ManagerAssistantNarrativeResponse,
    ManagerAssistantReviewRequest,
)
from app.services.manager_assistant import generate_manager_assistant_narrative

router = APIRouter(prefix="/insights/manager-assistant", tags=["Manager Assistant"])


@router.post(
    "/narrative",
    response_model=ManagerAssistantNarrativeResponse,
    responses={
        502: {"description": "Gemini call failed after all retries"},
    },
)
def generate_narrative(review: ManagerAssistantReviewRequest):
    try:
        narrative = generate_manager_assistant_narrative(review)
    except RuntimeError as e:
        raise HTTPException(status_code=502, detail=str(e)) from e

    return ManagerAssistantNarrativeResponse(narrative=narrative)
