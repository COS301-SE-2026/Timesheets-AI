"""
Momently AI Service
FastAPI application Demo 1 scaffold

Endpoints:
  GET  /health         liveness probe
  GET  /docs           Swagger UI
  POST /insights       plaeturns mock aggregation

  Patched: 25 August 2026
  added router imports
"""

from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware

from app.api.anomaly import router as anomaly_router
from app.api.burnout import router as burnout_router
from app.api.delivery_forecast import router as delivery_forecast_router
from app.api.weekly_summary import router as weekly_summary_router

app = FastAPI(
    title="Momently AI Service",
    description="Productivity insights and anomaly detection for Momently",
    version="0.1.0",
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["http://localhost:4200", "http://localhost:8080"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
    # will add the domain here!
)
app.include_router(burnout_router)
app.include_router(delivery_forecast_router)
app.include_router(anomaly_router)
app.include_router(weekly_summary_router)


@app.get("/health", tags=["Health"])
def health():
    return {"status": "ok", "service": "momently-ai"}


@app.get("/", include_in_schema=False)
def root():
    return {"message": "Momently AI Service, visit /docs for Swagger UI"}


# Placeholder insight endpoint(Not needed for demo 1, but will be used in later milestones when we integrate ML models)
@app.post("/insights", tags=["Insights"])
def get_insights(payload: dict):
    """
    Demo 1 placeholder.
    In later milestones this will call the ML models.
    """
    return {
        "status": "ok",
        "note": "ML models not yet integrated, Demo 1 returns stub data",
        "received": payload,
    }
