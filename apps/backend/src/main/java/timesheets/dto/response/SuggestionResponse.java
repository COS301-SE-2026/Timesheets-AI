package timesheets.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Data;
import timesheets.suggestions.SuggestionStatus;

@Data
public class SuggestionResponse {
  private UUID id;
  private UUID workspaceMemberId;
  private UUID projectId;
  private UUID taskId;
  private String title;
  private LocalDateTime startTime;
  private LocalDateTime endTime;
  private Integer durationMinutes;
  private double confidenceScore;
  private String explantion;
  private SuggestionStatus status;
}
