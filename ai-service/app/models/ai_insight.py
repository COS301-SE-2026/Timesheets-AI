"""
This module handles the AI insights model for the AI service
It replicates the existing db ai_insights table, matching the momently_schema.sql
shared by Nyasha (Backend engineer).
Author: Zamokuhle Zwane
Date: 12/07/2026

Patch: added scop and narrative fields
Patch: added workspace id
"""

import uuid
from datetime import datetime

from sqlalchemy import DateTime, ForeignKey, Numeric, String, Text
from sqlalchemy.dialects.postgresql import UUID
from sqlalchemy.orm import Mapped, mapped_column

from app.database import Base


# During review, please double check the mapping with Nyasha's schema. I'll link it to the PR for reference.
class AIInsight(Base):
    __tablename__ = "ai_insights"

    id: Mapped[uuid.UUID] = mapped_column(UUID(as_uuid=True), primary_key=True, default=uuid.uuid4)
    workspace_member_id: Mapped[uuid.UUID] = mapped_column(
        UUID(as_uuid=True), ForeignKey("workspace_members.id"), nullable=True
    )
    workspace_id: Mapped[uuid.UUID] = mapped_column(
        UUID(as_uuid= True), ForeignKey("workspaces.id"), nullable=True
    )
    project_id: Mapped[uuid.UUID] = mapped_column(
        UUID(as_uuid=True), ForeignKey("projects.id"), nullable=True
    )
    time_entry_id: Mapped[uuid.UUID] = mapped_column(
        UUID(as_uuid=True), ForeignKey("time_entries.id"), nullable=True
    )
    insight_type: Mapped[str] = mapped_column(String(30))
    score: Mapped[float | None] = mapped_column(Numeric(5, 2), nullable=True)
    scope: Mapped[str] = mapped_column(String(10), default="USER")
    confidence: Mapped[float | None] = mapped_column(Numeric(5, 2), nullable=True)
    description: Mapped[str | None] = mapped_column(Text, nullable=True)
    recommendation: Mapped[str | None] = mapped_column(Text, nullable=True)
    narrative: Mapped[str | None] = mapped_column(Text, nullable=True)
    created_at: Mapped[datetime] = mapped_column(DateTime, default=datetime.utcnow)
