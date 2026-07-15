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


def should_return_ok_When_endpoint_is_called():
    # Arrange
    # here client is set up at module level

    # Act
    response = client.get("/health")

    # Assert
    assert response.status_code == 200
    assert response.json()["status"] == "ok"


def should_return_healthy_status_when_database_is_up():
    # Arrange
    # i set up the client at module level

    # Act
    with patch("app.api.health.check_connection", return_value=True):
        response = client.get("/health")

    # Assert
    assert response.status_code == 200
    assert response.json()["database"] == "connected"


def should_return_unhealthy_status_when_database_is_down():
    # Arrange
    # i set up the client at module level

    # Act
    with patch("app.api.health.check_connection", return_value=False):
        response = client.get("/health")

    # Assert
    # the health endpoint stays up even when the db is down, it just reports the fact.
    assert response.status_code == 200
    assert response.json()["database"] == "disconnected"
