package timesheets.domain;

import timesheets.enums.TimeEntryStatus;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

//so this is the JPA entity- you can kinda think of it like the data template
// it's basically showing us what the time entry looks like in the database
// a java object is what the row looks like in the database

@Entity //this tells Java that the class maps to a DB
@Table(name = "time_entries") //what the table will be called in the database, otherwise TimeEntry would be used

public class TimeEntry {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(name = "workspace_member_id", nullable = false)
    private UUID workspaceMemberId;
    
    @Column(name = "project_id", nullable = false)
    private UUID projectId;
    
    @Column(name = "task_id")
    private UUID taskId;
    
    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;
    
    @Column(name = "end_time")
    private LocalDateTime endTime;
    
    @Column(name = "duration_minutes")
    private Integer durationMinutes;
    
    @Column(name = "entry_type")
    private String entryType;
    
    private String description;
    
    @Column(name = "is_locked")
    private Boolean isLocked = false;
    
    @Column(name = "locked_at")
    private LocalDateTime lockedAt;
    
    @Column(name = "edited_at")
    private LocalDateTime editedAt;
    
    @Column(name = "edited_by_workspace_member_id")
    private UUID editedByWorkspaceMemberId;
    
    @Enumerated(EnumType.STRING)
    private TimeEntryStatus status = TimeEntryStatus.DRAFT;
    
    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;
    
    @Column(name = "reviewed_by_workspace_member_id")
    private UUID reviewedByWorkspaceMemberId;
    
    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;
    
    @Column(name = "rejection_reason")
    private String rejectionReason;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @PrePersist //this makes the method run before the entity is saved
    protected void onCreate() {
        createdAt = LocalDateTime.now(); //sets the timestamp before the insert
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate //runs before the entity is updated
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    
    //the getters and setters 
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
    


    public LocalDateTime getStartTime() { 
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) { 
        this.startTime = startTime; 
    }
    


    public LocalDateTime getEndTime() { 
        return endTime; 
    }

    public void setEndTime(LocalDateTime endTime) { 
        this.endTime = endTime; 
    }
    


    public Integer getDurationMinutes() { 
        return durationMinutes; 
    }

    public void setDurationMinutes(Integer durationMinutes) { 
        this.durationMinutes = durationMinutes; 
    }
    


    public String getEntryType() { 
        return entryType; 
    }

    public void setEntryType(String entryType) { 
        this.entryType = entryType; 
    }
    


    public String getDescription() { 
        return description; 
    }

    public void setDescription(String description) { 
        this.description = description; 
    }
    


    public Boolean getIsLocked() { 
        return isLocked; 
    }

    public void setIsLocked(Boolean isLocked) { 
        this.isLocked = isLocked; 
    }
    


    public LocalDateTime getLockedAt() { 
        return lockedAt; 
    }

    public void setLockedAt(LocalDateTime lockedAt) { 
        this.lockedAt = lockedAt; 
    }
    


    public LocalDateTime getEditedAt() { 
        return editedAt; 
    }

    public void setEditedAt(LocalDateTime editedAt) { 
        this.editedAt = editedAt; 
    }
    


    public UUID getEditedByWorkspaceMemberId() { 
        return editedByWorkspaceMemberId; 
    }

    public void setEditedByWorkspaceMemberId(UUID editedByWorkspaceMemberId) { 
        this.editedByWorkspaceMemberId = editedByWorkspaceMemberId; 
    }
    


    public TimeEntryStatus getStatus() { 
        return status; 
    }

    public void setStatus(TimeEntryStatus status) { 
        this.status = status; 
    }
    


    public LocalDateTime getSubmittedAt() { 
        return submittedAt; 
    }

    public void setSubmittedAt(LocalDateTime submittedAt) { 
        this.submittedAt = submittedAt; 
    }
    


    public UUID getReviewedByWorkspaceMemberId() { 
        return reviewedByWorkspaceMemberId; 
    }

    public void setReviewedByWorkspaceMemberId(UUID reviewedByWorkspaceMemberId) { 
        this.reviewedByWorkspaceMemberId = reviewedByWorkspaceMemberId; 
    }
    


    public LocalDateTime getReviewedAt() { 
        return reviewedAt; 
    }
    public void setReviewedAt(LocalDateTime reviewedAt) { 
        this.reviewedAt = reviewedAt; 
    }
    


    public String getRejectionReason() { 
        return rejectionReason; 
    }

    public void setRejectionReason(String rejectionReason) { 
        this.rejectionReason = rejectionReason; 
    }
    


    public LocalDateTime getCreatedAt() { 
        return createdAt; 
    }

    public LocalDateTime getUpdatedAt() { 
        return updatedAt; 
    }



    public void setCreatedAt(LocalDateTime createdAt) { 
        this.createdAt = createdAt; 
    }

    public void setUpdatedAt(LocalDateTime updatedAt) { 
        this.updatedAt = updatedAt; 
    }
}