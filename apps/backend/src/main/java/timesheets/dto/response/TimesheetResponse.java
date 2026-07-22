package timesheets.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import timesheets.domain.Timesheet;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TimesheetResponse {
  private UUID id;
  private UUID workspaceMemberId;
  private LocalDate periodStart;
  private LocalDate periodEnd;
  private String status;
  private LocalDateTime submittedAt;
  private LocalDateTime approvedAt;
  private UUID approvedByWorkspaceMemberId;
  private LocalDateTime rejectedAt;
  private String rejectionReason;
  private Boolean isLocked;
  private LocalDateTime lockedAt;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;

  public static TimesheetResponse from(Timesheet timesheet) {
    if (timesheet == null) {
      return null;
    }
    return TimesheetResponse.builder()
        .id(timesheet.getId())
        .workspaceMemberId(timesheet.getWorkspaceMemberId())
        .periodStart(timesheet.getPeriodStart())
        .periodEnd(timesheet.getPeriodEnd())
        .status(timesheet.getStatus())
        .submittedAt(timesheet.getSubmittedAt())
        .approvedAt(timesheet.getApprovedAt())
        .approvedByWorkspaceMemberId(timesheet.getApprovedByWorkspaceMemberId())
        .rejectedAt(timesheet.getRejectedAt())
        .rejectionReason(timesheet.getRejectionReason())
        .isLocked(timesheet.getIsLocked())
        .lockedAt(timesheet.getLockedAt())
        .createdAt(timesheet.getCreatedAt())
        .updatedAt(timesheet.getUpdatedAt())
        .build();
  }
}
