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
    

    
    //these are the getters and setters
    public UUID getId() {
        return id;
    }
    
    public void setId(UUID id) {
        this.id = id;
    }
    
    public UUID getWorkspaceMemberId() {
        return workspaceMemberId;
    }
    
    public void setWorkspaceMemberId(UUID workspaceMemberId) {
        this.workspaceMemberId = workspaceMemberId;
    }
    
    public UUID getProjectId() {
        return projectId;
    }
    
    public void setProjectId(UUID projectId) {
        this.projectId = projectId;
    }
    
    public UUID getTaskId() {
        return taskId;
    }
    
    public void setTaskId(UUID taskId) {
        this.taskId = taskId;
    }
    
    public LocalDateTime getStartedAt() {
        return startedAt;
    }
    
    public void setStartedAt(LocalDateTime startedAt) {
        this.startedAt = startedAt;
    }
    
    public LocalDateTime getEndedAt() {
        return endedAt;
    }
    
    public void setEndedAt(LocalDateTime endedAt) {
        this.endedAt = endedAt;
    }
    
    public Long getPausedDurationSeconds() {
        return pausedDurationSeconds;
    }
    
    public void setPausedDurationSeconds(Long pausedDurationSeconds) {
        this.pausedDurationSeconds = pausedDurationSeconds;
    }
    
    public String getSource() {
        return source;
    }
    
    public void setSource(String source) {
        this.source = source;
    }
    
    public Boolean getIsRunning() {
        return isRunning;
    }
    
    public void setIsRunning(Boolean isRunning) {
        this.isRunning = isRunning;
    }
    
    public String getNotes() {
        return notes;
    }
    
    public void setNotes(String notes) {
        this.notes = notes;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}