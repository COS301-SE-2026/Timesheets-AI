package timesheets.suggestions;

import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Data;

@Data
public class EditSuggestionRequest {

  private String title;
  private LocalDateTime startTime;
  private LocalDateTime endTime;
  private UUID projectId;
  private UUID taskId;
  private String description;
}
