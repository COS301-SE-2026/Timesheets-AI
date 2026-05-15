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
    
}