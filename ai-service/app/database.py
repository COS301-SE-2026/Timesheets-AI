"""
This handles the database connection and session management for the AI service.
In short, it sets up the SQLAlchemy engine, session factory, and provides a utility function to check the database connection.
please check my draft file for links/resources on SQLAlchemy features and usage.

Author: Zamokuhle Zwane
Date: 12/07/2026
"""

import logging

from sqlalchemy import create_engine, text
from sqlalchemy.orm import DeclarativeBase, sessionmaker

from app.config import settings

# In this file, I am setting up the db connection and session managing
# it will be used in the health endpoint
engine = create_engine(
    settings.database_url,
    pool_pre_ping=True,
    pool_size=5,
    max_overflow=10,
)

logger = logging.getLogger(__name__)
SessionLocal = sessionmaker(autocommit=False, autoflush=False, bind=engine)


class Base(DeclarativeBase):
    pass


def get_db():
    db = SessionLocal()
    try:
        yield db
    finally:
        db.close()


def check_connection():
    try:
        with engine.connect() as connection:
            connection.execute(text("SELECT 1"))
        return True
    except Exception:
        logger.exception("Database connection error: %s", e)
        return False
