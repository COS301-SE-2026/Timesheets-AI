"""
Project-level delivery forecasting service.

This service calculates a project forecast from existing Momently data:
- project budget and planned dates
- task estimates and completion
- actual logged time
- recent team velocity

This is a heuristic forecast, not a trained prediction model.

- the delivery forecast service

the service calculates:
    - how much time has been logged?
    - how much estimated work remains?
    - with the pace of the team, when will that work finish?
    - is the schedule at risk?

Author: Nyasha
Date: 22/09/2026
"""

import uuid
from datetime import date, datetime, timedelta, timezone

from sqlalchemy import func, select
from sqlalchemy.orm import Session

from app.models.project import Project
from app.models.project_member import ProjectMember
from app.models.task import Task
from app.models.time_entry import TimeEntry
from app.services.project_forecast_evidence import get_project_forecast_evidence
from app.services.project_forecast_explanation import (
    generate_project_forecast_explanation,
)

# I am using the last 14 days of the teams activity
# choosing this window so we see how a team is doing for an extended period of time
LOOKBACK_DAYS = 14
MIN_WEEKLY_VELOCITY_HOURS = 1.0
MIN_RECENT_ACTIVITY_HOURS = 1.0

BUDGET_WARNING_THRESHOLD_PERCENT = 5.0
BUDGET_AT_RISK_THRESHOLD_PERCENT = 15.0

SCHEDULE_WARNING_DELAY_DAYS = 3
SCHEDULE_AT_RISK_DELAY_DAYS = 7

MIN_TASK_ESTIMATE_COVERAGE = 0.80


def calculate_project_forecast(
    db: Session,
    project_id: uuid.UUID,
    access_token: str | None = None,
) -> dict | None:
    """
    - this is the main function, it is what will bring the project, its tasks, and calculate the different forecast sections
    """

    project = (
        db.query(Project).filter(Project.id == project_id, Project.is_deleted.is_(False)).first()
    )

    if project is None:
        return None

    # external evidence uses the same recent period as the team velocity
    evidence_end_time = datetime.now(timezone.utc).replace(tzinfo=None)
    evidence_start_time = evidence_end_time - timedelta(days=LOOKBACK_DAYS)

    tasks = db.query(Task).filter(Task.project_id == project_id, Task.is_deleted.is_(False)).all()

    used_hours = _get_used_hours(db, project_id)
    task_progress = _calculate_task_progress(tasks)

    velocity_data = _get_recent_team_velocity(db, project_id)
    weekly_velocity = velocity_data["weekly_velocity"]

    forecast_end_date = _calculate_forecast_end_date(
        task_progress["estimated_remaining_hours"], weekly_velocity
    )

    budget = _calculate_budget_forecast(
        project, used_hours, task_progress["estimated_remaining_hours"]
    )

    schedule = _calculate_schedule_forecast(project, forecast_end_date)

    risk = _calculate_risk(budget, schedule, task_progress)

    external_evidence = None

    # external evidence is supporting information only
    # the project forecast should still work if it cannot be retrieved
    if access_token:
        external_evidence = get_project_forecast_evidence(
            project_id=project_id,
            start_time=evidence_start_time,
            end_time=evidence_end_time,
            access_token=access_token,
        )

    confidence = _calculate_forecast_confidence(
        project,
        tasks,
        velocity_data,
        external_evidence,
    )

    forecast = {
        "project_id": project.id,
        "project_name": project.name,
        "budget": budget,
        "schedule": schedule,
        "tasks": task_progress,
        "velocity": {
            "recent_hours_per_week": round(weekly_velocity, 2),
            "lookback_days": LOOKBACK_DAYS,
            "hours_in_period": velocity_data["hours_in_period"],
            "has_sufficient_data": velocity_data["has_sufficient_data"],
        },
        "risk": risk,
        "confidence": confidence,
        "external_evidence": (
            external_evidence.model_dump(by_alias=True) if external_evidence is not None else None
        ),
    }

    ai_explanation = generate_project_forecast_explanation(forecast)

    forecast["ai_explanation"] = ai_explanation.model_dump() if ai_explanation is not None else None

    return forecast


def _get_used_hours(db: Session, project_id: uuid.UUID) -> float:
    """
    - calculates the total number of hours logged for the project
    - using the time entries are used cause they have the actual time
    - I am making sure that time entries and entries without a duration should not be included
    """

    # adds together the duration of all valid time entries belonging to the project
    total_seconds = (
        db.query(func.coalesce(func.sum(TimeEntry.duration_seconds), 0))
        .filter(
            TimeEntry.project_id == project_id,
            TimeEntry.is_deleted.is_(False),
            TimeEntry.duration_seconds.isnot(None),
        )
        .scalar()
    )

    # duration stored in seconds so we divide by 3600,
    return round(float(total_seconds) / 3600, 2)


def _calculate_task_progress(tasks: list[Task]) -> dict:
    """
    - calculates the total number of hours logged for the project
    - using the time entries are used cause they have the actual time
    - I am making sure that time entries and entries without a duration should not be included
    """
    total_tasks = len(tasks)

    completed_tasks = sum(1 for task in tasks if task.status == "DONE")
    remaining_tasks = total_tasks - completed_tasks

    completion_percentage = (completed_tasks / total_tasks) * 100 if total_tasks > 0 else 0.0
    estimated_remaining_hours = 0.0

    for task in tasks:
        if task.status == "DONE":
            continue

        # if estimated hours has no number 0 should be used to maintain consistency
        estimated = float(task.estimated_hours or 0)
        actual = float(task.actual_hours or 0)

        estimated_remaining_hours += max(estimated - actual, 0)

    return {
        "total_tasks": total_tasks,
        "completed_tasks": completed_tasks,
        "remaining_tasks": remaining_tasks,
        "completion_percentage": round(completion_percentage, 2),
        "estimated_remaining_hours": round(estimated_remaining_hours, 2),
    }


def _get_recent_team_velocity(db: Session, project_id: uuid.UUID) -> dict:
    """
    - should calculate the recent velocity of a team in a project
    - looks at the hours logged by active members for 14 days
    - these hours then get converted into a weekly rate
    - the weekly velocity helps to estimate how long remaining work can take
    """
    since = datetime.now(timezone.utc).replace(tzinfo=None) - timedelta(days=LOOKBACK_DAYS)

    # I do not want members who are not active to contribute to the team velocity
    active_member_ids = select(ProjectMember.workspace_member_id).where(
        ProjectMember.project_id == project_id,
        ProjectMember.is_active.is_(True),
    )

    total_seconds = (
        db.query(func.coalesce(func.sum(TimeEntry.duration_seconds), 0))
        .filter(
            TimeEntry.project_id == project_id,
            TimeEntry.workspace_member_id.in_(active_member_ids),
            TimeEntry.is_deleted.is_(False),
            TimeEntry.duration_seconds.isnot(None),
            TimeEntry.start_time >= since,
        )
        .scalar()
    )

    hours_in_period = float(total_seconds) / 3600

    # this will convert the hours from the 14-day period into an average weekly pace
    weekly_velocity = hours_in_period * (7 / LOOKBACK_DAYS)
    has_sufficient_data = hours_in_period >= MIN_RECENT_ACTIVITY_HOURS

    return {
        "weekly_velocity": max(
            weekly_velocity,
            MIN_WEEKLY_VELOCITY_HOURS,
        ),
        "hours_in_period": round(hours_in_period, 2),
        "has_sufficient_data": has_sufficient_data,
    }


def _calculate_forecast_end_date(
    remaining_hours: float,
    weekly_velocity: float,
) -> date:
    """
    - this will estimate when the remaining project work should be completed
    - it will use the remaining estimated hours and the teams recent weekly activity
    - I divide the weekly velocity to estimate how many weeks of work is left
    """
    if remaining_hours <= 0:
        return date.today()

    weeks_remaining = remaining_hours / weekly_velocity
    days_remaining = weeks_remaining * 7

    return date.today() + timedelta(days=round(days_remaining))


def _calculate_budget_forecast(
    project: Project,
    used_hours: float,
    remaining_estimated_hours: float,
) -> dict:
    """
    - calculate the hour budget forecast for the project
    - combines the hours already logged with estimate hours remaining
    - helps to see if the project will go over the allocated hours
    """
    budget_hours = float(project.budget_hours) if project.budget_hours is not None else None

    forecast_total_hours = used_hours + remaining_estimated_hours

    # if the project does not have budget hours then I cannot calculate the remaining
    # doing this so that I can handle all types of projects so that all kinds of projects are handled
    if budget_hours is None:
        remaining_budget_hours = None
        forecast_overrun_hours = None
    else:
        remaining_budget_hours = budget_hours - used_hours
        forecast_overrun_hours = max(
            forecast_total_hours - budget_hours,
            0,
        )
        # I want to prevent an under budget from giving negative

    return {
        "budget_hours": budget_hours,
        "used_hours": round(used_hours, 2),
        "remaining_budget_hours": (
            round(remaining_budget_hours, 2) if remaining_budget_hours is not None else None
        ),
        "forecast_total_hours": round(forecast_total_hours, 2),
        "forecast_overrun_hours": (
            round(forecast_overrun_hours, 2) if forecast_overrun_hours is not None else None
        ),
    }


def _calculate_schedule_forecast(
    project: Project,
    forecast_end_date: date,
) -> dict:
    """
    - this should compare the forecast completion date with the planned one
    - I have also now added timeline progress to later compare it to task completion for better risk calculation
    """
    planned_end_date = project.end_date
    delay_days = None
    timeline_progress_percentage = None

    if planned_end_date is not None:
        delay_days = max(
            (forecast_end_date - planned_end_date).days,
            0,
        )

    # both dates will be needed to see how much of the project timeline has passed
    if project.start_date is not None and planned_end_date is not None:
        total_project_days = (planned_end_date - project.start_date).days
        elapsed_project_days = (date.today() - project.start_date).days

        if total_project_days > 0:
            timeline_progress_percentage = (elapsed_project_days / total_project_days) * 100
            timeline_progress_percentage = max(
                0.0,
                min(timeline_progress_percentage, 100.0),
            )

    return {
        "start_date": project.start_date,
        "planned_end_date": planned_end_date,
        "forecast_end_date": forecast_end_date,
        "delay_days": delay_days,
        "timeline_progress_percentage": (
            round(timeline_progress_percentage, 2)
            if timeline_progress_percentage is not None
            else None
        ),
    }


def _calculate_risk(
    budget: dict,
    schedule: dict,
    task_progress: dict,
) -> dict:
    """
    - my helper func to create the risk indicators for the project
    - this will look at the budget forecast, schedule forecast and task progress individually
    - then I combine the risks into a final project risk
    - NOTE: I want to eventually improve them with GitHub, Jira, Calendar
    """
    budget_risk = "UNKNOWN"

    # if the project has budget hours, compare the forecast total with the allocated hours
    if budget["budget_hours"] is not None:
        budget_hours = budget["budget_hours"]
        forecast_overrun_hours = budget["forecast_overrun_hours"] or 0

        # I should treat the overrun differently
        # for example if budget = 100 hours, 105 hour is different from 160 hours
        if budget_hours > 0:
            budget_overrun_percentage = (forecast_overrun_hours / budget_hours) * 100

            if budget_overrun_percentage >= BUDGET_AT_RISK_THRESHOLD_PERCENT:
                budget_risk = "AT_RISK"
            elif budget_overrun_percentage >= BUDGET_WARNING_THRESHOLD_PERCENT:
                budget_risk = "WARNING"
            else:
                budget_risk = "HEALTHY"

    schedule_risk = "UNKNOWN"

    # if the project has an end date, and the forecast says it will finish after that date
    if schedule["planned_end_date"] is not None:
        delay_days = schedule["delay_days"] or 0

        if delay_days >= SCHEDULE_AT_RISK_DELAY_DAYS:
            schedule_risk = "AT_RISK"
        elif delay_days >= SCHEDULE_WARNING_DELAY_DAYS:
            schedule_risk = "WARNING"
        else:
            schedule_risk = "HEALTHY"

    # compares task completion against how much of the planned project timeline has passed
    timeline_progress = schedule["timeline_progress_percentage"]
    completion_percentage = task_progress["completion_percentage"]

    if task_progress["total_tasks"] == 0:
        task_progress_risk = "UNKNOWN"

    # if there are no project dates, keep the original task progress rule as a fallback
    elif timeline_progress is None:
        task_progress_risk = "WARNING" if completion_percentage < 50 else "HEALTHY"

    else:
        progress_gap = timeline_progress - completion_percentage

        if progress_gap <= 0:
            task_progress_risk = "HEALTHY"
        elif progress_gap < 20:
            task_progress_risk = "WARNING"
        else:
            task_progress_risk = "AT_RISK"

    known_risks = [
        budget_risk,
        schedule_risk,
        task_progress_risk,
    ]

    """
    - if it has at risk at all, overall project is at risk
    - if there is nothing at risk, but there is a warning then it gets a warning
    - if all of them cannot be calculated then overall unknown
    - otherwise healthy
    """
    if "AT_RISK" in known_risks:
        overall = "AT_RISK"
    elif "WARNING" in known_risks:
        overall = "WARNING"
    elif all(risk == "UNKNOWN" for risk in known_risks):
        overall = "UNKNOWN"
    else:
        overall = "HEALTHY"

    return {
        "budget": budget_risk,
        "schedule": schedule_risk,
        "task_progress": task_progress_risk,
        "overall": overall,
    }


def _calculate_forecast_confidence(
    project: Project,
    tasks: list[Task],
    velocity_data: dict,
    external_evidence,
) -> dict:
    """
    - this will check how much data was available for the forcast
    - each of the data add to the confidence level
    - I recoord the missing data so that the manager can see why the confidence is lower
    """

    evidence_available = 0
    missing_evidence = []

    # both the dates are needed to compare the progress with the planned timeline, so if missing added to the missing var
    if project.start_date is not None and project.end_date is not None:
        evidence_available += 1
    else:
        missing_evidence.append("PROJECT_DATES")

    # the budget hours will be needed to see if a project goes over the allocated time
    if project.budget_hours is not None and float(project.budget_hours) > 0:
        evidence_available += 1
    else:
        missing_evidence.append("BUDGET_HOURS")

    # tasks are needed to calculate how much of the project work is done
    if tasks:
        evidence_available += 1
    else:
        missing_evidence.append("TASKS")

    # the unfinished tasks are the ones affecting the forecast
    incomplete_tasks = [task for task in tasks if task.status != "DONE"]

    estimated_incomplete_tasks = [
        task for task in incomplete_tasks if task.estimated_hours is not None
    ]

    # if all tasks are already complete, estimates for remaining work are no longer needed
    if not incomplete_tasks and tasks:
        estimate_coverage_percentage = 100.0
        evidence_available += 1

    # if there are unfinished tasks, check how many of them have estimates
    elif incomplete_tasks:
        estimate_coverage = len(estimated_incomplete_tasks) / len(incomplete_tasks)
        estimate_coverage_percentage = round(estimate_coverage * 100, 2)

        # enough of the remaining tasks have estimates to support the forecast
        if estimate_coverage >= MIN_TASK_ESTIMATE_COVERAGE:
            evidence_available += 1
        else:
            missing_evidence.append("TASK_ESTIMATES")

    # there are no tasks so there cannot be any task estimate evidence
    else:
        estimate_coverage_percentage = 0.0
        missing_evidence.append("TASK_ESTIMATES")

    # this will check whether there was enough recent logged time to calculate the team velocity
    if velocity_data["has_sufficient_data"]:
        evidence_available += 1
    else:
        missing_evidence.append("RECENT_VELOCITY")

    has_external_evidence = external_evidence is not None and (
        external_evidence.github.available or external_evidence.jira.available
    )

    if has_external_evidence:
        evidence_available += 1
    else:
        missing_evidence.append("EXTERNAL_EVIDENCE")

    evidence_total = 6

    # if there is not enough data then I cannot have strong confidence in the forecast
    if evidence_available >= 5:
        level = "HIGH"
    elif evidence_available >= 3:
        level = "MEDIUM"
    else:
        level = "LOW"

    return {
        "level": level,
        "evidence_available": evidence_available,
        "evidence_total": evidence_total,
        "missing_evidence": missing_evidence,
        "task_estimate_coverage_percentage": estimate_coverage_percentage,
    }
