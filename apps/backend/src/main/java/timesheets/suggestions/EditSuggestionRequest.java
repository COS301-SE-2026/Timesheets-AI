package timesheets.suggestions;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class EditSuggestionRequest {

  private String title;
  private LocalDateTime startTime;
  private LocalDateTime endTime;
}
