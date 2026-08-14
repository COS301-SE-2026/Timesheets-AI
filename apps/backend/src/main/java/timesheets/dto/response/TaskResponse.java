package timesheets.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import timesheets.domain.Task;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskResponse {
  private UUID id;
  private UUID projectId;
  private String projectName;
  private String jiraTicketKey;
  private UUID parentTaskId;
  private String title;
  private String description;
  private String status;
  private String priority;
  private BigDecimal estimatedHours;
  private BigDecimal actualHours;
  private String assignedToName;
  private UUID assignedWorkspaceMemberId;
  private LocalDate dueDate;
  private LocalDateTime completedAt;
  private Boolean isDeleted;
  private LocalDateTime deletedAt;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;

  public static TaskResponse from(Task task) {
    return TaskResponse.builder()
        .id(task.getId())
        .projectId(task.getProjectId())
        .jiraTicketKey(task.getJiraTicketKey())
        .parentTaskId(task.getParentTaskId())
        .title(task.getTitle())
        .description(task.getDescription())
        .status(task.getStatus())
        .priority(task.getPriority())
        .estimatedHours(task.getEstimatedHours())
        .actualHours(task.getActualHours())
        .assignedWorkspaceMemberId(task.getAssignedWorkspaceMemberId())
        .dueDate(task.getDueDate())
        .completedAt(task.getCompletedAt())
        .createdAt(task.getCreatedAt())
        .updatedAt(task.getUpdatedAt())
        .build();
  }

  public static TaskResponse fromWithDetails(Task task, String projectName, String assignedToName) {
    TaskResponse response = from(task);

    response.setProjectName(projectName);
    response.setAssignedToName(assignedToName);

    return response;
  }
}
