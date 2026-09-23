"""
This file the generation the one paragraph narrative for the manager assistant
evidence review, mirrors app/services/weekly_summary.py's gemini call
pattern exactly: retry with backoff, then raise, no silent fallback here

the fallback-on-failure behaviour lives in ManagerAssistantNarrativeClient.java
instead, it catches any exception from this call (including the 502 this
raises after retries) and falls back to a templated sentence there. keeping
the retry-then-raise pattern here instead of duplicating a second fallback in
python, one place owns "what happens when gemini is unavailable", not two

Author: Zamokuhle Zwane
Date: 20 September 2026
"""

import time

import google.generativeai as genai

from app.config import settings
from app.schemas.manager_assistant import ManagerAssistantReviewRequest

MAX_RETRIES = 3
RETRY_BACKOFF_SECONDS = 2


def generate_manager_assistant_narrative(review: ManagerAssistantReviewRequest) -> str:
    prompt = _build_prompt(review)
    return _call_gemini_with_retry(prompt)


def _build_prompt(review: ManagerAssistantReviewRequest) -> str:
    evidence_lines = "\n".join(f"- {e.source_name}: {e.detail}" for e in review.evidence_sources)

    return (
        f"You are writing one short paragraph (2 sentences max) for a manager "
        f"reviewing a timesheet. Be plain and factual, no fluff, no exclamation marks.\n\n"
        f"Confidence score: {review.confidence_score_percent}%\n"
        f"Verdict: {review.verdict}\n"
        f"Evidence found:\n{evidence_lines}\n"
        f"Conflicting evidence detected: {review.has_conflict}\n"
        f"Missing evidence detected: {review.has_missing_evidence}\n\n"
        f"If LIKELY_REJECT, explain what's missing and why that's a problem. "
        f"If LIKELY_APPROVE, confirm the evidence lines up, no inconsistencies. "
        f"If NEEDS_REVIEW, say the evidence is partial and a manual look is worth it. "
        f"Do not repeat the raw numbers, the manager already sees those in the UI above your text."
    )


def _call_gemini_with_retry(prompt: str) -> str:
    # same model/config pattern as weekly_summary, genai.configure() called here not at module level, avoids the pytest collection KeyError this
    # codebase already hit once before
    model = genai.GenerativeModel("gemini-3.5-flash-lite")
    genai.configure(api_key=settings.gemini_api_key)

    last_error = None
    for attempt in range(MAX_RETRIES):
        try:
            response = model.generate_content(prompt)
            return response.text.strip()
        # retrying regardless of error shape, same as weekly_summary
        except Exception as e:  # noqa: BLE001
            last_error = e
            time.sleep(RETRY_BACKOFF_SECONDS * (2**attempt))

    raise RuntimeError(f"Gemini call failed after {MAX_RETRIES} attempts: {last_error}")
