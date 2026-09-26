"""
Builds the five Developer Insights score cards: Productivity, Burnout Risk,
Task Switching, Estimate Accuracy, Cycle Time. Status-band thresholds are
named constants here, server-side, per the spec.

Author: Zamokuhle Zwane
Date: 26/09/2026
"""

from datetime import date, timedelta
from uuid import UUID

from sqlalchemy.orm import Session

from app.models.ai_insight import AIInsight
from app.services.dashboard_aggregates import cycle_time_stats, estimate_vs_actual

# Productivity/Estimate Accuracy: reuses productivity.py's existing
# LOW_SCORE_THRESHOLD=60 as the At Risk boundary, adds a Healthy floor of 85.
PRODUCTIVITY_AT_RISK_BELOW = 60.0
PRODUCTIVITY_HEALTHY_AT_OR_ABOVE = 85.0

# ASSUMPTION: no existing thresholds anywhere for these two - new, and
# should be reviewed with the team before Demo 3, not treated as settled.
TASK_SWITCHING_HEALTHY_AT_OR_BELOW = 3.0  # switches/day
TASK_SWITCHING_AT_RISK_ABOVE = 6.0
CYCLE_TIME_HEALTHY_AT_OR_BELOW = 3.0  # days
CYCLE_TIME_AT_RISK_ABOVE = 7.0

# BURNOUT has no numeric score column (see save_burnout_insight in
# burnout.py - only description/recommendation are set). This maps the
# existing risk_level strings to a display number so the card/gauge has
# something to show. Display convenience only - never feed this back into
# calculate_burnout_risk's own threshold logic.
_BURNOUT_RISK_TO_SCORE = {"LOW": 20.0, "MEDIUM": 55.0, "HIGH": 90.0}


def _band_higher_is_better(score: float, at_risk_below: float, healthy_at_or_above: float) -> str:
    if score < at_risk_below:
        return "AT_RISK"
    if score >= healthy_at_or_above:
        return "HEALTHY"
    return "MODERATE"


def _band_lower_is_better(score: float, healthy_at_or_below: float, at_risk_above: float) -> str:
    if score > at_risk_above:
        return "AT_RISK"
    if score <= healthy_at_or_below:
        return "HEALTHY"
    return "MODERATE"


def _latest_two(db: Session, workspace_member_id: UUID, insight_type: str) -> list[AIInsight]:
    return (
        db.query(AIInsight)
        .filter(
            AIInsight.workspace_member_id == workspace_member_id,
            AIInsight.insight_type == insight_type,
            AIInsight.project_id.is_(None),
        )
        .order_by(AIInsight.created_at.desc())
        .limit(2)
        .all()
    )


def _sparkline(
    db: Session, workspace_member_id: UUID, insight_type: str, limit: int = 8
) -> list[float]:
    rows = (
        db.query(AIInsight.score)
        .filter(
            AIInsight.workspace_member_id == workspace_member_id,
            AIInsight.insight_type == insight_type,
            AIInsight.project_id.is_(None),
        )
        .order_by(AIInsight.created_at.desc())
        .limit(limit)
        .all()
    )
    return [float(row.score) for row in reversed(rows) if row.score is not None]


def _productivity_card(db: Session, workspace_member_id: UUID) -> dict:
    latest_two = _latest_two(db, workspace_member_id, "PRODUCTIVITY")
    if not latest_two:
        return {
            "insight_type": "PRODUCTIVITY",
            "score": None,
            "status": "MODERATE",
            "delta_vs_previous": None,
            "recommendation": "Not enough data yet to score productivity for this period.",
            "sparkline": [],
        }

    latest = latest_two[0]
    previous = latest_two[1] if len(latest_two) > 1 else None
    score = float(latest.score) if latest.score is not None else 0.0

    return {
        "insight_type": "PRODUCTIVITY",
        "score": score,
        "status": _band_higher_is_better(
            score, PRODUCTIVITY_AT_RISK_BELOW, PRODUCTIVITY_HEALTHY_AT_OR_ABOVE
        ),
        "delta_vs_previous": (
            round(score - float(previous.score), 2)
            if previous and previous.score is not None
            else None
        ),
        "recommendation": latest.recommendation
        or "Productivity is tracking as expected for this period.",
        "sparkline": _sparkline(db, workspace_member_id, "PRODUCTIVITY"),
    }


def _burnout_card(db: Session, workspace_member_id: UUID) -> dict:
    """
    KNOWN FRAGILITY: risk_level was never persisted as a column on
    ai_insights (only baked into free-text description/recommendation in
    save_burnout_insight). This re-derives it from that text, which breaks
    silently if anyone edits the wording in burnout.py. Recommend a real
    risk_level column in a follow-up migration.
    """
    latest_two = _latest_two(db, workspace_member_id, "BURNOUT")
    if not latest_two:
        return {
            "insight_type": "BURNOUT",
            "score": None,
            "status": "HEALTHY",
            "delta_vs_previous": None,
            "recommendation": "No burnout signals detected yet.",
            "sparkline": [],
        }

    def _infer_risk_level(description: str | None, recommendation: str | None) -> str:
        if recommendation:
            return "HIGH"
        if description and "day logged" in description:
            return "MEDIUM"
        return "LOW"

    latest = latest_two[0]
    previous = latest_two[1] if len(latest_two) > 1 else None

    risk_level = _infer_risk_level(latest.description, latest.recommendation)
    previous_risk_level = (
        _infer_risk_level(previous.description, previous.recommendation) if previous else None
    )

    score = _BURNOUT_RISK_TO_SCORE[risk_level]
    previous_score = (
        _BURNOUT_RISK_TO_SCORE.get(previous_risk_level) if previous_risk_level else None
    )

    rows = (
        db.query(AIInsight.description, AIInsight.recommendation)
        .filter(
            AIInsight.workspace_member_id == workspace_member_id,
            AIInsight.insight_type == "BURNOUT",
        )
        .order_by(AIInsight.created_at.desc())
        .limit(8)
        .all()
    )
    sparkline = [
        _BURNOUT_RISK_TO_SCORE[_infer_risk_level(row.description, row.recommendation)]
        for row in reversed(rows)
    ]

    return {
        "insight_type": "BURNOUT",
        "score": score,
        "status": {"LOW": "HEALTHY", "MEDIUM": "MODERATE", "HIGH": "AT_RISK"}[risk_level],
        "delta_vs_previous": round(score - previous_score, 2)
        if previous_score is not None
        else None,
        "recommendation": latest.description or "No burnout signals detected yet.",
        "sparkline": sparkline,
    }


def _task_switching_card(db: Session, workspace_member_id: UUID) -> dict:
    latest_two = _latest_two(db, workspace_member_id, "TASK_SWITCHING")
    if not latest_two:
        return {
            "insight_type": "TASK_SWITCHING",
            "score": None,
            "status": "HEALTHY",
            "delta_vs_previous": None,
            "recommendation": "No task-switching data yet for this period.",
            "sparkline": [],
        }

    latest = latest_two[0]
    previous = latest_two[1] if len(latest_two) > 1 else None
    score = float(latest.score) if latest.score is not None else 0.0

    return {
        "insight_type": "TASK_SWITCHING",
        "score": score,
        "status": _band_lower_is_better(
            score, TASK_SWITCHING_HEALTHY_AT_OR_BELOW, TASK_SWITCHING_AT_RISK_ABOVE
        ),
        "delta_vs_previous": (
            round(score - float(previous.score), 2)
            if previous and previous.score is not None
            else None
        ),
        "recommendation": latest.description
        or "Task-switching frequency is within a healthy range.",
        "sparkline": _sparkline(db, workspace_member_id, "TASK_SWITCHING"),
    }


def _estimate_accuracy_card(
    db: Session, workspace_member_id: UUID, period_start: date, period_end: date
) -> dict:
    """
    Not persisted (no ESTIMATE_ACCURACY insight_type exists) - computed live
    for this period AND the immediately preceding period of equal length,
    to get a real (not synthetic) delta.
    """
    current = estimate_vs_actual(db, workspace_member_id, period_start, period_end)
    span_days = (period_end - period_start).days + 1
    previous_end = period_start - timedelta(days=1)
    previous_start = previous_end - timedelta(days=span_days - 1)
    previous = estimate_vs_actual(db, workspace_member_id, previous_start, previous_end)

    def _accuracy_from(payload: dict) -> float | None:
        total_estimated = sum(c["estimated_hours"] for c in payload["categories"])
        if total_estimated == 0:
            return None
        total_actual = sum(c["actual_hours"] for c in payload["categories"])
        error_ratio = abs(total_actual - total_estimated) / total_estimated
        return round(max(0.0, 100.0 - error_ratio * 100), 1)

    score = _accuracy_from(current)
    previous_score = _accuracy_from(previous)

    if score is None:
        return {
            "insight_type": "ESTIMATE_ACCURACY",
            "score": None,
            "status": "MODERATE",
            "delta_vs_previous": None,
            "recommendation": "No estimated hours recorded against tasks this period.",
            "sparkline": [],
        }

    return {
        "insight_type": "ESTIMATE_ACCURACY",
        "score": score,
        "status": _band_higher_is_better(
            score, PRODUCTIVITY_AT_RISK_BELOW, PRODUCTIVITY_HEALTHY_AT_OR_ABOVE
        ),
        "delta_vs_previous": round(score - previous_score, 2)
        if previous_score is not None
        else None,
        "recommendation": (
            "Estimates are tracking closely with actuals."
            if score >= PRODUCTIVITY_HEALTHY_AT_OR_ABOVE
            else "Estimates are drifting from actual hours - worth revisiting sizing."
        ),
        "sparkline": [s for s in (previous_score, score) if s is not None],
    }


def _cycle_time_card(
    db: Session, workspace_member_id: UUID, period_start: date, period_end: date
) -> dict:
    current = cycle_time_stats(db, workspace_member_id, period_start, period_end)
    span_days = (period_end - period_start).days + 1
    previous_end = period_start - timedelta(days=1)
    previous_start = previous_end - timedelta(days=span_days - 1)
    previous = cycle_time_stats(db, workspace_member_id, previous_start, previous_end)

    score = current["average_days"]
    if score is None:
        return {
            "insight_type": "CYCLE_TIME",
            "score": None,
            "status": "MODERATE",
            "delta_vs_previous": None,
            "recommendation": "No tasks completed in this period yet.",
            "sparkline": [],
        }

    previous_score = previous["average_days"]

    return {
        "insight_type": "CYCLE_TIME",
        "score": score,
        "status": _band_lower_is_better(
            score, CYCLE_TIME_HEALTHY_AT_OR_BELOW, CYCLE_TIME_AT_RISK_ABOVE
        ),
        "delta_vs_previous": round(score - previous_score, 2)
        if previous_score is not None
        else None,
        "recommendation": f"Tasks are completing in {score} days on average across {current['task_count']} task(s).",
        "sparkline": [s for s in (previous_score, score) if s is not None],
    }


def build_score_cards(
    db: Session, workspace_member_id: UUID, period_start: date, period_end: date
) -> list[dict]:
    return [
        _productivity_card(db, workspace_member_id),
        _burnout_card(db, workspace_member_id),
        _task_switching_card(db, workspace_member_id),
        _estimate_accuracy_card(db, workspace_member_id, period_start, period_end),
        _cycle_time_card(db, workspace_member_id, period_start, period_end),
    ]
