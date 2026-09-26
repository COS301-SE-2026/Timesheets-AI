"""
This file handles deterministic (non-ML) aggregates for the Developer Insights page:
time allocation, estimate-vs-actual, cycle time, and time split by task.
Same pattern as github_activity.py - raw SQL via text(), no ML, no Gemini call,
not persisted as ai_insights rows.

Author: Zamokuhle Zwane
Date: 23/09/2026
"""

from datetime import date, datetime, time
from uuid import UUID

from sqlalchemy import text
from sqlalchemy.orm import Session

# tasks has no category/label column (confirmed against app/models/task.py), so time_allocation and estimate_vs_actual both bucket
# by a keyword heuristic on the task title instead. Called out in the
_CATEGORY_KEYWORDS: dict[str, tuple[str, ...]] = {
    "Bug Fixes": ("fix", "bug", "hotfix", "patch"),
    "Code Review": ("review", "pr ", "pull request"),
    "Feature Dev": ("feature", "implement", "add ", "build "),
}
_DEFAULT_CATEGORY = "Other"
_MEETING_KEYWORDS = ("meeting", "standup", "stand-up", "sync", "call")


def categorize_task_title(title: str | None) -> str:
    """
    categorises tasks Checked in a fixed order so "code review for the login fix" lands under
    Bug Fixes before Code Review would grab it, a title mentioning a bug
    wins priority since that's what actually blocks a release
    """
    if not title:
        return _DEFAULT_CATEGORY

    lowered = title.lower()

    if any(keyword in lowered for keyword in _MEETING_KEYWORDS):
        return "Meetings"

    for category in ("Bug Fixes", "Code Review", "Feature Dev"):
        if any(keyword in lowered for keyword in _CATEGORY_KEYWORDS[category]):
            return category

    return _DEFAULT_CATEGORY


def time_allocation(
    db: Session, workspace_member_id: UUID, period_start: date, period_end: date
) -> dict:
    """
    Sum(time_entries.duration_seconds) grouped by task category, for the
    "Time Allocation" donut. Entries with no task_id (ad-hoc logging) fall
    back to categorizing off the entry's own description.
    """
    rows = (
        db.execute(
            text(
                """
                SELECT te.duration_seconds, te.description AS entry_description, t.title AS task_title
                FROM time_entries te
                LEFT JOIN tasks t ON t.id = te.task_id
                WHERE te.workspace_member_id = :member_id
                  AND te.start_time >= :start_time
                  AND te.start_time <= :end_time
                  AND te.is_deleted = false
                """
            ),
            {
                "member_id": workspace_member_id,
                "start_time": datetime.combine(period_start, time.min),
                "end_time": datetime.combine(period_end, time.max),
            },
        )
        .mappings()
        .all()
    )

    totals: dict[str, float] = {}
    for row in rows:
        title_to_categorize = row["task_title"] or row["entry_description"]
        category = categorize_task_title(title_to_categorize)
        hours = float(row["duration_seconds"] or 0) / 3600.0
        totals[category] = totals.get(category, 0.0) + hours

    total_hours = round(sum(totals.values()), 2)

    return {
        "total_hours": total_hours,
        "categories": [
            {
                "category": category,
                "hours": round(hours, 2),
                # avoid a divide-by-zero when there's no logged time yet at all
                "percentage": round((hours / total_hours) * 100, 1) if total_hours > 0 else 0.0,
            }
            for category, hours in sorted(totals.items(), key=lambda item: item[1], reverse=True)
        ],
    }


def estimate_vs_actual(
    db: Session, workspace_member_id: UUID, period_start: date, period_end: date
) -> dict:
    """
    Sum(estimated_hours) vs sum(actual_hours) from tasks assigned to this dev,
    grouped by the same category bucket as time_allocation.
    """
    rows = (
        db.execute(
            text(
                """
                SELECT title, estimated_hours, actual_hours
                FROM tasks
                WHERE assigned_workspace_member_id = :member_id
                  AND due_date >= :period_start
                  AND due_date <= :period_end
                  AND is_deleted = false
                  AND estimated_hours IS NOT NULL
                """
            ),
            {
                "member_id": workspace_member_id,
                "period_start": period_start,
                "period_end": period_end,
            },
        )
        .mappings()
        .all()
    )

    buckets: dict[str, dict[str, float]] = {}
    for row in rows:
        category = categorize_task_title(row["title"])
        bucket = buckets.setdefault(category, {"estimated": 0.0, "actual": 0.0})
        bucket["estimated"] += float(row["estimated_hours"] or 0)
        bucket["actual"] += float(row["actual_hours"] or 0)

    return {
        "categories": [
            {
                "category": category,
                "estimated_hours": round(values["estimated"], 2),
                "actual_hours": round(values["actual"], 2),
            }
            for category, values in sorted(
                buckets.items(), key=lambda item: item[1]["estimated"], reverse=True
            )
        ]
    }


def cycle_time_stats(
    db: Session, workspace_member_id: UUID, period_start: date, period_end: date
) -> dict:
    """
    Average days from task creation to completion, for tasks this dev completed in the period. Uses created_at -> completed_at since tasks
    have no "started_at" column, measures full lifecycle time, not active work time.
    """
    rows = (
        db.execute(
            text(
                """
                SELECT created_at, completed_at
                FROM tasks
                WHERE assigned_workspace_member_id = :member_id
                  AND status = 'DONE'
                  AND completed_at IS NOT NULL
                  AND completed_at >= :start_time
                  AND completed_at <= :end_time
                  AND is_deleted = false
                """
            ),
            {
                "member_id": workspace_member_id,
                "start_time": datetime.combine(period_start, time.min),
                "end_time": datetime.combine(period_end, time.max),
            },
        )
        .mappings()
        .all()
    )

    if not rows:
        return {"average_days": None, "task_count": 0}

    total_days = sum(
        (row["completed_at"] - row["created_at"]).total_seconds() / 86400.0 for row in rows
    )
    return {"average_days": round(total_days / len(rows), 2), "task_count": len(rows)}


def time_split_by_task(
    db: Session, workspace_member_id: UUID, period_start: date, period_end: date, top_n: int = 5
) -> dict:
    """
    Sum(duration_seconds) grouped by the actual task title (not a category bucket) Only the top_n  tasks by hours are broken out individually; everything else collapses
    into "Other" so the legend doesn't grow unbounded on a busy dev top_n=5 is an arbitrary cutoff, not a spec'd number, worth checking with design before Demo 3.
    """
    rows = (
        db.execute(
            text(
                """
                SELECT COALESCE(t.title, 'Untracked time') AS task_title, te.duration_seconds
                FROM time_entries te
                LEFT JOIN tasks t ON t.id = te.task_id
                WHERE te.workspace_member_id = :member_id
                  AND te.start_time >= :start_time
                  AND te.start_time <= :end_time
                  AND te.is_deleted = false
                """
            ),
            {
                "member_id": workspace_member_id,
                "start_time": datetime.combine(period_start, time.min),
                "end_time": datetime.combine(period_end, time.max),
            },
        )
        .mappings()
        .all()
    )

    totals: dict[str, float] = {}
    for row in rows:
        hours = float(row["duration_seconds"] or 0) / 3600.0
        totals[row["task_title"]] = totals.get(row["task_title"], 0.0) + hours

    total_hours = round(sum(totals.values()), 2)
    ranked = sorted(totals.items(), key=lambda item: item[1], reverse=True)

    top_tasks = ranked[:top_n]
    other_hours = sum(hours for _, hours in ranked[top_n:])
    if other_hours > 0:
        top_tasks.append(("Other", other_hours))

    return {
        "total_hours": total_hours,
        "tasks": [
            {
                "task_title": title,
                "hours": round(hours, 2),
                "percentage": round((hours / total_hours) * 100, 1) if total_hours > 0 else 0.0,
            }
            for title, hours in top_tasks
        ],
    }


def task_switching_by_day(
    db: Session, workspace_member_id: UUID, period_start: date, period_end: date
) -> dict:
    """
    Task-switching count bucketed by day-of-week (Mon-Sun), summed across every occurrence of that weekday within the period. Same switch
    definition as task_switching.py's calculate_task_switching (consecutive same-day entries with a different task_id) - that function computes this
    same per-day dict internally but discards it down to one period average This duplicates the counting logic rather than importing it, since the
    two need different final shapes.
    """
    rows = (
        db.execute(
            text(
                """
                SELECT start_time, task_id
                FROM time_entries
                WHERE workspace_member_id = :member_id
                  AND start_time >= :start_time
                  AND start_time <= :end_time
                  AND is_deleted = false
                  AND task_id IS NOT NULL
                ORDER BY start_time ASC
                """
            ),
            {
                "member_id": workspace_member_id,
                "start_time": datetime.combine(period_start, time.min),
                "end_time": datetime.combine(period_end, time.max),
            },
        )
        .mappings()
        .all()
    )

    weekday_labels = ["Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"]
    switches_by_weekday = {label: 0 for label in weekday_labels}

    previous_task_id = None
    previous_day = None
    for row in rows:
        entry_day = row["start_time"].date()
        if (
            previous_task_id is not None
            and entry_day == previous_day
            and row["task_id"] != previous_task_id
        ):
            switches_by_weekday[weekday_labels[entry_day.weekday()]] += 1
        previous_task_id = row["task_id"]
        previous_day = entry_day

    return {
        "days": [
            {"day_label": label, "switches": switches_by_weekday[label]} for label in weekday_labels
        ]
    }
