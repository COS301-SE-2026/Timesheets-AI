package timesheets.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.LocalDateTime;

//DTO - data transfer object

// this file will essentially be what the front-end sends to create the time-entry
// defines the JSON that frontend must send and then SProngBoot will automatically map the JSON to the object field getters and setters
public class TimeEntryRequest {

    @NotNull(message = "Project ID is required")
    private Long projectId;

    private Long taskId;
    private LocalDateTime startTime;
    private LocalDateTime endTime;

    @Positive(message = "Duration must be positive")
    private Integer durationMinutes;
    private String entryType;
    private String description;

    //the getters and setters are below
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
    
}