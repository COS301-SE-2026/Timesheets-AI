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
    
    //my getters and setters below
    public Long getId() { 
        return id; 
    }

    public void setId(Long id) { 
        this.id = id; 
    }
    


    public Long getWorkspaceMemberId() { 
        return workspaceMemberId; 
    }

    public void setWorkspaceMemberId(Long workspaceMemberId) { 
        this.workspaceMemberId = workspaceMemberId; 
    }
    


    public Long getProjectId() { 
        return projectId; 
    }

    public void setProjectId(Long projectId) { 
        this.projectId = projectId; 
    }
    


    public Long getTaskId() { 
        return taskId; 
    }

    public void setTaskId(Long taskId) { 
        this.taskId = taskId; 
    }
    


    public LocalDateTime getStartTime() { 
        return startTime; 
    }

    public void setStartTime(LocalDateTime startTime) { 
        this.startTime = startTime; 
    }
    


    public LocalDateTime getEndTime() { 
        return endTime; 
    }

    public void setEndTime(LocalDateTime endTime) { 
        this.endTime = endTime; 
    }
    
    
    public Integer getDurationMinutes() { 
        return durationMinutes; 
    }

    public void setDurationMinutes(Integer durationMinutes) { 
        this.durationMinutes = durationMinutes; 
    }
    

    public String getEntryType() { 
        return entryType; 
    }

    public void setEntryType(String entryType) { 
        this.entryType = entryType; 
    }
    


    public String getDescription() { 
        return description; 
    }

    public void setDescription(String description) { 
        this.description = description; 
    }
    
    
    public TimeEntryStatus getStatus() { 
        return status; 
    }

    public void setStatus(TimeEntryStatus status) { 
        this.status = status; 
    }
    


    public Boolean getIsLocked() { 
        return isLocked; 
    }

    public void setIsLocked(Boolean isLocked) { 
        this.isLocked = isLocked; 
    }
    

    public LocalDateTime getCreatedAt() { 
        return createdAt; 
    }

    public void setCreatedAt(LocalDateTime createdAt) { 
        this.createdAt = createdAt; 
    }
    

    public LocalDateTime getUpdatedAt() { 
        return updatedAt; 
    }

    public void setUpdatedAt(LocalDateTime updatedAt) { 
        this.updatedAt = updatedAt; 
    }


    public String getRejectionReason() { 
        return rejectionReason; 
    }

    public void setRejectionReason(String rejectionReason) { 
        this.rejectionReason = rejectionReason; 
    }
}