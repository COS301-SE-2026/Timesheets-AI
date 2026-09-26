"""
This file calculates git hub commit activity vs hours logged this is deterministic counting not an ai insigh

Author: Zamokuhle Zwane
Date: 01/09/2026

Patch: added alignment_score (Part 2, option A - real percentage instead of
just the categorical alignment field), via _get_team_avg_commits_per_hour.

Patch: fixed infinite recursion in the above - _get_team_avg_commits_per_hour
calls get_github_activity() per teammate, and that call was itself trying to
compute a team average again, forever. _compute_alignment is now an internal
flag: only the outermost/real caller (True, the default) computes the team
average; the recursive per-teammate lookups pass False so they just return
their own commits_per_hour without recursing further.
"""

from datetime import datetime
from uuid import UUID

from sqlalchemy import text
from sqlalchemy.orm import Session

from app.models.workspace_member import WorkspaceMember


def get_github_activity(
    db: Session,
    workspace_member_id: UUID,
    start: datetime,
    end: datetime,
    _compute_alignment: bool = True,
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

    # THE FIX: only compute the team average (which recurses into this same
    # function per teammate) when _compute_alignment is True. The recursive
    # calls below pass False, so they skip straight past this block instead
    # of trying to compute their own team average and recursing forever.
    alignment_score = None
    if _compute_alignment:
        team_avg_commits_per_hour = _get_team_avg_commits_per_hour(
            db, workspace_member_id, start, end
        )
        if team_avg_commits_per_hour is not None and team_avg_commits_per_hour > 0:
            alignment_score = round(min(commits_per_hour / team_avg_commits_per_hour, 1.0) * 100, 1)

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
        "alignment_score": alignment_score,
        "explanation": explanation,
    }


def _get_team_avg_commits_per_hour(
    db: Session, workspace_member_id: UUID, start: datetime, end: datetime
) -> float | None:
    """
    Same shape as task_switching.py's _get_team_switches_per_day: average
    commits-per-hour across every other member of this person's workspace,
    used as the alignment score's denominator.

    Calls get_github_activity(..., _compute_alignment=False) per teammate -
    that flag is the fix for the recursion bug: it tells the teammate's own
    get_github_activity() call not to try computing ITS OWN team average
    (which would call this function again, for their teammates, forever).
    """
    this_member = (
        db.query(WorkspaceMember).filter(WorkspaceMember.id == workspace_member_id).first()
    )
    if this_member is None:
        return None

    teammates = (
        db.query(WorkspaceMember.id)
        .filter(
            WorkspaceMember.workspace_id == this_member.workspace_id,
            WorkspaceMember.id != workspace_member_id,
        )
        .all()
    )
    teammate_ids = [row[0] for row in teammates]
    if not teammate_ids:
        return None

    rates = [
        get_github_activity(db, teammate_id, start, end, _compute_alignment=False)[
            "commits_per_hour"
        ]
        for teammate_id in teammate_ids
    ]
    non_zero_rates = [rate for rate in rates if rate > 0]
    if not non_zero_rates:
        return None

    return sum(non_zero_rates) / len(non_zero_rates)
