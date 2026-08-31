"""
This file unit tests for weekly summary, the gemini call will be mocked throughout.
Still following the AAA method
we are mocking the calls to the api, will do further tests another way
Author: Zamokuhle Zwane
Date: 31/08/2026
"""

import uuid
from datetime import date
from unittest.mock import MagicMock, patch

import pytest

from app.services.weekly_summary import (
    _build_prompt,
    _call_gemini_with_retry,
    save_weekly_summary_insight,
)

SUBJECT_ID = uuid.uuid4()
WEEK_START = date(2026, 8, 4)


def should_include_hours_project_count_in_prompt():
    prompt = _build_prompt(28.4, 2, 87, 83, "USER")
    assert "28.4" in prompt
    assert "2 project" in prompt


def should_describe_upward_movement_when_score_increased():
    prompt = _build_prompt(20.0, 1, 90, 80, "USER")
    assert "moved up" in prompt


def should_describe_downward_when_Score_decreased():
    prompt = _build_prompt(20.0, 1, 70, 90, "USER")
    assert "dropped" in prompt


def should_say_team_when_subject_type_is_team():
    prompt = _build_prompt(100.0, 4, None, None, "TEAM")
    assert "team" in prompt.lower()


@patch("app.services.weekly_summary.genai.configure")
@patch("app.services.weekly_summary.genai.GenerativeModel")
def should_return_text_when_gemini_call_succeeds(mock_model_class, mock_configure):
    # arrange
    mock_response = MagicMock()
    mock_response.text = "  You logged 28.4h this week.  "
    mock_model_instance = MagicMock()
    mock_model_instance.generate_content.return_value = mock_response
    mock_model_class.return_value = mock_model_instance

    # act
    result = _call_gemini_with_retry("test prompt")

    # ssert
    assert result == "You logged 28.4h this week."
    mock_model_instance.generate_content.assert_called_once()


@patch("app.services.weekly_summary.genai.configure")
@patch("app.services.weekly_summary.time.sleep")
@patch("app.services.weekly_summary.genai.GenerativeModel")
def should_retry_when_gemini_calls_fails_then_succeeds(
    mock_model_class, mock_sleep, mock_configure
):
    # arrange
    mock_response = MagicMock()
    mock_response.text = "recovered on retry"
    mock_model_instance = MagicMock()
    mock_model_instance.generate_content.side_effect = [Exception("transient error"), mock_response]
    mock_model_class.return_value = mock_model_instance

    # act
    result = _call_gemini_with_retry("test prompt")

    # assert
    assert result == "recovered on retry"
    assert mock_model_instance.generate_content.call_count == 2


@patch("app.services.weekly_summary.genai.configure")
@patch("app.services.weekly_summary.time.sleep")
@patch("app.services.weekly_summary.genai.GenerativeModel")
def should_raise_when_all_retries_are_exhausted(mock_model_class, mock_sleep, mock_configure):
    # ARRANGE
    mock_model_instance = MagicMock()
    mock_model_instance.generate_content.side_effect = Exception("persistent error")
    mock_model_class.return_value = mock_model_instance

    # ACT & ASSERT
    with pytest.raises(RuntimeError, match="Gemini call failed"):
        _call_gemini_with_retry("test prompt")


def should_call_db_correctly_when_saving_weekly_summary_insight():
    # ARRANGE
    result = {"narrative": "test narrative", "week_start": WEEK_START}
    db = MagicMock()

    # ACT
    insight = save_weekly_summary_insight(db, SUBJECT_ID, "USER", result)

    # ASSERT
    db.add.assert_called_once()
    db.commit.assert_called_once()
    assert insight is not None
