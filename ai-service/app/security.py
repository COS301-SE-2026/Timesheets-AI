"""
Shared-secret auth for the AI service.

The AI service is an internal service: only the Spring backend (and the scheduler running
inside this process) should ever call it. The browser never talks to it directly, so every
route except /health requires the X-Internal-Api-Key header, which the backend sends on
every call (see AiServiceClient and ManagerAssistantNarrativeClient).

Behaviour:
- key configured: the header must match, compared in constant time
- key not configured and APP_ENV=development: allowed, with a warning logged once at startup, so local dev keeps working
- key not configured anywhere else: every protected route returns 503 instead of silently running open

Author: Zamokuhle Zwane
"""

import logging
import secrets
from typing import Annotated

from fastapi import Header, HTTPException, status

from app.config import settings

logger = logging.getLogger(__name__)

API_KEY_HEADER = "X-Internal-Api-Key"


def warn_if_unprotected() -> None:
    if not settings.ai_service_api_key:
        logger.warning(
            "AI_SERVICE_API_KEY is not set, AI service routes are %s",
            "open (development only)" if settings.is_development else "DISABLED until it is set",
        )


def require_internal_api_key(
    x_internal_api_key: Annotated[str | None, Header(alias=API_KEY_HEADER)] = None,
) -> None:
    expected = settings.ai_service_api_key

    if not expected:
        if settings.is_development:
            return
        raise HTTPException(
            status_code=status.HTTP_503_SERVICE_UNAVAILABLE,
            detail="Service is not configured.",
        )

    provided = x_internal_api_key or ""
    if not secrets.compare_digest(provided.encode(), expected.encode()):
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Invalid or missing API key.",
        )
