package timesheets.domain;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "leave_requests")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LeaveRequest {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(name = "workspace_member_id", nullable = false)
  private UUID workspaceMemberId;

  @Column(name = "leave_type", nullable = false, length = 30)
  private String leaveType;

  @Column(name = "start_date", nullable = false)
  private LocalDate startDate;

  @Column(name = "end_date", nullable = false)
  private LocalDate endDate;

  @Column(name = "total_days", nullable = false, precision = 4, scale = 1)
  private Double totalDays;

  @Column(name = "reason")
  private String reason;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "attachments", columnDefinition = "jsonb")
  private String attachments;

  @Column(name = "status", nullable = false, length = 20)
  @Builder.Default
  private String status = "PENDING";

  @Column(name = "approved_by_workspace_member_id")
  private UUID approvedByWorkspaceMemberId;

  @Column(name = "approved_at")
  private LocalDateTime approvedAt;

  @Column(name = "rejection_reason")
  private String rejectionReason;

  @Column(name = "availability_id")
  private UUID availabilityId;

  @Column(name = "created_at", updatable = false)
  private LocalDateTime createdAt;

  @Column(name = "updated_at")
  private LocalDateTime updatedAt;

  @PrePersist
  protected void onCreate() {
    createdAt = LocalDateTime.now();
    updatedAt = LocalDateTime.now();

    if (status == null) {
      status = "PENDING";
    }
  }

  @PreUpdate
  protected void onUpdate() {
    updatedAt = LocalDateTime.now();
  }
}
