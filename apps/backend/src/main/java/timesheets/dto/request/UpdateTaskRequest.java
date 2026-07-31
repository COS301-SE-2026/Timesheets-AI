package timesheets.dto.request;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import lombok.Data;

/*
- I am making a comprehensive file that contains all the possible task requests
- the reason I chose to do this is because there are so many possible ways to update a task
- I cannot create a file for each of these, further I cannot create one class that can update all the fields because
        because this can overwhelm frontend, as well as overwhelm myself
*/

/*
- update task request is a container class for all the task update DTO
- each innner class will handle a specific type of request */
@Data
public class UpdateTaskRequest {

  // request to update the task priority
  @Data
  public static class UpdatePriority {
    @Pattern(
        regexp = "LOW|MEDIUM|HIGH|CRITICAL",
        message = "Priority must be LOW, MEDIUM, HIGH or CRITICAL")
    private String priority = "MEDIUM";
  }

  // request to update the status
  @Data
  public static class UpdateStatus {
    @Pattern(
        regexp = "TODO|IN_PROGRESS|DONE|BLOCKED",
        message = "Status must be TODO, IN_PROGRESS, DONE or BLOCKED")
    private String status;
  }

  // request for assigning or reassigning a task to a workspace member
  @Data
  public static class Assign {
    @NotNull(message = "Workspace member ID is required")
    private UUID assignedWorkspaceMemberId;
  }

  // request to update task schedule, hours and details
  @Data
  public static class UpdateSchedule {
    private String title;
    private String description;

    @Positive(message = "Estimated hours must be positive")
    private BigDecimal estimatedHours;

    @Positive(message = "Actual hours must be positive")
    private BigDecimal actualHours;

    @FutureOrPresent(message = "Due date cannot be in the past")
    private LocalDate dueDate;

    private UUID projectId;
    private String jiraTicketKey;
    private UUID parentTaskId;
  }

  /*
  - I would like to do a request to update the full task - maybe if a user requests from admin that a full task is updated??
  - I am not sure if it is necessary for now, so I will leave it
  */

}
