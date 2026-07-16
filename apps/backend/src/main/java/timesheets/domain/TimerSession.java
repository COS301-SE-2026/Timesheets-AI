package timesheets.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// this is the JPA entity, it will show what a timer session looks like in the database
// a timer session tracks when a user starts and stops a timer
// when stopped, it automatically creates a DRAFT time entry

@Entity
@Table(name = "timer_sessions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TimerSession {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(name = "workspace_member_id", nullable = false)
  private UUID workspaceMemberId;

  @Column(name = "project_id", nullable = false)
  private UUID projectId;

  @Column(name = "task_id")
  private UUID taskId;

  @Column(name = "started_at", nullable = false)
  private LocalDateTime startedAt;

  @Column(name = "ended_at")
  private LocalDateTime endedAt;

  @Column(name = "paused_duration_seconds")
  private Long pausedDurationSeconds;

  @Column(name = "is_running", nullable = false)
  private Boolean isRunning;

  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @PrePersist
  protected void onCreate() {
    createdAt = LocalDateTime.now();
  }
}
