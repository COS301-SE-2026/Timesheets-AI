"""
This file handles unit tests for burnout enpoint, still following AAA
Author: Zamokuhle Zwane
Date: 23/08/2026
"""

import uuid
from datetime import date
from unittest.mock import MagicMock

from app.services.burnout import calculate_burnout_risk, save_burnout_insight

MEMBER_ID = uuid.uuid4()
PERIOD_START = date(2026, 7, 1)
PERIOD_END = date(2026, 7, 7)


def make_day_row(day, hours):
    row = MagicMock()
    row.day = day
    row.total_seconds = hours * 3600
    return row


def mock_db_returning(rows):
    db = MagicMock()
    db.query.return_value.filter.return_value.group_by.return_value.order_by.return_value.all.return_value = rows
    return db


def should_return_low_risk_when_no_long_days_logged():
    # arranve
    db = mock_db_returning([make_day_row("2026-07-01", 6)])

    # act
    result = calculate_burnout_risk(db, MEMBER_ID, PERIOD_START, PERIOD_END)

    # assert
    assert result["risk_level"] == "LOW"


def should_return_high_risk_when_three_consecutive_long_days_logged():
    # arrange
    db = mock_db_returning(
        [
            make_day_row("2026-07-01", 11),
            make_day_row("2026-07-02", 12),
            make_day_row("2026-07-03", 11),
        ]
    )

    # act
    result = calculate_burnout_risk(db, MEMBER_ID, PERIOD_START, PERIOD_END)

    # assert
    assert result["risk_level"] == "HIGH"


def should_call_db_correctly_when_saving_burnout_insight():
    # arrange
    result = {"risk_level": "HIGH", "reason": "blah blah"}
    db = MagicMock()

    # act
    insight = save_burnout_insight(db, MEMBER_ID, result)

    # assert
    db.add.assert_called_once()
    db.commit.assert_called_once()
    assert insight is not None
