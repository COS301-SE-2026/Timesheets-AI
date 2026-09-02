"""
This file tests for nightly insights, checks orchestration counts and not the underlying calc logic which is
already covered somewhere else. Once again i used the AAA method but just for the scheduler itself

Author: Zamokuhle Zwane
Date: 31/08/2026
"""

from unittest.mock import MagicMock, patch

from app.scheduler import _run_nightly_job, _run_weekly_job


@patch("app.scheduler.run_nightly_insights_job")
@patch("app.scheduler.SessionLocal")
def should_close_session_when_nightly_job_succeeds(mock_session_local, mock_run_job):
    # arrange
    mock_db = MagicMock()
    mock_session_local.return_value = mock_db

    # act
    _run_nightly_job()

    # assert
    mock_run_job.assert_called_once()
    mock_db.close.assert_called_once()


@patch("app.scheduler.run_nightly_insights_job", side_effect=Exception("db exploded"))
@patch("app.scheduler.SessionLocal")
def should_close_session_even_when_nightly_job_raises(mock_session_local, mock_run_job):
    # arrange
    mock_db = MagicMock()
    mock_session_local.return_value = mock_db

    # act, should not raise
    _run_nightly_job()

    # assert
    mock_db.close.assert_called_once()


@patch("app.scheduler.run_weekly_summary_job")
@patch("app.scheduler.SessionLocal")
def should_close_session_when_weekly_job_succeeds(mock_session_local, mock_run_job):
    # arrange
    mock_db = MagicMock()
    mock_session_local.return_value = mock_db

    # act
    _run_weekly_job()

    # assert
    mock_run_job.assert_called_once()
    mock_db.close.assert_called_once()
