package timesheets.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

public class StartTimerResponse {

  private UUID id;
  private UUID projectId;
  private UUID taskId;
  private LocalDateTime startedAt;
  private Boolean active;

  public StartTimerResponse() {}

  public StartTimerResponse(
      UUID id, UUID projectId, UUID taskId, LocalDateTime startedAt, Boolean active) {
    this.id = id;
    this.projectId = projectId;
    this.taskId = taskId;
    this.startedAt = startedAt;
    this.active = active;
  }

  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public UUID getProjectId() {
    return projectId;
  }

  public void setProjectId(UUID projectId) {
    this.projectId = projectId;
  }

  public UUID getTaskId() {
    return taskId;
  }

  public void setTaskId(UUID taskId) {
    this.taskId = taskId;
  }

  public LocalDateTime getStartedAt() {
    return startedAt;
  }

  public void setStartedAt(LocalDateTime startedAt) {
    this.startedAt = startedAt;
  }

  public Boolean getActive() {
    return active;
  }

  public void setActive(Boolean active) {
    this.active = active;
  }
}
