package timesheets.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import timesheets.domain.Project;
import timesheets.enums.WorkspaceRole;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectResponse {
  private UUID id;
  private String name;
  private String description;
  private String status;
  private BigDecimal budgetHours;
  private BigDecimal hourlyRate;
  private BigDecimal budgetCost;
  private LocalDate startDate;
  private LocalDate endDate;
  private WorkspaceRole myRole; // the role that the user has on the project
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;

  public static ProjectResponse from(Project project, WorkspaceRole myRole) {
    return ProjectResponse.builder()
        .id(project.getId())
        .name(project.getName())
        .description(project.getDescription())
        .status(project.getStatus())
        .budgetHours(project.getBudgetHours())
        .hourlyRate(project.getHourlyRate())
        .budgetCost(project.getBudgetCost())
        .startDate(project.getStartDate())
        .endDate(project.getEndDate())
        .myRole(myRole)
        .createdAt(project.getCreatedAt())
        .updatedAt(project.getUpdatedAt())
        .build();
  }
}
