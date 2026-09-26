"""
This file handles the router for the ai dashboard endpoint

Author: Zamokuhle Zwane
Date: 01/09/2026

Patch: default get_dashboard to hide resolved insights unless include_resolved=true
Patch: added PATCH /insights/{insight_id}/resolve (V19)
Patch: added ?period= query param (4w/8w/12w, default 8w) and wired
productivity_trend, time_allocation, estimate_vs_actual, score_cards,
time_split_by_task into get_dashboard's response (Developer Insights v2)
"""

from datetime import date, datetime, timedelta, timezone
from typing import Annotated, Literal
from uuid import UUID

from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy.orm import Session

from app.database import get_db
from app.models.ai_insight import AIInsight
from app.models.project import Project
from app.models.user import User
from app.models.workspace_member import WorkspaceMember
from app.schemas.dashboard import (
    DashboardInsight,
    DashboardResponse,
    EstimateVsActual,
    ProductivityTrend,
    ProductivityTrendPoint,
    ResolveInsightRequest,
    ResolveInsightResponse,
    ScoreCard,
    TaskSwitchingByDay,
    TimeAllocation,
    TimeSplitByTask,
)
from app.services.dashboard_aggregates import estimate_vs_actual as compute_estimate_vs_actual
from app.services.dashboard_aggregates import task_switching_by_day as compute_task_switching_by_day
from app.services.dashboard_aggregates import time_allocation as compute_time_allocation
from app.services.dashboard_aggregates import time_split_by_task as compute_time_split_by_task
from app.services.github_activity import get_github_activity
from app.services.score_cards import build_score_cards

PeriodOption = Literal["4w", "8w", "12w"]
_PERIOD_TO_WEEKS = {"4w": 4, "8w": 8, "12w": 12}

router = APIRouter(
    prefix="/insights",
    tags=["Insights"],
)


def week_key_to_label(iso_year_week: tuple[int, int]) -> str:
    year, week = iso_year_week
    monday = date.fromisocalendar(year, week, 1)
    return monday.strftime("%b %-d")


def _get_productivity_trend(
    db: Session, workspace_member_id: UUID, weeks: int
) -> ProductivityTrend:
    """
    Buckets persisted PRODUCTIVITY insights (project_id is None, i.e. the overall score) into weekly points. If there's no history yet, falls back
    to computing the score live per week straight off calculate_productivity_score, real numbers, not fabricated history.
    """
    from app.services.productivity import calculate_productivity_score

    since = datetime.now(timezone.utc) - timedelta(weeks=weeks)
    member = db.query(WorkspaceMember).filter(WorkspaceMember.id == workspace_member_id).first()

    user_rows = (
        db.query(AIInsight)
        .filter(
            AIInsight.workspace_member_id == workspace_member_id,
            AIInsight.insight_type == "PRODUCTIVITY",
            AIInsight.project_id.is_(None),
            AIInsight.created_at >= since,
        )
        .order_by(AIInsight.created_at.asc())
        .all()
    )

    if user_rows:
        team_rows = (
            db.query(AIInsight)
            .filter(
                AIInsight.workspace_id == (member.workspace_id if member else None),
                AIInsight.insight_type == "PRODUCTIVITY",
                AIInsight.project_id.is_(None),
                AIInsight.created_at >= since,
            )
            .all()
        )
        user_by_week: dict[tuple[int, int], list[float]] = {}
        for row in user_rows:
            key = row.created_at.isocalendar()[:2]
            user_by_week.setdefault(key, []).append(float(row.score or 0))

        team_by_week: dict[tuple[int, int], list[float]] = {}
        for row in team_rows:
            key = row.created_at.isocalendar()[:2]
            team_by_week.setdefault(key, []).append(float(row.score or 0))

        points = [
            ProductivityTrendPoint(
                week_label=week_key_to_label(key),
                user_score=round(sum(scores) / len(scores), 2),
                team_average=(
                    round(sum(team_by_week[key]) / len(team_by_week[key]), 2)
                    if key in team_by_week
                    else None
                ),
            )
            for key, scores in sorted(user_by_week.items())
        ]
        return ProductivityTrend(points=points, is_synthetic=False)

    #fallback: no persisted history, compute live per week
    today = date.today()
    points = []
    teammate_ids = (
        [
            row[0]
            for row in db.query(WorkspaceMember.id)
            .filter(
                WorkspaceMember.workspace_id == member.workspace_id,
                WorkspaceMember.id != workspace_member_id,
            )
            .all()
        ]
        if member
        else []
    )
    for week_offset in range(weeks - 1, -1, -1):
        week_end = today - timedelta(weeks=week_offset)
        week_start = week_end - timedelta(days=6)
        user_result = calculate_productivity_score(db, workspace_member_id, week_start, week_end)
        teammate_scores = [
            s["score"]
            for s in (
                calculate_productivity_score(db, teammate_id, week_start, week_end)
                for teammate_id in teammate_ids
            )
            if s["score"] is not None
        ]
        points.append(
            ProductivityTrendPoint(
                week_label=week_start.strftime("%b %-d"),
                user_score=user_result["score"],
                team_average=round(sum(teammate_scores) / len(teammate_scores), 2)
                if teammate_scores
                else None,
            )
        )
    return ProductivityTrend(points=points, is_synthetic=True)


@router.get(
    "/dashboard/{workspace_member_id}",
    response_model=DashboardResponse,
)
def get_dashboard(
    workspace_member_id: UUID,
    db: Annotated[Session, Depends(get_db)],
    include_resolved: bool = False,
    period: PeriodOption = "8w",
):
    member = db.query(WorkspaceMember).filter(WorkspaceMember.id == workspace_member_id).first()

    if member is None:
        return DashboardResponse(
            workspace_member_id=workspace_member_id,
            insights=[],
            period=period,
        )

    weeks = _PERIOD_TO_WEEKS[period]
    period_end = date.today()
    period_start = period_end - timedelta(weeks=weeks)

    since = datetime.now(timezone.utc) - timedelta(days=14)

    query = db.query(AIInsight).filter(
        AIInsight.created_at >= since,
        (
            (AIInsight.workspace_member_id == workspace_member_id)
            | ((AIInsight.workspace_id == member.workspace_id) & (AIInsight.scope == "TEAM"))
        ),
    )

    # hide resolved insights by default so the list doesn't just grow and grow, same reasoning as the manager insights redesign
    if not include_resolved:
        query = query.filter(AIInsight.resolved.is_(False))

    rows = query.order_by(AIInsight.created_at.desc()).all()

    member_ids = {row.workspace_member_id for row in rows if row.workspace_member_id}
    project_ids = {row.project_id for row in rows if row.project_id}

    member_names: dict[UUID, str] = {}
    if member_ids:
        member_rows = (
            db.query(WorkspaceMember.id, User.first_name, User.last_name)
            .join(User, WorkspaceMember.user_id == User.id)
            .filter(WorkspaceMember.id.in_(member_ids))
            .all()
        )
        member_names = {wm_id: f"{first} {last}" for wm_id, first, last in member_rows}

    project_names: dict[UUID, str] = {}
    if project_ids:
        project_rows = db.query(Project.id, Project.name).filter(Project.id.in_(project_ids)).all()
        project_names = {p_id: name for p_id, name in project_rows}

    github_start = datetime.now(timezone.utc) - timedelta(days=7)
    github_end = datetime.now(timezone.utc)
    github = get_github_activity(db, workspace_member_id, github_start, github_end)

    return DashboardResponse(  # must match the db logic
        workspace_member_id=workspace_member_id,
        insights=[
            DashboardInsight(
                id=row.id,
                insight_type=row.insight_type,
                scope=row.scope,
                score=float(row.score) if row.score is not None else None,
                confidence=(float(row.confidence) if row.confidence is not None else None),
                description=row.description,
                recommendation=row.recommendation,
                narrative=row.narrative,
                project_id=row.project_id,
                project_name=project_names.get(row.project_id),
                workspace_member_id=row.workspace_member_id,
                member_name=member_names.get(row.workspace_member_id),
                workspace_id=row.workspace_id,
                created_at=row.created_at,
                resolved=row.resolved,
                resolved_at=row.resolved_at,
                resolved_by_workspace_member_id=row.resolved_by_workspace_member_id,
            )
            for row in rows
        ],
        github=github,
        period=period,
        productivity_trend=_get_productivity_trend(db, workspace_member_id, weeks),
        time_allocation=TimeAllocation(
            **compute_time_allocation(db, workspace_member_id, period_start, period_end)
        ),
        estimate_vs_actual=EstimateVsActual(
            **compute_estimate_vs_actual(db, workspace_member_id, period_start, period_end)
        ),
        score_cards=[
            ScoreCard(**card)
            for card in build_score_cards(db, workspace_member_id, period_start, period_end)
        ],
        time_split_by_task=TimeSplitByTask(
            **compute_time_split_by_task(db, workspace_member_id, period_start, period_end)
        ),
        task_switching_by_day=TaskSwitchingByDay(
            **compute_task_switching_by_day(db, workspace_member_id, period_start, period_end)
        ),
    )


@router.patch(
    "/{insight_id}/resolve",
    response_model=ResolveInsightResponse,
)
def resolve_insight(
    insight_id: UUID,
    payload: ResolveInsightRequest,
    db: Annotated[Session, Depends(get_db)],
):
    insight = db.query(AIInsight).filter(AIInsight.id == insight_id).first()

    if insight is None:
        raise HTTPException(status_code=404, detail="Insight not found")

    insight.resolved = True
    insight.resolved_at = datetime.now(timezone.utc)
    insight.resolved_by_workspace_member_id = payload.resolved_by_workspace_member_id

    db.commit()
    db.refresh(insight)

    return ResolveInsightResponse(
        id=insight.id,
        resolved=insight.resolved,
        resolved_at=insight.resolved_at,
        resolved_by_workspace_member_id=insight.resolved_by_workspace_member_id,
    )
