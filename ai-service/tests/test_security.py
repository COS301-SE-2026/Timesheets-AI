"""
Tests for the shared-secret auth on the AI service routes.

Uses the manager assistant narrative route as the probe: with a valid key the request gets
past auth and fails body validation (422), so no database or Gemini call is needed.
"""

import pytest
from fastapi.testclient import TestClient

from app.config import settings
from app.main import app

PROBE = "/insights/manager-assistant/narrative"

client = TestClient(app)


@pytest.fixture
def api_key(monkeypatch):
    monkeypatch.setattr(settings, "ai_service_api_key", "test-key")
    return "test-key"


def should_reject_request_when_api_key_header_is_missing(api_key):
    response = client.post(PROBE, json={})

    assert response.status_code == 401


def should_reject_request_when_api_key_is_wrong(api_key):
    response = client.post(PROBE, json={}, headers={"X-Internal-Api-Key": "nope"})

    assert response.status_code == 401


def should_let_request_through_auth_when_api_key_is_correct(api_key):
    response = client.post(PROBE, json={}, headers={"X-Internal-Api-Key": api_key})

    # past auth, so it only fails because the empty body is not a valid review
    assert response.status_code == 422


def should_keep_health_open_without_an_api_key(api_key):
    response = client.get("/health")

    assert response.status_code == 200


def should_disable_protected_routes_when_key_unset_outside_development(monkeypatch):
    monkeypatch.setattr(settings, "ai_service_api_key", "")
    monkeypatch.setattr(settings, "app_env", "production")

    response = client.post(PROBE, json={})

    assert response.status_code == 503


def should_allow_protected_routes_when_key_unset_in_development(monkeypatch):
    monkeypatch.setattr(settings, "ai_service_api_key", "")
    monkeypatch.setattr(settings, "app_env", "development")

    response = client.post(PROBE, json={})

    assert response.status_code == 422
