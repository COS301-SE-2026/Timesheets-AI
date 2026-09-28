"""
this is a file that hanldes nightly jobs, it loops through active members, runs overall and per project passes for the productivity
and task switching and burnout jobs.
it loops active tasks separately for delivery forecast and weekly summary runs on its own separate entry point


Author: Zamokuhle Zwane
Date: 31/08/2026
"""

import logging
import uuid
from collections.abc import Callable
from datetime import date, datetime, time, timedelta

from sqlalchemy.orm import Session

from app.models.ai_insight import AIInsight
from app.models.time_entry import TimeEntry
from app.services.anomaly import detect_anomalies, save_anomaly_insights
from app.services.burnout import calculate_burnout_risk, save_burnout_insight
from app.services.delivery_forecast import (
    calculate_delivery_forecast,
    save_delivery_forecast_insight,
)
from app.services.productivity import (
    calculate_productivity_score,
    get_projects_with_tasks_in_period,
    save_productivity_insight,
)
from app.services.task_switching import calculate_task_switching, save_task_switching_insight
from app.services.weekly_summary import generate_weekly_summary, save_weekly_summary_insight

logger = logging.getLogger(__name__)


def run_nightly_insights_job(db: Session, period_start: date, period_end: date) -> None:
    members = _get_active_members(db)
    window = (period_start, period_end)

    for member_id in members:
        # each pass is isolated so one failure cannot starve the later insight types
        _run_safely(db, "productivity", member_id, _run_productivity_passes, *window)
        _run_safely(db, "task switching", member_id, _run_task_switching_passes, *window)
        _run_safely(db, "burnout", member_id, _run_burnout_pass, *window)
        _run_safely(db, "anomaly", member_id, _run_anomaly_pass, *window)

    tasks = _get_active_tasks(db)
    for task_id in tasks:
        _run_safely(db, "delivery forecast", task_id, _run_delivery_forecast_pass)


def run_weekly_summary_job(db: Session, week_start: date) -> None:
    members = _get_active_members(db)

    for member_id in members:
        result = generate_weekly_summary(db, member_id, "USER", week_start)
        save_weekly_summary_insight(db, member_id, "USER", result)

    workspace_ids = _get_active_workspaces(db)
    for workspace_id in workspace_ids:
        result = generate_weekly_summary(db, workspace_id, "TEAM", week_start)
        save_weekly_summary_insight(db, workspace_id, "TEAM", result)


def _run_safely(
    db: Session,
    pass_name: str,
    subject_id: uuid.UUID,
    run_pass: Callable[..., None],
    *args: date,
) -> None:
    try:
        run_pass(db, subject_id, *args)
    except Exception:
        # roll back the failed pass so the session is clean for the next one
        db.rollback()
        logger.exception("%s pass failed for %s", pass_name, subject_id)


def _run_productivity_passes(db: Session, member_id, period_start, period_end) -> None:
    overall = calculate_productivity_score(db, member_id, period_start, period_end)
    if overall["score"] is not None:
        save_productivity_insight(db, member_id, overall, project_id=None)

    project_ids = get_projects_with_tasks_in_period(db, member_id, period_start, period_end)
    for project_id in project_ids:
        scoped = calculate_productivity_score(
            db, member_id, period_start, period_end, project_id=project_id
        )
        if scoped["score"] is not None:
            save_productivity_insight(db, member_id, scoped, project_id=project_id)


def _run_task_switching_passes(db: Session, member_id, period_start, period_end) -> None:
    overall = calculate_task_switching(db, member_id, period_start, period_end)
    save_task_switching_insight(db, member_id, overall, project_id=None)

    project_ids = get_projects_with_tasks_in_period(db, member_id, period_start, period_end)
    for project_id in project_ids:
        scoped = calculate_task_switching(
            db, member_id, period_start, period_end, project_id=project_id
        )
        save_task_switching_insight(db, member_id, scoped, project_id=project_id)


def _run_burnout_pass(db: Session, member_id, period_start, period_end) -> None:
    result = calculate_burnout_risk(db, member_id, period_start, period_end)
    save_burnout_insight(db, member_id, result)


def _run_anomaly_pass(
    db: Session, member_id: uuid.UUID, period_start: date, period_end: date
) -> None:
    window_start = datetime.combine(period_start, time.min)
    # end is exclusive so the whole last day is included
    window_end = datetime.combine(period_end + timedelta(days=1), time.min)

    rows = (
        db.query(TimeEntry.id)
        .filter(
            TimeEntry.workspace_member_id == member_id,
            TimeEntry.is_deleted.is_(False),
            TimeEntry.start_time >= window_start,
            TimeEntry.start_time < window_end,
        )
        .all()
    )

    # skip entries already flagged, otherwise every rerun inserts duplicate anomaly rows
    flagged_rows = (
        db.query(AIInsight.time_entry_id)
        .filter(
            AIInsight.workspace_member_id == member_id,
            AIInsight.insight_type == "ANOMALY",
            AIInsight.time_entry_id.isnot(None),
        )
        .all()
    )
    flagged = {row[0] for row in flagged_rows}
    entry_ids = [row[0] for row in rows if row[0] not in flagged]
    if not entry_ids:
        return

    anomalies = detect_anomalies(db, member_id, entry_ids)
    if anomalies:
        save_anomaly_insights(db, member_id, anomalies)


def _run_delivery_forecast_pass(db: Session, task_id) -> None:
    from app.models.task import Task

    result = calculate_delivery_forecast(db, task_id)
    if result["forecast_date"] is None and result["planned_date"] is None:
        return

    task = db.query(Task).filter(Task.id == task_id).first()
    if task is not None:
        save_delivery_forecast_insight(
            db, task.assigned_workspace_member_id, task.project_id, result
        )


def _get_active_members(db: Session):
    from app.models.user import User
    from app.models.workspace_member import WorkspaceMember

    rows = (
        db.query(WorkspaceMember.id)
        .join(User, WorkspaceMember.user_id == User.id)
        .filter(User.status == "ACTIVE")
        .all()
    )
    return [row[0] for row in rows]


def _get_active_tasks(db: Session):
    from app.models.task import Task

    rows = db.query(Task.id).filter(Task.status != "DONE", Task.is_deleted.is_(False)).all()
    return [row[0] for row in rows]


def _get_active_workspaces(db: Session):
    from app.models.workspace import Workspace

    rows = db.query(Workspace.id).all()
    return [row[0] for row in rows]
