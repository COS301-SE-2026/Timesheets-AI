package timesheets.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import timesheets.evidence.EvidenceEvent;

@Entity
@Table(name = "suggested_work_sessions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SuggestedWorkSessionEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(name = "workspace_member_id", nullable = false)
  private UUID workspaceMemberId;

  @Column(name = "start_time", nullable = false)
  private LocalDateTime startTime;

  @Column(name = "end_time", nullable = false)
  private LocalDateTime endTime;

  @Column(name = "title")
  private String title;

  @Column(name = "project_id")
  private UUID projectId;

  @Column(name = "task_id")
  private UUID taskId;

  // this tells Hibernate to store this java object as JSON
  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "evidence_events", columnDefinition = "jsonb")
  private List<EvidenceEvent> evidenceEvents;

  @Column(name = "confidence_score")
  private double confidenceScore;

  @Column(name = "duration_minutes")
  private Integer durationMinutes;

  @Column(name = "duration_seconds")
  private Integer durationSeconds;

  @Column(name = "entry_type")
  private String entryType;

  @Column(name = "description")
  private String description;

  @Column(name = "explanation")
  private String explanation;

  @Column(name = "status")
  private String status;

  @Column(name = "created_at", nullable = false)
  private LocalDateTime createdAt;

  @Column(name = "updated_at")
  private LocalDateTime updatedAt;

  @PrePersist
  protected void onCreate() {
    createdAt = LocalDateTime.now();
    updatedAt = LocalDateTime.now();
  }

  @PreUpdate
  protected void onUpdate() {
    updatedAt = LocalDateTime.now();
  }
}
