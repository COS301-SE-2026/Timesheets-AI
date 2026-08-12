package timesheets.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import timesheets.domain.TimeEntry;

// this is what the backend will send back to the frontend, when a time entry is created or
// retrieved
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TimeEntryResponse {

  private UUID id;
  private UUID timesheetId;
  private UUID workspaceMemberId;
  private UUID projectId;
  private UUID taskId;
  private LocalDateTime startTime;
  private LocalDateTime endTime;
  private Integer durationSeconds;
  private String entryType;
  private String description;
  private Boolean isDeleted;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;

  public static TimeEntryResponse from(TimeEntry entry) {

    return TimeEntryResponse.builder()
        .id(entry.getId())
        .timesheetId(entry.getTimesheetId())
        .workspaceMemberId(entry.getWorkspaceMemberId())
        .projectId(entry.getProjectId())
        .taskId(entry.getTaskId())
        .startTime(entry.getStartTime())
        .endTime(entry.getEndTime())
        .durationSeconds(entry.getDurationSeconds())
        .entryType(entry.getEntryType())
        .description(entry.getDescription())
        .isDeleted(entry.getIsDeleted())
        .createdAt(entry.getCreatedAt())
        .updatedAt(entry.getUpdatedAt())
        .build();
  }
}
