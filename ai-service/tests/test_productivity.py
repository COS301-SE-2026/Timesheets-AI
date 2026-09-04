"""
This handles unit tests for the productivity score calculation logic
these will be mocking the db, so technically they test math and the logic, not actual sql filters,
those would require a separate intergration test against a real db instance

Author: Zamokuhle Zwane
Date: 13/07/2026
"""

import uuid
from datetime import date
from unittest.mock import MagicMock

import pytest

from app.services.productivity import calculate_productivity_score, save_productivity_insight

"""
this is a small helper so tests dont repeat the same fake task setup
"""


def make_task(estimated_hours, actual_hours):
    task = MagicMock()
    task.estimated_hours = estimated_hours
    task.actual_hours = actual_hours
    return task


"""
this is a function to build a fake db session where queries return whatever task
the task list the test wants regardless of the arguments passed in
"""


def mock_db_returning(tasks):
    db = MagicMock()
    db.query.return_value.filter.return_value.all.return_value = tasks
    return db


# A couple of mock values
MEMBER_ID = uuid.uuid4()
PERIOD_START = date(2026, 7, 1)
PERIOD_END = date(2026, 7, 31)


def test_score_calculates_correct_ratio():
    tasks = [make_task(10, 8), make_task(5, 5)]
    db = mock_db_returning(tasks)

    result = calculate_productivity_score(db, MEMBER_ID, PERIOD_START, PERIOD_END)

    assert result["task_count"] == 2
    assert result["total_estimated_hours"] == 15
    assert result["total_actual_hours"] == 13
    assert result["score"] == pytest.approx(86.67, rel=1e-2)  # score must be in the 86.67% range


def test_no_qualifying_tasks_returns_null_score():
    db = mock_db_returning([])
    result = calculate_productivity_score(db, MEMBER_ID, PERIOD_START, PERIOD_END)

    assert result["task_count"] == 0
    assert result["score"] is None
    assert result["recommendation"] is None


"""
this test is technically not require as it shouldnt be reachable 
given the query filters estimated_hours > 0, but its worth confirm the function
is safe given it changes
"""


def test_zero_total_estimated_hours_does_not_crash():
    tasks = [make_task(0, 0)]
    db = mock_db_returning(tasks)

    result = calculate_productivity_score(db, MEMBER_ID, PERIOD_START, PERIOD_END)

    assert result["score"] is None


def test_null_actual_hours_treated_as_zero():
    tasks = [make_task(10, None)]
    db = mock_db_returning(tasks)

    result = calculate_productivity_score(db, MEMBER_ID, PERIOD_START, PERIOD_END)

    assert result["score"] == 0
    assert result["total_actual_hours"] == 0


def test_low_score_triggers_recommendation():
    tasks = [make_task(10, 3)]  # 30%, and it'll be under the 60% threshold
    db = mock_db_returning(tasks)

    result = calculate_productivity_score(db, MEMBER_ID, PERIOD_START, PERIOD_END)

    assert result["score"] == 30
    assert result["recommendation"] is not None
    assert "below" in result["recommendation"]


def test_high_score_triggers_recommendation():
    tasks = [make_task(10, 16)]  # 160%, and it'll be over the 150% threshold
    db = mock_db_returning(tasks)

    result = calculate_productivity_score(db, MEMBER_ID, PERIOD_START, PERIOD_END)

    assert result["score"] == 160
    assert result["recommendation"] is not None
    assert "above" in result["recommendation"]


def test_healthy_score_has_no_recommendation():
    tasks = [make_task(10, 9)]  # 90%, this is well within range
    db = mock_db_returning(tasks)

    result = calculate_productivity_score(db, MEMBER_ID, PERIOD_START, PERIOD_END)

    assert result["score"] == 90
    assert result["recommendation"] is None


"""
Adding this to attempt to reach 100% coverage, before this its at 82%
"""


def test_save_productivity_insights_calls_db_correctly():
    result = {
        "score": 75.5,
        "recommendation": None,
        "description": "Productivity score calculate successfully. You have completed 75.5% tasks during the specified period",
    }

    db = MagicMock()
    insight = save_productivity_insight(db, MEMBER_ID, result)

    # confirm the insight object was built with the right fields
    assert insight.workspace_member_id == MEMBER_ID
    assert insight.insight_type == "PRODUCTIVITY"
    assert insight.project_id is None
    assert insight.score == 75.5
    assert insight.confidence == 100.0
    assert insight.description == result["description"]
    assert insight.recommendation is None

    # to confirm the actual db calls happpened, in the right order with the right obejct

    db.add.assert_called_once_with(insight)
    db.commit.assert_called_once()
    db.refresh.assert_called_once_with(insight)
