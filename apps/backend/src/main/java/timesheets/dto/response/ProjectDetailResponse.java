package timesheets.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import timesheets.enums.WorkspaceRole;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectDetailResponse {
  private UUID id;
  private String name;
  private String description;
  private String status;
  private BigDecimal budgetHours;
  private BigDecimal hourlyRate;
  private BigDecimal budgetCost;
  private BigDecimal totalCost;
  private List<MemberInfo> members;
  private BigDecimal hoursLogged;
  private BigDecimal progressPercentage; // hoursLogged / budgetHours * 100
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class MemberInfo {
    private UUID workspaceMemberId;
    private String firstName;
    private String lastName;
    private String email;
    private WorkspaceRole role; // manager or a dev
    private BigDecimal hoursLogged;
    private LocalDateTime joinedAt;
  }
}
