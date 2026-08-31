package timesheets.dto.request;

import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Data;

/*
 - this will be for partial updates
 - all the fields are optional so only the required fields will be updated
*/
@Data
public class TimeEntryPatchRequest {

  private UUID projectId;
  private UUID taskId;
  private LocalDateTime startTime;
  private LocalDateTime endTime;
  private Integer durationSeconds;
  private String entryType;
  private String description;
  private Boolean isBillable;
}
