package timesheets.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// tasks will belong to projects and they can be asisgned to a manager

@Entity
@Table(name = "tasks")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Task {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(name = "project_id", nullable = false)
  private UUID projectId;

  @Column(name = "jira_ticket_key")
  private String jiraTicketKey;

  @Column(name = "parent_task_id")
  private UUID parentTaskId;

  @Column(name = "title", nullable = false)
  private String title;

  @Column(name = "description")
  private String description;

  @Column(name = "status")
  @Builder.Default
  private String status = "TODO";

  @Column(name = "priority")
  @Builder.Default
  private String priority = "MEDIUM";

  @Column(name = "estimated_hours")
  private BigDecimal estimatedHours;

  @Column(name = "actual_hours")
  private BigDecimal actualHours;

  @Column(name = "assigned_workspace_member_id")
  private UUID assignedWorkspaceMemberId;

  @Column(name = "due_date")
  private LocalDate dueDate;

  @Column(name = "completed_at")
  private LocalDateTime completedAt;

  @Column(name = "is_deleted")
  @Builder.Default
  private Boolean isDeleted = false;

  @Column(name = "deleted_at")
  private LocalDateTime deletedAt;

  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @Column(name = "updated_at")
  private LocalDateTime updatedAt;

  @PrePersist
  protected void onCreate() {
    createdAt = LocalDateTime.now();
    updatedAt = LocalDateTime.now();

    if (status == null) {
      status = "TODO";
    }

    if (priority == null) {
      priority = "MEDIUM";
    }

    if (isDeleted == null) {
      isDeleted = false;
    }
  }

  @PreUpdate
  protected void onUpdate() {
    updatedAt = LocalDateTime.now();
  }
}
