"""
Dev-only endpoint to run the nightly/weekly insight jobs on demand, instead of waiting for the 02:00 / Friday 07:00 cron. Not for production use.
just for curl/swagger endpoints

Author: Zamokuhle Zwane
Date: 19/09/2026
"""

from datetime import date, timedelta
from typing import Annotated
from uuid import UUID

from fastapi import APIRouter, Depends
from sqlalchemy.orm import Session

from app.database import get_db
from app.jobs.nightly_insights_job import (
    run_nightly_insights_job,
    run_weekly_summary_job,
)
from app.models.workspace_member import WorkspaceMember

router = APIRouter(prefix="/dev", tags=["Dev"])


@router.post("/trigger-insights/{workspace_member_id}")
def trigger_insights(workspace_member_id: UUID, db: Annotated[Session, Depends(get_db)]):
    member = db.query(WorkspaceMember).filter(WorkspaceMember.id == workspace_member_id).first()
    if member is None:
        return {"status": "error", "message": "workspace member not found"}

    today = date.today()
    week_start = today - timedelta(days=today.weekday())
    run_nightly_insights_job(db, week_start, today)
    run_weekly_summary_job(db, week_start)

    return {"status": "ok", "message": f"insights generated for {workspace_member_id}"}
