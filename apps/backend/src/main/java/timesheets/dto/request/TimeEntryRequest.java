package timesheets.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Data;

// DTO - data transfer object

// this file will essentially be what the front-end sends to create the time-entry, so basically
// this is what backend expects
// defines the JSON that frontend must send and then SpringBoot will automatically map the JSON to
// the object field getters and setters
@Data
public class TimeEntryRequest {

  @NotNull(message = "Project ID is required")
  private UUID projectId;

  private UUID taskId;

  private LocalDateTime startTime;
  private LocalDateTime endTime;

  @Positive(message = "Duration must be positive")
  private Integer durationSeconds;

  private String entryType;
  private String description;
}
