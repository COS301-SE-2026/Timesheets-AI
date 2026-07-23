package timesheets.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LeaveRequestResponse {

  private UUID id;
  private UUID workspaceMemberId;
  private String memberName;
  private String leaveType;
  private LocalDate startDate;
  private LocalDate endDate;
  private Double totalDays;
  private String reason;
  private String attachments;
  private String status;
  private String approvedByName;
  private LocalDateTime approvedAt;
  private String rejectionReason;
  private UUID availabilityId;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
}
