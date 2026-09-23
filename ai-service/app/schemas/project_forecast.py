"""
- This combines the project budget, task progress, logged time and the team velocity
-  helps to estimate where the project is going

Author: Nyasha
Date: 22/09/2026
"""

import uuid
from datetime import date

from pydantic import BaseModel


class ProjectBudgetForecast(BaseModel):
    """
    - this has the hour-based budget info
    - the hours logged will be combined with the estimated work remaining
    - to help with calculating the expected hours for a project
    - it will help me see if the project might exceed the allocated hours
    """

    budget_hours: float | None
    used_hours: float
    remaining_budget_hours: float | None
    forecast_total_hours: float | None
    forecast_overrun_hours: float | None


class ProjectScheduleForecast(BaseModel):
    """
    - has the planned forecast and delivery dates for the project
    - the forecast will be calculated from the amount of estimated work and the remaining of teams velocity
    """

    start_date: date | None
    planned_end_date: date | None
    forecast_end_date: date | None
    delay_days: int | None
    timeline_progress_percentage: float | None


class ProjectTaskProgress(BaseModel):
    """
    - I'm going to use this to just summarise the estimated work
    """

    total_tasks: int
    completed_tasks: int
    remaining_tasks: int
    completion_percentage: float
    estimated_remaining_hours: float


class ProjectVelocity(BaseModel):
    """
    - this will be the teams working pace
    - the hours logged will be converted into a weekly rate
    - then the service should estimate how long a project tajes
    """

    recent_hours_per_week: float
    lookback_days: int


class ProjectRisk(BaseModel):
    """
    - I want to keep the individual risk areas separate and that will allow a manager to see why a project has that risk status
    """

    budget: str
    schedule: str
    task_progress: str
    overall: str


class ProjectForecastResponse(BaseModel):
    """
    - the response should be split into budget, schedule, task progress, velocity and risk sections
    """

    project_id: uuid.UUID
    project_name: str
    budget: ProjectBudgetForecast
    schedule: ProjectScheduleForecast
    tasks: ProjectTaskProgress
    velocity: ProjectVelocity
    risk: ProjectRisk

    model_config = {"from_attributes": True}
