"""
Central model registry.

SQLAlchemy resolves string-based ForeignKey references (e.g. ForeignKey("workspaces.id"))
against Base.metadata at mapper-configuration time. A table only ends up in that metadata
if its model module has actually been imported somewhere in the running process.

AIInsight has FKs to workspace_members, workspaces, projects, and time_entries — but several
of those models were previously only imported locally inside specific job functions, so any
code path that creates/saves an AIInsight without going through those functions first (e.g.
calling the weekly-summary endpoint directly) would fail with:

    sqlalchemy.exc.NoReferencedTableError: Foreign key associated with column
    'ai_insights.workspace_id' could not find table 'workspaces' ...

Importing every model here, and importing this package once at startup (see app/database.py
or app/main.py), guarantees the full metadata is registered before any table is touched.
"""

from app.models.ai_insight import AIInsight
from app.models.project import Project
from app.models.task import Task
from app.models.time_entry import TimeEntry
from app.models.user import User
from app.models.workspace import Workspace
from app.models.workspace_member import WorkspaceMember

__all__ = [
    "AIInsight",
    "Project",
    "Task",
    "TimeEntry",
    "User",
    "Workspace",
    "WorkspaceMember",
]
