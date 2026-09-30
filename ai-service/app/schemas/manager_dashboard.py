"""
Request/response shapes for the manager dashboard endpoint. Mirrors manager_dashboard.py's dict shapes 1:1, same pattern as schemas/dashboard.py.

Author: Zamokuhle Zwane
Date: 25/09/2026
"""

from datetime import date
from typing import Literal, Optional
from uuid import UUID

from pydantic import BaseModel

StatusBand = Literal["HIGH_RISK", "AT_RISK"]


class WeeklyProgressPoint(BaseModel):
    week_label: str
    percent: float


class ProjectCompletionForecast(BaseModel):
    weekly_progress: list[WeeklyProgressPoint]
    current_progress_percent: float
    forecast_completion_date: Optional[date] = None


class TeamLoggedHoursMember(BaseModel):
    member_name: str
    hours: float


class TeamLoggedHours(BaseModel):
    members: list[TeamLoggedHoursMember]


class TaskOverview(BaseModel):
    total: int
    todo: int
    in_progress: int
    done: int
    blocked: int
    narrative: str


class VelocityWeek(BaseModel):
    week_label: str
    completed: int


class Velocity(BaseModel):
    weeks: list[VelocityWeek]
    rolling_average: float


class ProjectHealth(BaseModel):
    score: int
    status_label: str
    on_track_percent: float
    at_risk_percent: float
    behind_percent: float


class JiraTypeCount(BaseModel):
    issue_type: str
    count: int
    percentage: float


class JiraTicketBreakdown(BaseModel):
    total: int
    by_type: list[JiraTypeCount]


class FlaggedBurnoutMember(BaseModel):
    workspace_member_id: UUID
    risk_score: float
    status_band: StatusBand
    reason: Optional[str] = None
    insight_id: UUID


class GithubActivityByMember(BaseModel):
    workspace_member_id: UUID
    member_name: str
    hours_logged: float
    commit_count: int


class ManagedProject(BaseModel):
    project_id: UUID
    project_name: str


class ManagerDashboardResponse(BaseModel):
    project_completion_forecast: ProjectCompletionForecast
    team_logged_hours: TeamLoggedHours
    flagged_burnout_members: list[FlaggedBurnoutMember]
    task_overview: TaskOverview
    velocity: Velocity
    project_health: ProjectHealth
    github_activity_by_member: list[GithubActivityByMember]
    jira_ticket_breakdown: JiraTicketBreakdown
    managed_projects: list[ManagedProject]
    scoped_project_id: Optional[UUID] = None
    period: str
