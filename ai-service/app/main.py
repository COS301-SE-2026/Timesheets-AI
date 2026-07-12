"""
This handles the main application setup for the AI service, 
including FastAPI initialization, middleware configuration, and route inclusion.
Please check my draft file for links/resources on FastAPI features and usage.

Author: Zamokuhle Zwane
Date: 12/07/2026
"""

from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware

from app.api.health import router as health_router

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
)

app.include_router(health_router)

@app.get("/", include_in_schema=False)
def root():
    return {"message": "Momently AI Service, visit /docs for Swagger UI"}   