"""
This file wires the nightly and weekly insight jobs into APScheduler, ties to FastAPI's lifespan
I decided for nightly job at 02:00AM i figured this makes sense because not a lot of people using momently will be active at that time
and weekly jobs on fridays at 07:00AM for a summary of the week at the end of the week, duhhh.

Reference for apscheduler: https://apscheduler.readthedocs.io/en/3.x/
Author: Zamokuhle Zwane
Date: 31/08/2026
"""

import logging
from datetime import date, timedelta

from apscheduler.schedulers.background import BackgroundScheduler
from apscheduler.triggers.cron import CronTrigger

from app.database import SessionLocal
from app.jobs.nightly_insights_job import run_nightly_insights_job, run_weekly_summary_job

logger = logging.getLogger(__name__)

scheduler = BackgroundScheduler()


def _run_nightly_job() -> None:
    db = SessionLocal()
    try:
        period_end = date.today() - timedelta(days=1)
        period_start = period_end
        logger.info("Starting nightly insights job for %s", period_start)
        run_nightly_insights_job(db, period_start, period_end)
        logger.info("Nightly insights job completed")
    except Exception:
        logger.exception("Nightly insights job failed")
    finally:
        db.close()


def _run_weekly_job() -> None:
    db = SessionLocal()
    try:
        today = date.today()
        week_start = today - timedelta(days=today.weekday())
        logger.info("Starting weekly summary job for week of %s", week_start)
        run_weekly_summary_job(db, week_start)
        logger.info("Weekly summary job completed")
    except Exception:
        logger.exception("Weekly summary job failed")
    finally:
        db.close()


def start_scheduler() -> None:
    scheduler.add_job(
        _run_nightly_job,
        trigger=CronTrigger(hour=2, minute=0),
        id="nightly_insights_job",
        replace_existing=True,
    )
    scheduler.add_job(
        _run_weekly_job,
        trigger=CronTrigger(day_of_week="fri", hour=7, minute=0),
        id="weekly_summary_job",
        replace_existing=True,
    )
    scheduler.start()
    logger.info("Scheduler started, nightly job at 02:00, weekly job Fridays at 07:00")


def stop_scheduler() -> None:
    scheduler.shutdown(wait=False)
    logger.info("Scheduler shut down")
