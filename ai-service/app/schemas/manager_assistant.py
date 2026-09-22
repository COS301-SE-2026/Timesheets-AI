"""
This handles the request/response shapes for the manager assistant narrative endpoint,
matches the pattern in app/schemas/weekly_summary.py
Author: Zamokuhle Zwane
Date: 20 September 2026
"""

from pydantic import BaseModel


class EvidenceSource(BaseModel):
    source_name: str
    match_count: int
    sub_score: float
    detail: str


class ManagerAssistantReviewRequest(BaseModel):
    timesheet_id: str
    confidence_score_percent: int
    verdict: str
    evidence_sources: list[EvidenceSource]
    narrative: str | None = None
    has_conflict: bool
    has_missing_evidence: bool


class ManagerAssistantNarrativeResponse(BaseModel):
    narrative: str