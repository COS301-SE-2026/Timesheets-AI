"""
This file handles the router for the ai dashboard endpoint

Author: Zamokuhle Zwane
Date: 01/09/2026
"""

from datetime import datetime, timedelta, timezone
from uuid import UUID

from fastapi import APIRouter, Depends
from sqlalchemy.orm import Session

from app.database import get_db
from app.models.ai_insight import AIInsight
from app.models.workspace_member import WorkspaceMember
from app.schemas.dashboard import DashboardInsight, DashboardResponse
from app.services.github_activity import get_github_activity

router = APIRouter(
    prefix="/insights",
    tags=["Insights"],
)


@router.get(
    "/dashboard/{workspace_member_id}",
    response_model=DashboardResponse,
)
def get_dashboard(
    workspace_member_id: UUID,
    db: Session = Depends(get_db),
):
    member = db.query(WorkspaceMember).filter(WorkspaceMember.id == workspace_member_id).first()

    if member is None:
        return DashboardResponse(
            workspace_member_id=workspace_member_id,
            insights=[],
        )

    since = datetime.now(timezone.utc) - timedelta(days=14)

    rows = (
        db.query(AIInsight)
        .filter(
            AIInsight.created_at >= since,
            (
                (AIInsight.workspace_member_id == workspace_member_id)
                | ((AIInsight.workspace_id == member.workspace_id) & (AIInsight.scope == "TEAM"))
            ),
        )
        .order_by(AIInsight.created_at.desc())
        .all()
    )

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
                workspace_member_id=row.workspace_member_id,
                workspace_id=row.workspace_id,
                created_at=row.created_at,
            )
            for row in rows
        ],
        github=github,
    )
