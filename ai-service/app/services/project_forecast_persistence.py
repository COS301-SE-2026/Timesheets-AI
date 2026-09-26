"""
- this will be for the forecasts stored in the DB
- only the latest forecast is what is stored, if a forecast already exists then it just gets replaced
- its a trade-off we accept so that UI loads quick and so that only necessary requests are made

Author: Nyasha
Date: 25/09/2026
"""

import json
import uuid
from datetime import datetime
from zoneinfo import ZoneInfo

from sqlalchemy.orm import Session

from app.models.ai_insight import AIInsight
from app.models.project import Project


# just making the values into format when needed so that they can be stored in DB
def _make_json_safe(forecast: dict) -> dict:
    return json.loads(
        json.dumps(
            forecast,
            default=str,
        )
    )


def save_project_forecast(
    db: Session,
    project_id: uuid.UUID,
    forecast: dict,
) -> AIInsight | None:
    """
    - saves the most recent forecast in DB
    - makes the project forecast row if it is the first sync
    """

    project = (
        db.query(Project)
        .filter(
            Project.id == project_id,
            Project.is_deleted.is_(False),
        )
        .first()
    )

    if project is None:
        return None

    # this checks if there is already another insight for project
    # so that if there are already existing syncs they just update the same record
    existing_insight = (
        db.query(AIInsight)
        .filter(
            AIInsight.project_id == project_id,
            AIInsight.scope == "PROJECT",
            AIInsight.insight_type == "PROJECT_FORECAST",
        )
        .first()
    )

    synced_at = datetime.now(ZoneInfo("Africa/Johannesburg")).replace(tzinfo=None)
    json_safe_forecast = _make_json_safe(forecast)

    if existing_insight is not None:
        existing_insight.workspace_id = project.workspace_id
        existing_insight.data = json_safe_forecast
        existing_insight.last_synced_at = synced_at

        db.commit()
        db.refresh(existing_insight)

        return existing_insight

    # stores this as a project insight instead of just having it for a certain workspace member
    insight = AIInsight(
        workspace_id=project.workspace_id,
        project_id=project_id,
        workspace_member_id=None,
        time_entry_id=None,
        insight_type="PROJECT_FORECAST",
        scope="PROJECT",
        score=None,
        confidence=None,
        description=None,
        recommendation=None,
        narrative=None,
        data=json_safe_forecast,
        last_synced_at=synced_at,
    )

    db.add(insight)
    db.commit()
    db.refresh(insight)

    return insight


def get_saved_project_forecast(
    db: Session,
    project_id: uuid.UUID,
) -> AIInsight | None:
    """
    - gets the saved project forecast from ai_insights
    - does not recalculate the forecast
    - does not call Gemini or external integrations
    - returns None when the project has never been synced
    """

    return (
        db.query(AIInsight)
        .filter(
            AIInsight.project_id == project_id,
            AIInsight.insight_type == "PROJECT_FORECAST",
            AIInsight.scope == "PROJECT",
        )
        .first()
    )
