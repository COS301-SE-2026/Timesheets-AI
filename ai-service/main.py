"""
Entrypoint shim so `uvicorn main:app` keeps working.
The real app (all routers, CORS, scheduler lifespan) lives in app/main.py.
"""
from app.main import app  # noqa: F401