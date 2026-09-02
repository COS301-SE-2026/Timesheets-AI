"""
This file handles handles the request/response shapes for the dashboard schema
matches the db

Author: Zamokuhle Zwane
Date: 01/09/2026

Patched: added the GitHubActivity schema to handle the GitHub activity data for the dashboard
"""

from datetime import datetime
from typing import Optional
from uuid import UUID

from pydantic import BaseModel


class DashboardInsight(BaseModel):
    id: UUID
    insight_type: str
    scope: str
    score: Optional[float] = None
    confidence: Optional[float] = None
    description: Optional[str] = None
    recommendation: Optional[str] = None
    narrative: Optional[str] = None
    project_id: Optional[UUID] = None
    workspace_member_id: Optional[UUID] = None
    workspace_id: Optional[UUID] = None
    created_at: datetime

class GitHubActivity(BaseModel):
    connected: bool
    hours_logged: float
    commit_count: int
    commits_per_hour: float
    additions: int
    deletions: int
    active_repositories: int
    active_days: int
    alignment: Optional[str] = None
    explanation: Optional[str] = None

class DashboardResponse(BaseModel):
    workspace_member_id: UUID
    insights: list[DashboardInsight]
    github: Optional[GitHubActivity] = None
