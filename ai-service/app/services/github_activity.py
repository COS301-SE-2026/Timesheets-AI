"""
This file calculates git hub commit activity vs hours logged this is deterministic counting not an ai insigh

Author: Zamokuhle Zwane
Date: 01/09/2026
"""

from datetime import datetime
from uuid import UUID

from sqlalchemy import text
from sqlalchemy.orm import Session


def get_github_activity(
    db: Session,
    workspace_member_id: UUID,
    start: datetime,
    end: datetime,
) -> dict:
    # real V1 git_commits columns: lines_added, lines_removed, not additions/deletions
    github_rows = (
        db.execute(
            text(
                """
            SELECT repository_name, commit_time, lines_added, lines_removed
            FROM git_commits
            WHERE workspace_member_id = :member_id
              AND commit_time >= :start_time
              AND commit_time <= :end_time
            ORDER BY commit_time ASC
            """
            ),
            {"member_id": workspace_member_id, "start_time": start, "end_time": end},
        )
        .mappings()
        .all()
    )

    time_rows = (
        db.execute(
            text(
                """
            SELECT duration_seconds
            FROM time_entries
            WHERE workspace_member_id = :member_id
              AND start_time >= :start_time
              AND start_time <= :end_time
              AND is_deleted = false
            """
            ),
            {"member_id": workspace_member_id, "start_time": start, "end_time": end},
        )
        .mappings()
        .all()
    )

    hours_logged = sum(float(row["duration_seconds"] or 0) for row in time_rows) / 3600.0

    commit_count = len(github_rows)
    additions = sum(int(row["lines_added"] or 0) for row in github_rows)
    deletions = sum(int(row["lines_removed"] or 0) for row in github_rows)

    repositories = {row["repository_name"] for row in github_rows if row["repository_name"]}
    active_days = {row["commit_time"].date() for row in github_rows if row["commit_time"]}

    commits_per_hour = commit_count / hours_logged if hours_logged > 0 else 0.0

    alignment = None
    explanation = None

    # not calculating an alignment verdict until there's enough signal, a fake "0.00" is worse than showing nothing for a brand new github connection
    if hours_logged > 0 and commit_count == 0:
        alignment = "LOW_ACTIVITY"
        explanation = "Time was logged during this period, but no GitHub commits were detected."
    elif hours_logged > 0 and commits_per_hour >= 1:
        alignment = "ACTIVE"
        explanation = "GitHub activity was present across the logged work period."
    elif hours_logged > 0:
        alignment = "MODERATE"
        explanation = "GitHub activity was present but relatively light compared with logged time."

    return {
        "connected": True,
        "hours_logged": round(hours_logged, 2),
        "commit_count": commit_count,
        "commits_per_hour": round(commits_per_hour, 2),
        "additions": additions,
        "deletions": deletions,
        "active_repositories": len(repositories),
        "active_days": len(active_days),
        "alignment": alignment,
        "explanation": explanation,
    }
