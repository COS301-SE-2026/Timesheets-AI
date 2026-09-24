"""
- this will be the ai generated explanation
- the explanation will be based on what I calculated, I don't want the ai making up values


Author: Nyasha
Date: 24/09/2026
"""

from pydantic import BaseModel


class ProjectForecastRecommendation(BaseModel):
    """
    - will be the recommendation for the manager
    """

    title: str
    description: str


class ProjectForecastExplanation(BaseModel):
    """
    - this will be the AI explanation of the project forecast
    - doing this so that the manager has a readable explanation of why the project has that current forecast and risk
    """

    summary: str
    risk_explanation: str
    contributing_factors: list[str]
    recommendations: list[ProjectForecastRecommendation]
