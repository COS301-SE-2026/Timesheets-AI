"""
- these are the schemas for the forecast scenarios
- I want a manager to be able to compare changes to the project without having to actually change data


Author: Nyasha
Date: 25/09/2026
"""

import uuid
from datetime import date

from pydantic import BaseModel


class ProjectForecastScenario(BaseModel):
    """
    - will be the calculated result of one project forecast scenario
    """

    scenario_type: str
    scenario_name: str

    velocity_multiplier: float
    scope_multiplier: float

    adjusted_weekly_velocity: float
    adjusted_remaining_hours: float

    forecast_total_hours: float
    forecast_end_date: date

    # compares this scenario with the current project trajectory
    # negative days means earlier completion, positive means later
    completion_date_difference_days: int = 0

    # negative hours means fewer forecast hours, positive means more
    forecast_hours_difference: float = 0.0


class ProjectForecastScenarioResponse(BaseModel):
    """
    - the full scenario comparison generated for a project
    - i am also adding a baseline so that managers have something to compare to
    """

    project_id: uuid.UUID
    project_name: str
    scenarios: list[ProjectForecastScenario]
