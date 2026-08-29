"""
Tests for task_switching, AAA method

Author: Zamkuhle Zwane
Date: 22/08/2026
"""

import uuid
from datetime import date, datetime
from unittest.mock import MagicMock

from app.services.task_switching import calculate_task_switching, save_task_switching_insight

MEMBER_ID = uuid.uuid4()
PERIOD_START = date(2026, 7, 1)
PERIOD_END = date(2026, 7, 7)


def make_entry(task_id, start_time):
    entry = MagicMock()
    entry.task_id = task_id
    entry.start_time = start_time
    entry.project_id = uuid.uuid4()
    entry.is_deleted = False
    return entry


def mock_db_returning(entries):
    db = MagicMock()
    db.query.return_value.filter.return_value.order_by.return_value.all.return_value = entries
    return db


def should_return_zero_switches_when_no_entries_exist():
    # arrange
    db = mock_db_returning([])

    # act
    result = calculate_task_switching(db, MEMBER_ID, PERIOD_START, PERIOD_END)

    # assert
    assert result["switches_per_day"] == 0.0


def should_count_a_switch_when_consecutive_entries_differ_in_task():
    # arrange
    task_a = uuid.uuid4()
    task_b = uuid.uuid4()
    entries = [
        make_entry(task_a, datetime(2026, 7, 1, 9, 0)),
        make_entry(task_b, datetime(2026, 7, 1, 11, 0)),
    ]
    db = mock_db_returning(entries)

    # act
    result = calculate_task_switching(db, MEMBER_ID, PERIOD_START, PERIOD_END)

    # assert
    assert result["switches_per_day"] > 0


def should_not_count_a_switch_when_consecutive_entries_share_task():
    # arrange
    task_a = uuid.uuid4()
    entries = [
        make_entry(task_a, datetime(2026, 7, 1, 9, 0)),
        make_entry(task_a, datetime(2026, 7, 1, 11, 0)),
    ]
    db = mock_db_returning(entries)

    # act
    result = calculate_task_switching(db, MEMBER_ID, PERIOD_START, PERIOD_END)

    # assert
    assert result["switches_per_day"] == 0


def should_call_db_correctly_when_saving_task_switching_insight():
    # arrange
    result = {"switches_per_day": 3.5, "team_average": None, "description": "test"}
    db = MagicMock()

    # act
    insight = save_task_switching_insight(db, MEMBER_ID, result)

    # assert
    db.add.assert_called_once()
    db.commit.assert_called_once()
    assert insight is not None
