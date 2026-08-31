"""
This handles weekly summary generation. It calls gemini's 2.5 Flash-Lite which is this free tier to turn a weeks, raw numbers into
plain-language narrative

Free tier, 1000 requests/day, generous for weekly cadence. the known trade offs: no SLA(handled with retry)
and free tiers prompts/responses may be used Google to improve their models

references: https://ai.google.dev/gemini-api/docs
Author: Zamokuhle Zwane
Date: 28/08/2026
"""

import os
import time
import uuid
from datetime import date, datetime, timedelta

import google.generativeai as genai
from sqlalchemy import and_, func
from sqlalchemy.orm import Session

from app.models.ai_insight import AIInsight
from app.models.time_entry import TimeEntry

# these are service level agreement handles the retry
MAX_RETRIES = 3
RETRY_BACKOFF_SECONDS = 2


def generate_weekly_summary(
    db: Session,
    subject_id: uuid.UUID,
    subject_type: str,
    week_start: date,
) -> dict:
    week_end = week_start + timedelta(days=6)

    hours_logged, project_count = _get_week_totals(
        db, subject_id, subject_type, week_start, week_end
    )
    latest_score, previous_score = _get_recent_productivity_scores(db, subject_id, subject_type)

    prompt = _build_prompt(hours_logged, project_count, latest_score, previous_score, subject_type)
    narrative = _call_gemini_with_retry(prompt)

    return {"narrative": narrative, "week_start": week_start}


def _build_prompt(hours_logged, project_count, latest_score, previous_score, subject_type) -> str:
    subject_phrase = "the team" if subject_type == "TEAM" else "you"

    movement = ""
    if latest_score is not None and previous_score is not None:
        diff = latest_score - previous_score
        if diff > 0:
            movement = f"Productivity moved up from {previous_score} to {latest_score}."
        elif diff < 0:
            movement = f"Productivity dropped from {previous_score} to {latest_score}."
        else:
            movement = f"Productivity held steady at {latest_score}."

    return (
        f"Write a short, plain-language weekly summary for a timesheet app. "
        f"Two to three sentences, no bullet points, no markdown formatting. "
        f"{subject_phrase.capitalize()} logged {hours_logged} hours across {project_count} project(s) this week. "
        f"{movement} "
        f"Keep the tone factual and neutral, not congratulatory or alarming."
    )


def _call_gemini_with_retry(prompt: str) -> str:
    model = genai.GenerativeModel("gemini-2.5-flash-lite")
    genai.configure(api_key=os.environ["GEMINI_API_KEY"])

    last_error = None
    for attempt in range(MAX_RETRIES):
        try:
            response = model.generate_content(prompt)
            return response.text.strip()
        except Exception as e:  # noqa: BLE001, broad on purpose, retrying regardless of error shape
            last_error = e
            time.sleep(RETRY_BACKOFF_SECONDS * (2**attempt))

    raise RuntimeError(f"Gemini call failed after {MAX_RETRIES} attempts: {last_error}")


def _get_week_totals(db: Session, subject_id: uuid.UUID, subject_type: str, week_start, week_end):
    filters = [
        TimeEntry.start_time >= datetime.combine(week_start, datetime.min.time()),
        TimeEntry.start_time <= datetime.combine(week_end, datetime.max.time()),
        TimeEntry.is_deleted.is_(False),
    ]

    if subject_type == "USER":
        filters.append(TimeEntry.workspace_member_id == subject_id)

    total_seconds = (
        db.query(func.sum(TimeEntry.duration_seconds)).filter(and_(*filters)).scalar() or 0
    )
    hours_logged = round(total_seconds / 3600, 1)

    project_count = db.query(TimeEntry.project_id).filter(and_(*filters)).distinct().count()

    return hours_logged, project_count


def _get_recent_productivity_scores(db: Session, subject_id: uuid.UUID, subject_type: str):
    scope = "TEAM" if subject_type == "TEAM" else "USER"
    rows = (
        db.query(AIInsight)
        .filter(
            AIInsight.workspace_member_id == subject_id,
            AIInsight.insight_type == "PRODUCTIVITY",
            AIInsight.scope == scope,
        )
        .order_by(AIInsight.created_at.desc())
        .limit(2)
        .all()
    )
    if len(rows) < 2:
        return (rows[0].score if rows else None), None
    return rows[0].score, rows[1].score


def save_weekly_summary_insight(
    db: Session, subject_id: uuid.UUID, subject_type: str, result: dict
) -> AIInsight:
    insight = AIInsight(
        workspace_member_id=subject_id,
        insight_type="WEEKLY_SUMMARY",
        scope="TEAM" if subject_type == "TEAM" else "USER",
        narrative=result["narrative"],
    )
    db.add(insight)
    db.commit()
    db.refresh(insight)
    return insight
