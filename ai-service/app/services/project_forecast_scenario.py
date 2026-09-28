"""
- to calculate alt scenarios
- the scenarios will not change the project data
- they just add assumptions and scope so managers can compare scenarios

Author: Nyasha
Date: 25/09/2026
"""

import uuid
from datetime import date, timedelta

from app.schemas.project_forecast_scenario import (
    ProjectForecastScenario,
    ProjectForecastScenarioResponse,
)


def calculate_project_forecast_scenarios(
    project_id: uuid.UUID,
    project_name: str,
    used_hours: float,
    remaining_hours: float,
    weekly_velocity: float,
) -> ProjectForecastScenarioResponse:
    """
    - calculates many scenarios for the forecast
    - going to use the current trajactory as a baseline
    - the scenarios should only modify something related to that specific scenario
    """

    # these are the scenarios that I want to change, I might add more as time goes
    scenarios = [
        _calculate_scenario(
            scenario_type="CURRENT_TRAJECTORY",
            scenario_name="Current trajectory",
            used_hours=used_hours,
            remaining_hours=remaining_hours,
            weekly_velocity=weekly_velocity,
            velocity_multiplier=1.0,
            scope_multiplier=1.0,
        ),
        _calculate_scenario(
            scenario_type="INCREASED_VELOCITY",
            scenario_name="20% increased velocity",
            used_hours=used_hours,
            remaining_hours=remaining_hours,
            weekly_velocity=weekly_velocity,
            velocity_multiplier=1.20,
            scope_multiplier=1.0,
        ),
        _calculate_scenario(
            scenario_type="DECREASED_VELOCITY",
            scenario_name="20% decreased velocity",
            used_hours=used_hours,
            remaining_hours=remaining_hours,
            weekly_velocity=weekly_velocity,
            velocity_multiplier=0.80,
            scope_multiplier=1.0,
        ),
        _calculate_scenario(
            scenario_type="REDUCED_SCOPE",
            scenario_name="20% reduced remaining scope",
            used_hours=used_hours,
            remaining_hours=remaining_hours,
            weekly_velocity=weekly_velocity,
            velocity_multiplier=1.0,
            scope_multiplier=0.80,
        ),
        _calculate_scenario(
            scenario_type="ADDITIONAL_CAPACITY",
            scenario_name="25% additional capacity",
            used_hours=used_hours,
            remaining_hours=remaining_hours,
            weekly_velocity=weekly_velocity,
            velocity_multiplier=1.25,
            scope_multiplier=1.0,
        ),
    ]
    """
    assumptions I made (they are not evidence based)
        1. the current trajectory is of the projects weekly velocity and remaining work - there is no adjustment
        2. I chose 20%  increase velocity cause its big enough to make a meaningful change but not over assumption
            - it does not mean that the velocity actually increases by 20%
        3. chose 20% decrease velocity, to mirror the increase so there can be a change in both directions
        4. 20% scope reducing could be if the manager can give work to someone else
            - kept it at 20% to keep the changes consistent
            - 20% still being a reasonable enough number such that it makes a big enough change without over assumption
        5. the additional-capacity could be if there is an increase in the weekly velocity
            - could be through people working more
            - adding another member? did not want to limit the scope
            - i chose 25% to make it a little more like an actual intervention change instead of just velocity changing normally
            - the assumption is a what-if and does not account for onboarding or individual productivity
        6. the scenarios are more of what-if and do not claim that these changes will actually occur
    """

    baseline = scenarios[0]

    for scenario in scenarios:
        scenario.completion_date_difference_days = (
            scenario.forecast_end_date - baseline.forecast_end_date
        ).days

        scenario.forecast_hours_difference = round(
            scenario.forecast_total_hours - baseline.forecast_total_hours,
            2,
        )

    return ProjectForecastScenarioResponse(
        project_id=project_id,
        project_name=project_name,
        scenarios=scenarios,
    )


def _calculate_scenario(
    scenario_type: str,
    scenario_name: str,
    used_hours: float,
    remaining_hours: float,
    weekly_velocity: float,
    velocity_multiplier: float,
    scope_multiplier: float,
) -> ProjectForecastScenario:
    adjusted_weekly_velocity = weekly_velocity * velocity_multiplier
    adjusted_remaining_hours = remaining_hours * scope_multiplier

    forecast_total_hours = used_hours + adjusted_remaining_hours

    forecast_end_date = _calculate_scenario_end_date(
        adjusted_remaining_hours,
        adjusted_weekly_velocity,
    )

    return ProjectForecastScenario(
        scenario_type=scenario_type,
        scenario_name=scenario_name,
        velocity_multiplier=velocity_multiplier,
        scope_multiplier=scope_multiplier,
        adjusted_weekly_velocity=round(adjusted_weekly_velocity, 2),
        adjusted_remaining_hours=round(adjusted_remaining_hours, 2),
        forecast_total_hours=round(forecast_total_hours, 2),
        forecast_end_date=forecast_end_date,
    )


def _calculate_scenario_end_date(
    remaining_hours: float,
    weekly_velocity: float,
) -> date:
    """
    - this will calculate the expected completion data for a scenario
    - it calculates it using the remaining hours / weekly velocity
    """

    if remaining_hours <= 0:
        return date.today()

    if weekly_velocity <= 0:
        return date.today()

    weeks_remaining = remaining_hours / weekly_velocity
    days_remaining = weeks_remaining * 7

    return date.today() + timedelta(days=round(days_remaining))
