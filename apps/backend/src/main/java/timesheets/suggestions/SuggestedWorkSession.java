package timesheets.suggestions;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.Data;
import timesheets.evidence.EvidenceEvent;

@Data
public class SuggestedWorkSession {
  private UUID id;
  private UUID workspaceMemberId;
  private LocalDateTime startTime;
  private LocalDateTime endTime;
  // what we think the developer is using - will use AI to make it more readable
  private String title;
  private UUID projectId;
  private UUID taskId;
  private List<EvidenceEvent> evidenceEvents = new ArrayList<EvidenceEvent>();
  private double confidenceScore;
  private Integer durationMinutes;
  private String explanation;
  private SuggestionStatus status;
}
