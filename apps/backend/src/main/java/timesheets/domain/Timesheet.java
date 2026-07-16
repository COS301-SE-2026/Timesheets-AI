package timesheets.domain;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/*with the reafctoring the timesheet is now the one that will be submitted instead of a time-entry.
this is what more so matches what TMetric does*/

@Entity
@Table(name = "timesheets")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Timesheet {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(name = "workspace_member_id", nullable = false)
  private UUID workspaceMemberId;

  @Column(name = "period_start", nullable = false)
  private LocalDate periodStart;

  @Column(name = "period_end", nullable = false)
  private LocalDate periodEnd;

  @Column(name = "status")
  @Builder.Default
  private String status = "DRAFT";

  @Column(name = "submitted_at")
  private LocalDateTime submittedAt;

  @Column(name = "approved_at")
  private LocalDateTime approvedAt;

  @Column(name = "approved_by_workspace_member_id")
  private UUID approvedByWorkspaceMemberId;

  @Column(name = "rejected_at")
  private LocalDateTime rejectedAt;

  @Column(name = "rejection_reason")
  private String rejectionReason;

  @Column(name = "is_locked")
  @Builder.Default
  private Boolean isLocked = false;

  @Column(name = "locked_at")
  private LocalDateTime lockedAt;

  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;

  @PrePersist
  protected void onCreate() {
    createdAt = LocalDateTime.now();
    updatedAt = LocalDateTime.now();
    if (status == null) {
      status = "DRAFT";
    }
    if (isLocked == null) {
      isLocked = false;
    }
  }

  @PreUpdate
  protected void onUpdate() {
    updatedAt = LocalDateTime.now();
  }
}
