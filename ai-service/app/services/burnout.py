"""
This file handles burnout risk detection, nightly per user, writes scope=TEAM
regardless of who its about. Absolutely cant do per-project pass because this is about the person.

This is threshold based, its not a trained model
Author: Zamokuhle Zwane
Date: 23/08/2026
"""

import uuid
from datetime import date, datetime

from sqlalchemy import and_, func
from sqlalchemy.orm import Session

from app.models.ai_insight import AIInsight
from app.models.time_entry import TimeEntry

LONG_DAY_HOURS = 10.0  # two hours longer than the typical work day
CONSECUTIVE_LONG_DAYS_THRESHOLD = 3  # this can be changed to maybe 6 days


def calculate_burnout_risk(
    db: Session, workspace_member_id: uuid.UUID, period_start: date, period_end: date
) -> dict:
    daily_seconds = (
        db.query(
            func.date(TimeEntry.start_time).label("day"),
            func.sum(TimeEntry.duration_seconds).label("total_seconds"),
        )
        .filter(
            and_(
                TimeEntry.workspace_member_id == workspace_member_id,
                TimeEntry.start_time >= datetime.combine(period_start, datetime.min.time()),
                TimeEntry.start_time <= datetime.combine(period_end, datetime.max.time()),
                TimeEntry.is_deleted.is_(False),
            )
        )
        .group_by(func.date(TimeEntry.start_time))
        .order_by(func.date(TimeEntry.start_time))
        .all()
    )

    long_day_streak = 0
    max_streak = 0
    for row in daily_seconds:
        hours = (row.total_seconds or 0) / 3600
        if hours >= LONG_DAY_HOURS:
            long_day_streak += 1
            max_streak = max(max_streak, long_day_streak)
        else:
            long_day_streak = 0

    if max_streak >= CONSECUTIVE_LONG_DAYS_THRESHOLD:
        risk_level = "HIGH"
        reason = f"Sustain {LONG_DAY_HOURS}h+ days for {max_streak} days staight"
    elif max_streak >= 1:
        risk_level = "MEDIUM"
        reason = f"At least one {LONG_DAY_HOURS: 0f}h+ day logged this period"
    else:
        risk_level = "LOW"
        reason = "No sustained long-hour days detected"

    return {"risk_level": risk_level, "reason": reason}


def save_burnout_insight(db: Session, workspace_member_id: uuid.UUID, result: dict) -> AIInsight:
    insight = AIInsight(
        workspace_member_id=workspace_member_id,
        insight_type="BURNOUT",
        scope="TEAM",
        description=result["reason"],
        recommendation=(
            "Check in befire assigning further overtime."
            if result["risk_level"] == "HIGH"
            else None,
        ),
    )
    db.add(insight)
    db.commit()
    db.refresh(insight)
    return insight
