package timesheets.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "timer_sessions")
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
    
    @Column(name = "source")
    private String source;
    
    @Column(name = "is_running", nullable = false)
    private Boolean isRunning;
    
    @Column(name = "notes")
    private String notes;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}