"""
This handles anomaly detection for time enteries. when we have a new or edited time entry, it writes type=Anomaly,
scope=USER, payload={ entryId, reason, confidence }

This uses the isolationForest(scikit-learn) trained per user on their own entry history recently. an entry will be scored as a anomaly relative
to their own logging patterns

heres a refence to the model i used:
https://scikit-learn.org/stable/modules/generated/sklearn.ensemble.IsolationForest.html

Confidence is a percentile rank against the persons own history, bounded 0-1 by construction and interpretable
in plain language, rather than an unscaled score samples value

Author: Zamokuhle Zwane
Date: 27/08/2026
"""

import uuid

import numpy as np
from sklearn.ensemble import IsolationForest
from sqlalchemy.orm import Session

from app.models.ai_insight import AIInsight
from app.models.time_entry import TimeEntry

MIN_HISTORY_FOR_MODEL = 15
IMPOSSIBLE_DURATION_HOURS = 16.0
CONTAMINATION = 0.1


def detect_anomalies(
    db: Session, workspace_member_id: uuid.UUID, time_entry_ids: list[uuid.UUID]
) -> list[dict]:
    entries_to_check = (
        db.query(TimeEntry)
        .filter(
            TimeEntry.id.in_(time_entry_ids), TimeEntry.workspace_member_id == workspace_member_id
        )
        .all()
    )

    if not entries_to_check:
        return []
    history = _get_recent_history(db, workspace_member_id)

    if len(history) < MIN_HISTORY_FOR_MODEL:
        return [r for e in entries_to_check if (r := _rule_based_check(e)) is not None]
    return _model_based_check(entries_to_check, history)


def _rule_based_check(entry: TimeEntry) -> dict | None:
    hours = (entry.duration_seconds or 0) / 3600
    if hours > IMPOSSIBLE_DURATION_HOURS:
        return {
            "entry_id": entry.id,
            "reason": f"Single entry logged at {hours:.1f}h, exceeds a realistic single session.",
            "confidence": 0.9,
        }
    return None


def _model_based_check(entries_to_check: list[TimeEntry], history: list[TimeEntry]) -> list[dict]:
    all_entries = history + entries_to_check
    features = np.array([_extract_features(e) for e in all_entries])

    model = IsolationForest(contamination=CONTAMINATION, random_state=42)
    model.fit(features)

    scores = model.score_sample(features)
    history_scores = scores[: len(history)]

    check_start_index = len(history)
    results = []

    for i, entry in enumerate(entries_to_check):
        entry_score = scores[check_start_index + i]
        label = model.predict([features[check_start_index + i]])[0]

        if label == -1:
            hours = (entry.duration_seconds or 0) / 3600
            confidence = _percentile_confidence(entry_score, history_scores)
            results.append(
                {
                    "entry_id": entry.id,
                    "reason": _describe_anomaly(entry, hours),
                    "confidence": confidence,
                }
            )
    return results


def _percentile_confidence(entry_score: float, history_scores: np.ndarray) -> float:
    if len(history_scores) == 0:
        return 0.5
    more_normal_count = np.sum(history_scores > entry_score)
    return round(float(more_normal_count / len(history_scores)), 2)


def _extract_features(entry: TimeEntry) -> list[float]:
    hours = (entry.duration_seconds or 0) / 3600
    hour_of_day = entry.start_time.hour if entry.start_time else 0
    day_of_week = entry.start_time.weekday() if entry.start_time else 0
    return [hours, hour_of_day, day_of_week]


def _describe_anomaly(entry: TimeEntry, hours: float) -> str:
    if hours > 12:
        return f"Entry duration of {hours:.1f}h is unusually long compared to this person's typical entries."
    if entry.start_time and (entry.start_time.hour < 5 or entry.start_time.hour > 23):
        return "Entry logged at an unsual hour compared to this person's typical logging pattern."
    return "Entry pattern differs from this person's typically logging behaviour."


def _get_recent_history(
    db: Session, workspace_member_id: uuid.UUID, limit: int = 200
) -> list[TimeEntry]:
    return (
        db.query(TimeEntry)
        .filter(
            TimeEntry.workspace_member_id == workspace_member_id,
            TimeEntry.is_deleted.is_(False),
            TimeEntry.duration_seconds.isnot(None),
        )
        .order_by(TimeEntry.start_time.desc())
        .limit(limit)
        .all()
    )


def save_anomaly_insights(
    db: Session, workspace_member_id: uuid.UUID, anomalies: list[dict]
) -> list[AIInsight]:
    saved = []
    for anomaly in anomalies:
        insight = AIInsight(
            workspace_member_id=workspace_member_id,
            time_entry_id=anomaly["entry_id"],
            insight_type="ANOMALY",
            scope="USER",
            confidence=anomaly["confidence"] * 100,
            description=anomaly["reason"],
        )
        db.add(insight)
        saved.append(insight)
    db.commit()
    for insight in saved:
        db.refresh(insight)
    return saved
