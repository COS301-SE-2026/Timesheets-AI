"""
This handles the health check endpoint for the API

Author: Zamokuhle Zwane
Date: 12/07/2026
"""

from fastapi import APIRouter

from app.database import check_connection

router = APIRouter()

@router.get("/health", tags=["Health"])
def health():
    db_ok = check_connection()
    return {"status": "ok" if db_ok else "degraded",
            "Service": "momently-ai",
            "database": "connected" if db_ok else "disconnected"
            }