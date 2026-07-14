package timesheets.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// so the the JPA entity, a project will have task associated to it, so because there are tasks
// needed and tasks are associated to timers I have to have this here

@Entity
@Table(name = "projects")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Project {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(name = "workspace_id", nullable = false)
  private UUID workspaceId;

  @Column(name = "name", nullable = false)
  private String name;

  @Column(name = "description")
  private String description;

  @Column(name = "status")
  @Builder.Default
  private String status = "ACTIVE";

  @Column(name = "budget_hours")
  private Double budgetHours;

  @Column(name = "budget_cost")
  private Double budgetCost;

  @Column(name = "hourly_rate")
  private Double hourlyRate;

  @Column(name = "start_date")
  private LocalDateTime startDate;

  @Column(name = "end_date")
  private LocalDateTime endDate;

  @Column(name = "is_deleted")
  @Builder.Default
  private Boolean isDeleted = false;

  @Column(name = "deleted_at")
  private LocalDateTime deletedAt;

  @Column(name = "created_by_workspace_member_id")
  private UUID createdByWorkspaceMemberId;

  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @Column(name = "updated_at")
  private LocalDateTime updatedAt;

  @PrePersist // remember we want to to run before the record inserted
  protected void onCreate() {
    createdAt = LocalDateTime.now();
    updatedAt = LocalDateTime.now();

    if (status == null) {
      status = "ACTIVE";
    }

    if (isDeleted == null) {
      isDeleted = false;
    }
  }

  @PreUpdate // remember makes the function run right before an entity is updated in the DB
  protected void onUpdate() {
    updatedAt = LocalDateTime.now();
  }
}
