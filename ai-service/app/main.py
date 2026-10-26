"""
Momently AI Service
FastAPI application Demo 4 scaffold

Endpoints:
  GET  /health         liveness probe
  GET  /docs           Swagger UI (development only)
  POST /insights/productivity/...       productivity score
  POST /insights/anomaly/...            anomaly detection
  POST /insights/burnout/...            burnout risk
  POST /insights/delivery-forecast/...  delivery forecast
  POST /insights/task-switching/...     task switching
  POST /insights/weekly-summary/...     weekly summary (Gemini)

  Every route except /health needs the X-Internal-Api-Key header, see app/security.py

  Patched: 25 August 2026
  added router imports
"""

from contextlib import asynccontextmanager

from fastapi import Depends, FastAPI
from fastapi.middleware.cors import CORSMiddleware

from app.api import project_forecast_evidence
from app.api.anomaly import router as anomaly_router
from app.api.burnout import router as burnout_router
from app.api.dashboard import router as dashboard_router
from app.api.delivery_forecast import router as delivery_forecast_router
from app.api.dev_trigger import router as dev_trigger_router
from app.api.health import router as health_router
from app.api.manager_assistant import router as manager_assistant_router
from app.api.manager_dashboard import router as manager_dashboard_router
from app.api.productivity import router as productivity_router
from app.api.project_forecast import router as project_forecast_router
from app.api.task_switching import router as task_switching_router
from app.api.weekly_summary import router as weekly_summary_router
from app.config import settings
from app.scheduler import start_scheduler, stop_scheduler
from app.security import require_internal_api_key, warn_if_unprotected


@asynccontextmanager
async def lifespan(app: FastAPI):
    warn_if_unprotected()
    start_scheduler()
    yield
    stop_scheduler()


# interactive docs list every route and let anyone try them, so they only exist in development
app = FastAPI(
    title="Momently AI Service",
    description="Productivity insights and anomaly detection for Momently",
    version="0.1.0",
    lifespan=lifespan,
    docs_url="/docs" if settings.is_development else None,
    redoc_url=None,
    openapi_url="/openapi.json" if settings.is_development else None,
)

# Server to server only (Spring backend -> here), so there is nothing for a browser origin to do.
# Origins are empty unless CORS_ALLOWED_ORIGINS is set, and credentials are never allowed.
app.add_middleware(
    CORSMiddleware,
    allow_origins=settings.cors_origins,
    allow_credentials=False,
    allow_methods=["GET", "POST", "PATCH"],
    allow_headers=["Authorization", "Content-Type", "X-Internal-Api-Key"],
)

# /health stays open (docker healthcheck + uptime monitors), everything else needs the shared key
protected = [Depends(require_internal_api_key)]

app.include_router(health_router)
app.include_router(productivity_router, dependencies=protected)
app.include_router(anomaly_router, dependencies=protected)
app.include_router(burnout_router, dependencies=protected)
app.include_router(delivery_forecast_router, dependencies=protected)
app.include_router(task_switching_router, dependencies=protected)
app.include_router(weekly_summary_router, dependencies=protected)
app.include_router(dashboard_router, dependencies=protected)
app.include_router(manager_assistant_router, dependencies=protected)
app.include_router(manager_dashboard_router, dependencies=protected)
app.include_router(project_forecast_evidence.router, dependencies=protected)
app.include_router(project_forecast_router, dependencies=protected)

# on-demand job trigger is a dev convenience, it is not even mounted outside development
if settings.is_development:
    app.include_router(dev_trigger_router, dependencies=protected)


@app.get("/health", tags=["Health"])
def health():
    return {"status": "ok", "service": "momently-ai"}


@app.get("/", include_in_schema=False)
def root():
    return {"message": "Momently AI Service"}
