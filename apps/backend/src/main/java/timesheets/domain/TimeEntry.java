package timesheets.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// so this is the JPA entity- you can kinda think of it like the data template
// it's basically showing us what the time entry looks like in the database
// a java object is what the row looks like in the database

@Entity // this tells Java that the class maps to a DB
@Table(
    name =
        "time_entries") // what the table will be called in the database, otherwise TimeEntry would
// be used
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TimeEntry {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(name = "timesheet_id")
  private UUID timesheetId;

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

  @Column(name = "duration_seconds")
  private Integer durationSeconds;

  @Column(name = "entry_type")
  private String entryType;

  private String description;

  @Column(name = "is_deleted")
  @Builder.Default
  private Boolean isDeleted = false;

  @Column(name = "is_locked")
  @Builder.Default
  private Boolean isLocked = false;

  @Column(name = "deleted_at")
  private LocalDateTime deletedAt;

  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @Column(name = "updated_at")
  private LocalDateTime updatedAt;

  @PrePersist // this makes the method run before the entity is saved
  protected void onCreate() {
    createdAt = LocalDateTime.now(); // sets the timestamp before the insert
    updatedAt = LocalDateTime.now();

    if (entryType == null) {
      entryType = "MANUAL";
    }

    if (isDeleted == null) {
      isDeleted = false;
    }
  }

  @PreUpdate // runs before the entity is updated
  protected void onUpdate() {
    updatedAt = LocalDateTime.now();
  }
}
