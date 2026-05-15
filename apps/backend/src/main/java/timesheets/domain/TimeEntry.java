//I'm writing comments so that it makes it easier when you are reviewing code and you want to make sense of what is happening, don't think I will add to all the files, instead I will just refer you to this file maybe??

package timesheets.domain;

import timesheets.enums.TimeEntryStatus;
import jakarta.persistence.*;
import java.time.LocalDateTime;


//so this is the JPA entity- you can kinda think of it like the data template
// it's basically showing us what the time entry looks like in the database
// a java object is what the row looks like in the database

@Entity //this tells Java that the class maps to a DB
@Table(name = "time_entries") //what the table will be called in the database, otherwise TimeEntry would be used

public class TimeEntry {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "workspace_member_id", nullable = false)
    private Long workspaceMemberId;
    
    @Column(name = "project_id", nullable = false)
    private Long projectId;
    
    @Column(name = "task_id")
    private Long taskId;
    
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
    private Long editedByWorkspaceMemberId;
    
    @Enumerated(EnumType.STRING)
    private TimeEntryStatus status = TimeEntryStatus.DRAFT;
    
    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;
    
    @Column(name = "reviewed_by_workspace_member_id")
    private Long reviewedByWorkspaceMemberId;
    
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
    
}