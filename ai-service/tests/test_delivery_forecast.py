"""
This file handles unit tests for delivery forecast following AAA
Author: Zamokuhle Zwane
Date: 27/08/2026
"""

import uuid
from datetime import date
from unittest.mock import MagicMock

from app.services.delivery_forecast import calculate_delivery_forecast

def make_task(estimated, actual, status="IN_PROGRESS", due_date=None):
    task = MagicMock()
    task.id=uuid.uuid4()
    task.estimated_hours = estimated
    task.actual_hours = actual
    task.status = status
    task.due_date = due_date or date(2026, 8, 1)
    task.completed_at=None
    task.assigned_workspace_member_id = uuid.uuid4()
    task.is_deleted = False
    return task

def should_return_none_when_task_not_found():
    #arrange
    db = MagicMock()
    db.query.return_value.filter.return_value.first.return_value = None

    #act
    result = calculate_delivery_forecast(db, uuid.uuid4())

    #assert
    assert result["forecast_date"] is None

def should_return_planned_date_when_task_already_done():
    #arrange
    task=make_task(10, 10, status="DONE")
    db=MagicMock()
    db.query.return_value.filter.return_value.first.return_value = task

    #act
    result=calculate_delivery_forecast(db, task.id)

    #assert
    assert result["planned_date"] == task.due_date


def shoould_project_a_forecast_date_when_task_in_progress():
    #arrange
    task=make_task(10, 4)
    db= MagicMock()
    db.query.return_value.filter.return_value.first.return_value = task
    db.query.return_value.filter.return_value.all.return_value = []

    #act
    result = calculate_delivery_forecast(db, task.id)

    #assert
    assert result["forecast_date"] is None
