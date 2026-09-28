"""
This file handles the deterministic (non-ML) aggregates for the Manager Insights page: completion forecast, logged hours, task overview, velocity,
project health, and jira ticket breakdown by type.

Same pattern as github_activity.py and dashboard_aggregates.py, raw SQL via text(), no ML, no Gemini call. None of this writes to ai_insights, these are
computed live per request.

Author: Zamokuhle Zwane
Date: 26/09/2026
"""

from datetime import date, datetime, time, timedelta
from uuid import UUID

from sqlalchemy import text
from sqlalchemy.orm import Session

WEEKS_LOOKBACK = 6  # matches the wireframe's "last 6 weeks" completion chart


def project_completion_forecast(db: Session, project_id: UUID) -> dict:
    total_tasks = (
        db.execute(
            text(
                """
                SELECT COUNT(*) AS total
                FROM tasks
                WHERE project_id = :project_id AND is_deleted = false
                """
            ),
            {"project_id": project_id},
        )
        .mappings()
        .first()["total"]
    )

    if total_tasks == 0:
        return {
            "weekly_progress": [],
            "current_progress_percent": 0.0,
            "forecast_completion_date": None,
        }

    today = date.today()
    weekly_progress = []
    for week_offset in range(WEEKS_LOOKBACK - 1, -1, -1):
        week_end = today - timedelta(weeks=week_offset)
        done_by_week = (
            db.execute(
                text(
                    """
                    SELECT COUNT(*) AS done_count
                    FROM tasks
                    WHERE project_id = :project_id
                      AND is_deleted = false
                      AND status = 'DONE'
                      AND completed_at IS NOT NULL
                      AND completed_at <= :week_end
                    """
                ),
                {
                    "project_id": project_id,
                    "week_end": datetime.combine(week_end, time.max),
                },
            )
            .mappings()
            .first()["done_count"]
        )
        percent = round((done_by_week / total_tasks) * 100, 1)
        weekly_progress.append({"week_label": week_end.strftime("%b %-d"), "percent": percent})

    current_progress = weekly_progress[-1]["percent"]

    # linear projection: slope between the first and last of the 6 points, projected forward until it hits 100%. floored the slope so a project with
    # zero movement over 6 weeks doesn't divide by zero or go backwards
    first_percent = weekly_progress[0]["percent"]
    weeks_elapsed = WEEKS_LOOKBACK - 1
    weekly_rate = (current_progress - first_percent) / weeks_elapsed if weeks_elapsed > 0 else 0.0

    if weekly_rate <= 0.01 or current_progress >= 100:
        # no meaningful upward trend, or already done, can't project a future date honestly
        forecast_date = today if current_progress >= 100 else None
    else:
        weeks_to_finish = (100 - current_progress) / weekly_rate
        forecast_date = today + timedelta(weeks=round(weeks_to_finish, 1))

    return {
        "weekly_progress": weekly_progress,
        "current_progress_percent": current_progress,
        "forecast_completion_date": forecast_date,
    }


def team_logged_hours(db: Session, project_id: UUID, period_start: date, period_end: date) -> dict:
    rows = (
        db.execute(
            text(
                """
                SELECT u.first_name, u.last_name, SUM(te.duration_seconds) AS total_seconds
                FROM time_entries te
                JOIN workspace_members wm ON wm.id = te.workspace_member_id
                JOIN users u ON u.id = wm.user_id
                WHERE te.project_id = :project_id
                  AND te.is_deleted = false
                  AND te.start_time >= :start_time
                  AND te.start_time <= :end_time
                GROUP BY u.first_name, u.last_name
                """
            ),
            {
                "project_id": project_id,
                "start_time": datetime.combine(period_start, time.min),
                "end_time": datetime.combine(period_end, time.max),
            },
        )
        .mappings()
        .all()
    )

    members = [
        {
            "member_name": f"{row['first_name']} {row['last_name']}",
            "hours": round((row["total_seconds"] or 0) / 3600.0, 1),
        }
        for row in rows
    ]
    members.sort(key=lambda m: m["hours"], reverse=True)
    return {"members": members}


def task_overview(db: Session, project_id: UUID) -> dict:
    rows = (
        db.execute(
            text(
                """
                SELECT status, priority, COUNT(*) AS count
                FROM tasks
                WHERE project_id = :project_id AND is_deleted = false
                GROUP BY status, priority
                """
            ),
            {"project_id": project_id},
        )
        .mappings()
        .all()
    )

    status_counts: dict[str, int] = {"TODO": 0, "IN_PROGRESS": 0, "DONE": 0, "BLOCKED": 0}
    priority_counts: dict[str, int] = {}
    for row in rows:
        status_counts[row["status"]] = status_counts.get(row["status"], 0) + row["count"]
        if row["priority"]:
            priority_counts[row["priority"]] = (
                priority_counts.get(row["priority"], 0) + row["count"]
            )

    total = sum(status_counts.values())
    blocked_count = status_counts.get("BLOCKED", 0)

    # only build a "most open tasks are X priority" clause if there's an open task at all,
    # avoids a nonsense sentence on an empty/all-done project
    open_statuses_priority_counts: dict[str, int] = {}
    for row in rows:
        if row["status"] in ("TODO", "IN_PROGRESS") and row["priority"]:
            open_statuses_priority_counts[row["priority"]] = (
                open_statuses_priority_counts.get(row["priority"], 0) + row["count"]
            )

    if open_statuses_priority_counts:
        top_priority = max(open_statuses_priority_counts, key=open_statuses_priority_counts.get)
        narrative = f"Most open tasks are {top_priority.title()} priority"
        if blocked_count > 0:
            narrative += (
                f"; {blocked_count} task{'s' if blocked_count != 1 else ''} are currently blocked."
            )
        else:
            narrative += "."
    elif blocked_count > 0:
        narrative = (
            f"{blocked_count} task{'s' if blocked_count != 1 else ''} are currently blocked."
        )
    else:
        narrative = "No open tasks right now."

    return {
        "total": total,
        "todo": status_counts.get("TODO", 0),
        "in_progress": status_counts.get("IN_PROGRESS", 0),
        "done": status_counts.get("DONE", 0),
        "blocked": blocked_count,
        "narrative": narrative,
    }


def velocity(db: Session, project_id: UUID, weeks: int = 8) -> dict:
    """
    count of tasks completed per week for the last N weeks, plus a rolling
    average across those same weeks, for the "Velocity" line + dashed average.
    """
    today = date.today()
    weekly_counts = []
    for week_offset in range(weeks - 1, -1, -1):
        week_end = today - timedelta(weeks=week_offset)
        week_start = week_end - timedelta(days=6)
        count = (
            db.execute(
                text(
                    """
                    SELECT COUNT(*) AS done_count
                    FROM tasks
                    WHERE project_id = :project_id
                      AND is_deleted = false
                      AND status = 'DONE'
                      AND completed_at IS NOT NULL
                      AND completed_at >= :week_start
                      AND completed_at <= :week_end
                    """
                ),
                {
                    "project_id": project_id,
                    "week_start": datetime.combine(week_start, time.min),
                    "week_end": datetime.combine(week_end, time.max),
                },
            )
            .mappings()
            .first()["done_count"]
        )
        weekly_counts.append({"week_label": f"W{weeks - week_offset}", "completed": count})

    average = (
        round(sum(w["completed"] for w in weekly_counts) / len(weekly_counts), 1)
        if weekly_counts
        else 0.0
    )

    return {"weeks": weekly_counts, "rolling_average": average}


def project_health(db: Session, project_id: UUID) -> dict:
    """
    composite 0-100 score, weighted blend of on-time task %, estimate accuracy %, and blocked-task ratio
    weights: on_time 50%, estimate_accuracy 30%, blocked_ratio 20%
    """
    rows = (
        db.execute(
            text(
                """
                SELECT status, due_date, completed_at, estimated_hours, actual_hours
                FROM tasks
                WHERE project_id = :project_id AND is_deleted = false
                """
            ),
            {"project_id": project_id},
        )
        .mappings()
        .all()
    )

    if not rows:
        return {
            "score": 0,
            "status_label": "No data",
            "on_track_percent": 0.0,
            "at_risk_percent": 0.0,
            "behind_percent": 0.0,
        }

    total = len(rows)
    on_time_done = 0
    done_with_due_date = 0
    estimate_matches = []
    blocked_count = 0

    on_track_count = 0
    behind_count = 0
    at_risk_count = 0

    today = date.today()

    for row in rows:
        if row["status"] == "BLOCKED":
            blocked_count += 1

        if row["status"] == "DONE":
            if row["due_date"] is not None and row["completed_at"] is not None:
                done_with_due_date += 1
                if row["completed_at"].date() <= row["due_date"]:
                    on_time_done += 1
                    on_track_count += 1
                else:
                    behind_count += 1  # finished, but late
            else:
                on_track_count += 1  # no due date to miss, doesn't count against on-time %
        else:
            if row["due_date"] is not None and row["due_date"] < today:
                behind_count += 1
            else:
                at_risk_count += 1

        if (
            row["estimated_hours"] is not None
            and row["actual_hours"] is not None
            and row["estimated_hours"] > 0
        ):
            accuracy = 1 - (
                abs(float(row["actual_hours"]) - float(row["estimated_hours"]))
                / float(row["estimated_hours"])
            )
            estimate_matches.append(max(accuracy, 0.0))

    on_time_percent = (on_time_done / done_with_due_date * 100) if done_with_due_date > 0 else 100.0
    estimate_accuracy_percent = (
        (sum(estimate_matches) / len(estimate_matches) * 100) if estimate_matches else 100.0
    )
    blocked_ratio_percent = (blocked_count / total) * 100

    score = round(
        (on_time_percent * 0.5)
        + (estimate_accuracy_percent * 0.3)
        + ((100 - blocked_ratio_percent) * 0.2)
    )
    score = max(0, min(100, score))

    if score >= 75:
        status_label = "On Track"
    elif score >= 50:
        status_label = "At Risk"
    else:
        status_label = "Behind"

    return {
        "score": score,
        "status_label": status_label,
        "on_track_percent": round((on_track_count / total) * 100, 1),
        "at_risk_percent": round((at_risk_count / total) * 100, 1),
        "behind_percent": round((behind_count / total) * 100, 1),
    }


def jira_ticket_breakdown(db: Session, project_id: UUID) -> dict:
    """count of jira_tickets grouped by issue_type, for the "Jira Tickets" donut."""
    rows = (
        db.execute(
            text(
                """
                SELECT issue_type, COUNT(*) AS count
                FROM jira_tickets
                WHERE project_id = :project_id
                GROUP BY issue_type
                """
            ),
            {"project_id": project_id},
        )
        .mappings()
        .all()
    )

    total = sum(row["count"] for row in rows)
    return {
        "total": total,
        "by_type": [
            {
                "issue_type": row["issue_type"] or "Unknown",
                "count": row["count"],
                "percentage": round((row["count"] / total) * 100, 1) if total > 0 else 0.0,
            }
            for row in sorted(rows, key=lambda r: r["count"], reverse=True)
        ],
    }


BURNOUT_FLAG_THRESHOLD = 60.0
BURNOUT_HIGH_RISK_THRESHOLD = 80.0


def flagged_burnout_members(db: Session, project_id: UUID) -> list[dict]:
    from app.models.ai_insight import AIInsight

    project_member_rows = (
        db.execute(
            text(
                "SELECT workspace_member_id FROM project_members WHERE project_id = :project_id AND is_active = true"
            ),
            {"project_id": project_id},
        )
        .mappings()
        .all()
    )
    project_member_ids = [row["workspace_member_id"] for row in project_member_rows]
    if not project_member_ids:
        return []

    insights = (
        db.query(AIInsight)
        .filter(
            AIInsight.insight_type == "BURNOUT",
            AIInsight.resolved.is_(False),
            AIInsight.workspace_member_id.in_(project_member_ids),
        )
        .order_by(AIInsight.created_at.desc())
        .all()
    )

    seen_members = set()
    results = []
    for insight in insights:
        if insight.workspace_member_id in seen_members:
            continue
        if insight.score is None:
            continue  # older rows from before this patch, genuinely no score to show

        score = float(insight.score)
        if score < BURNOUT_FLAG_THRESHOLD:
            continue

        seen_members.add(insight.workspace_member_id)
        results.append(
            {
                "workspace_member_id": insight.workspace_member_id,
                "risk_score": score,
                "status_band": "HIGH_RISK" if score >= BURNOUT_HIGH_RISK_THRESHOLD else "AT_RISK",
                "reason": insight.description,
                "insight_id": insight.id,
            }
        )

    return results
