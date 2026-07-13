"""
This handles the health check endpoint for the API.
It uses FastAPI's TestClient to simulate requests to the API and verify the responses.
Please check my draft file for links/resources on FastAPI testing features and usage.

Author: Zamokuhle Zwane
Date: 12/07/2026
"""

from unittest.mock import patch

from fastapi.testclient import TestClient

from app.main import app

client = TestClient(app)


def test_health_db_up():
    with patch("app.api.health.check_connection", return_value=True):
        response = client.get("/health")
        assert response.status_code == 200
        assert response.json() == {
            "status": "ok",
            "Service": "momently-ai",
            "database": "connected",
        }


def test_health_db_down():
    with patch("app.api.health.check_connection", return_value=False):
        response = client.get("/health")
        assert response.status_code == 200
        assert response.json() == {
            "status": "degraded",
            "Service": "momently-ai",
            "database": "disconnected",
        }
