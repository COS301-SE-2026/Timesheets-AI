"""
This file tests for nightly insights, checks orchestration counts and not the underlying calc logic which is
already covered somewhere else. Once again i used the AAA method

Author: Zamokuhle Zwane
Date: 31/08/2026
"""

import uuid
from datetime import date
from unittest.mock import MagicMock, patch

from app.jobs.nightly_insights_job import (
    _get_active_members,
    _get_active_tasks,
    run_nightly_insights_job,
)

MEMBER_ID = uuid.uuid4()
PROJECT_ID = uuid.uuid4()
PERIOD_START = date(2026, 8, 4)
PERIOD_END = date(2026, 8, 10)


@patch("app.jobs.nightly_insights_job._get_active_tasks", return_value=[])
@patch("app.jobs.nightly_insights_job._get_active_members", return_value=[MEMBER_ID])
@patch("app.jobs.nightly_insights_job.save_burnout_insight")
@patch("app.jobs.nightly_insights_job.calculate_burnout_risk")
@patch("app.jobs.nightly_insights_job.save_task_switching_insight")
@patch(
    "app.jobs.nightly_insights_job.calculate_task_switching",
    return_value={"switches_per_day": 1.0, "team_average": None, "description": "x"},
)
@patch("app.jobs.nightly_insights_job.save_productivity_insight")
@patch("app.jobs.nightly_insights_job.get_projects_with_tasks_in_period", return_value=[PROJECT_ID])
@patch(
    "app.jobs.nightly_insights_job.calculate_productivity_score",
    return_value={
        "score": 80.0,
        "task_count": 1,
        "total_actual_hours": 8,
        "total_estimated_hours": 10,
        "recommendation": None,
        "description": "blah blah",
    },
)
def should_write_overall_and_project_rows_for_each_active_member(
    mock_calc_prod,
    mock_get_projects,
    mock_save_prod,
    mock_calc_switch,
    mock_save_switch,
    mock_calc_burnout,
    mock_save_burnout,
    mock_get_members,
    mock_get_tasks,
):
    # arrange
    mock_calc_burnout.return_value = {"risk_level": "LOW", "reason": "x"}
    db = MagicMock()

    # act
    run_nightly_insights_job(db, PERIOD_START, PERIOD_END)

    # assert
    assert mock_calc_prod.call_count == 2
    assert mock_save_prod.call_count == 2
    assert mock_calc_switch.call_count == 2
    assert mock_save_switch.call_count == 2
    assert mock_calc_burnout.call_count == 1
    assert mock_save_burnout.call_count == 1


@patch("app.jobs.nightly_insights_job._get_active_tasks", return_value=[])
@patch("app.jobs.nightly_insights_job._get_active_members", return_value=[])
def should_do_nothing_when_no_active_members_exist(mock_get_members, mock_get_tasks):
    # arrang
    db = MagicMock()

    # act & assert, should not raise
    run_nightly_insights_job(db, PERIOD_START, PERIOD_END)


def should_return_only_active_members_when_some_users_are_inactive():
    # arrange
    db = MagicMock()
    active_id = uuid.uuid4()
    db.query.return_value.join.return_value.filter.return_value.all.return_value = [(active_id,)]

    # act
    result = _get_active_members(db)

    # assert
    assert result == [active_id]


def should_exclude_done_and_deleted_tasks():
    # arrange
    db = MagicMock()
    active_task_id = uuid.uuid4()
    db.query.return_value.filter.return_value.all.return_value = [(active_task_id,)]

    # act
    result = _get_active_tasks(db)

    # assert
    assert result == [active_task_id]
