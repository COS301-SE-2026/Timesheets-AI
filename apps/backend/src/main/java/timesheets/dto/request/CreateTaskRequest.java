package timesheets.dto.request;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateTaskRequest {

  @NotBlank(message = "Task title is required")
  private String title;

  private String description;

  @NotNull(message = "Project ID is required")
  private UUID projectId;

  private String jiraTicketId;

  private UUID parentTaskId;

  @Pattern(
      regexp = "TODO|IN_PROGESS|DONE|BLOCKED",
      message = "Status must be TODO, IN_PROGRESS, DONE or BLOCKED")
  @Builder.Default
  private String status = "TODO";

  @Positive(message = "Estimated hours must be positive")
  private BigDecimal estimatedHours;

  private UUID assignedWorkspaceMemberId;

  @FutureOrPresent(message = "Due date cannot be in the past")
  private LocalDate dueDate;

  @Pattern(
      regexp = "LOW|MEDIUM|HIGH| CRITICAL",
      message = "Priority must be LOW, MEDIUM, HIGH or CRITICAL")
  @Builder.Default
  private String priority = "MEDIUM";
}
