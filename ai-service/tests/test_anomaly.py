"""
This file handles unit tests for the anomaly endpoint
Author: Zamokuhle
Date: 27/08/2026
"""

import uuid
from datetime import datetime
from unittest.mock import MagicMock

from app.services.anomaly import _rule_based_check, detect_anomalies

MEMBER_ID = uuid.uuid4()


def make_entry(duration_seconds, start_time=None):
    entry = MagicMock()
    entry.id = uuid.uuid4()
    entry.duration_seconds = duration_seconds
    entry.start_time = start_time or datetime(2026, 8, 4, 9, 0)
    entry.workspace_member_id = MEMBER_ID
    return entry


def should_flag_entry_when_duration_exceeds_16_hours():
    # arrage
    entry = make_entry(duration_seconds=17 * 3600)

    # act
    result = _rule_based_check(entry)

    # assert
    assert result is not None
    assert result["confidence"] == 0.9


def should_not_flag_entry_when_duration_is_reasonable():
    # arrange
    entry = make_entry(duration_seconds=8 * 3600)

    # act
    result = _rule_based_check(entry)

    # assert
    assert result is None


def should_fall_back_to_rule_based_check_when_history_is_too_small():
    # arrange
    check_entry = make_entry(duration_seconds=18 * 3600)
    db = MagicMock()
    db.query.return_value.filter.return_value.all.return_value = [check_entry]
    db.query.return_value.filter.return_value.order_by.return_value.limit.return_value.all.return_value = []

    # act
    results = detect_anomalies(db, MEMBER_ID, [check_entry.id])

    # assert
    assert len(results) == 1
    assert results[0]["entry_id"] == check_entry.id


def should_return_empty_list_when_no_matching_entries_found():
    # arrange
    db = MagicMock()
    db.query.return_value.filter.return_value.all.return_value = []

    # act
    results = detect_anomalies(db, MEMBER_ID, [uuid.uuid4()])

    assert results == []
