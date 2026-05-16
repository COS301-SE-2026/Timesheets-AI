package timesheets.dto.response;

import timesheets.domain.TimeEntry;
import timesheets.enums.TimeEntryStatus;
import java.time.LocalDateTime;

//this is what the backend will send back to the frontend, when a a time entry is created or retrieved

public class TimeEntryResponse {
    
    private Long id;
    private Long workspaceMemberId;
    private Long projectId;
    private Long taskId;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer durationMinutes;
    private String entryType;
    private String description;
    private TimeEntryStatus status;
    private Boolean isLocked;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String rejectionReason;
    
    public static TimeEntryResponse from(TimeEntry entry) {

        TimeEntryResponse response = new TimeEntryResponse();

        response.setId(entry.getId());
        response.setWorkspaceMemberId(entry.getWorkspaceMemberId());
        response.setProjectId(entry.getProjectId());
        response.setTaskId(entry.getTaskId());
        response.setStartTime(entry.getStartTime());
        response.setEndTime(entry.getEndTime());
        response.setDurationMinutes(entry.getDurationMinutes());
        response.setEntryType(entry.getEntryType());
        response.setDescription(entry.getDescription());
        response.setStatus(entry.getStatus());
        response.setIsLocked(entry.getIsLocked());
        response.setCreatedAt(entry.getCreatedAt());
        response.setUpdatedAt(entry.getUpdatedAt());
        response.setRejectionReason(entry.getRejectionReason());

        return response;
    }
    
}