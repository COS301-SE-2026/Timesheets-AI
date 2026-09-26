"""
This file handles handles the request/response shapes for the dashboard schema
matches the db

Author: Zamokuhle Zwane
Date: 01/09/2026

Patched: added the GitHubActivity schema to handle the GitHub activity data for the dashboard
"""

from datetime import datetime
from typing import Literal, Optional
from uuid import UUID

from pydantic import BaseModel

StatusBand = Literal["HEALTHY", "MODERATE", "AT_RISK"]


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
    project_name: Optional[str] = None
    workspace_member_id: Optional[UUID] = None
    member_name: Optional[str] = None
    workspace_id: Optional[UUID] = None
    created_at: datetime
    resolved: bool = False
    resolved_at: Optional[datetime] = None
    resolved_by_workspace_member_id: Optional[UUID] = None


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
    alignment_score: Optional[float] = None
    explanation: Optional[str] = None


class ProductivityTrendPoint(BaseModel):
    week_label: str
    user_score: Optional[float] = None
    team_average: Optional[float] = None


class ProductivityTrend(BaseModel):
    points: list[ProductivityTrendPoint]
    is_synthetic: bool


class TimeAllocationCategory(BaseModel):
    category: str
    hours: float
    percentage: float


class TimeAllocation(BaseModel):
    total_hours: float
    categories: list[TimeAllocationCategory]


class EstimateVsActualCategory(BaseModel):
    category: str
    estimated_hours: float
    actual_hours: float


class EstimateVsActual(BaseModel):
    categories: list[EstimateVsActualCategory]


class ScoreCard(BaseModel):
    insight_type: str
    score: Optional[float] = None
    status: StatusBand
    delta_vs_previous: Optional[float] = None
    recommendation: str
    sparkline: list[float]


class TimeSplitByTaskItem(BaseModel):
    task_title: str
    hours: float
    percentage: float


class TimeSplitByTask(BaseModel):
    total_hours: float
    tasks: list[TimeSplitByTaskItem]


class TaskSwitchingByDayItem(BaseModel):
    day_label: str
    switches: int


class TaskSwitchingByDay(BaseModel):
    days: list[TaskSwitchingByDayItem]


class DashboardResponse(BaseModel):
    workspace_member_id: UUID
    insights: list[DashboardInsight]
    github: Optional[GitHubActivity] = None
    period: Optional[str] = None
    productivity_trend: Optional[ProductivityTrend] = None
    time_allocation: Optional[TimeAllocation] = None
    estimate_vs_actual: Optional[EstimateVsActual] = None
    score_cards: Optional[list[ScoreCard]] = None
    time_split_by_task: Optional[TimeSplitByTask] = None
    task_switching_by_day: Optional[TaskSwitchingByDay] = None


class ResolveInsightRequest(BaseModel):
    resolved_by_workspace_member_id: UUID


class ResolveInsightResponse(BaseModel):
    id: UUID
    resolved: bool
    resolved_at: Optional[datetime] = None
    resolved_by_workspace_member_id: Optional[UUID] = None

    model_config = {"from_attributes": True}
