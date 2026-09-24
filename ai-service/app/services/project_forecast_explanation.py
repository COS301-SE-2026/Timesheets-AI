"""
- this is what makes the AI explanation for the project forecast
- gemini is used to explain the forecast result
- it does not calculate or change the project calculations I have made

Author: Nyasha
Date: 25/09/2026
"""

import json
import logging
import time

import google.generativeai as genai

from app.config import settings
from app.schemas.project_forecast_explanation import ProjectForecastExplanation

# I am going to be using it to log if I have error with Gemini
logger = logging.getLogger(__name__)

# using Zamo's strategy of the retries and the backoff
MAX_RETRIES = 3
RETRY_BACKOFF_SECONDS = 1


def generate_project_forecast_explanation(
    forecast: dict,
) -> ProjectForecastExplanation | None:
    """
    - this is what gives the manager the explanation of the calculated forecast
    - if gemini does not give a valid explanation then None is returned so that the forcast can still happen
    """

    # I want to log majority of what I can so that I can quickly diagnose problems when I need to
    if not settings.gemini_api_key:
        logger.warning("Gemini API key is not configured, project forecast explanation skipped")
        return None

    prompt = _build_project_forecast_prompt(forecast)

    try:
        response_text = _call_gemini_with_retry(prompt)
        response_data = json.loads(response_text)

        return ProjectForecastExplanation.model_validate(response_data)

    except (RuntimeError, ValueError, TypeError, json.JSONDecodeError):
        logger.exception(
            "Failed to generate AI explanation for project forecast %s",
            forecast.get("project_id"),
        )
        return None


def _build_project_forecast_prompt(forecast: dict) -> str:
    """
    - I am building the prompt used to explain the forecast
    - the recommendations must be based only on the evidence contained in the forecast
    - will keep adding and refining the prompt as I go, if I see that there is more to be added, will also see more recommendations since I have to be specific
    """

    # making sure that the forecast is in JSON, also I want dates and the UUIDs to be strings when they should be
    forecast_json = json.dumps(
        forecast,
        default=str,
        indent=2,
    )

    """
    Rules I want to implement:
        - GitHub and Jira are external evidence
        - the missing evidence and the forecast confidence has to be recognised not just give random assumptions
        - I only want JSON output, easier since its the format I require
    """
    return f"""
    You are explaining a deterministic software project forecast to a project manager.

    The forecast has already been calculated by the Momently forecasting service.
    You are NOT responsible for calculating, correcting, replacing, or predicting any forecast values.

    STRICT RULES:
    1. Use only information contained in the supplied forecast.

    2. Do not invent:
    - hours
    - dates
    - percentages
    - task counts
    - GitHub activity
    - Jira activity
    
    3. Never change or recalculate any deterministic forecast value.

    4. Do not claim that one metric caused another metric.
    You may explain that multiple indicators contribute to the calculated risk assessment, but do not invent causal relationships.

    5. Treat GitHub and Jira data as SUPPORTING EVIDENCE only.
    External evidence must not replace:
    - logged time
    - task estimates
    - project budget


    FORECAST:

    {forecast_json}


    Return exactly this structure:

    {{
        "summary": "A concise explanation of the project's current forecast.",
        "risk_explanation": "Explain why the supplied deterministic risk status was produced using the available evidence.",
        "contributing_factors": [
            "A specific factor supported by the supplied forecast"
        ],
        "recommendations": [
            {{
                "title": "Short recommendation title",
                "description": "A practical manager action supported by the supplied evidence"
            }}
        ]
    }}

    For contributing_factors:
    - include only factors supported by the forecast
    - prioritize the factors most relevant to risk and confidence

    For recommendations:
    - provide between 1 and 3 recommendations
    - do not recommend changing a deterministic forecast value
    """.strip()


def _call_gemini_with_retry(prompt: str) -> str:
    """
    - using Zamo's strategy of calling Gemini
    - will not accept empty responses
    - when all the retries are done, then show an error
    """

    model = genai.GenerativeModel("gemini-3.5-flash-lite")
    genai.configure(api_key=settings.gemini_api_key)

    last_error = None

    for attempt in range(MAX_RETRIES):
        try:
            response = model.generate_content(prompt)

            # if I get an empty response I'm considering it an error
            if not response.text:
                raise ValueError("Gemini returned an empty response")

            return response.text.strip()

        # catching the errors so that there can be a retyr
        except Exception as e:
            last_error = e
            time.sleep(RETRY_BACKOFF_SECONDS * (2**attempt))

    # I still want the forecast to continue even if AI fails because I can still give valid info
    raise RuntimeError(f"Gemini call failed after {MAX_RETRIES} attempts: {last_error}")
