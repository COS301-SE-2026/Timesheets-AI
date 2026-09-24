package timesheets.suggestions;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.Data;
import timesheets.domain.SuggestedWorkSessionEntity;
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

  public SuggestedWorkSessionEntity toEntity() {

    return SuggestedWorkSessionEntity.builder()
        .id(id)
        .workspaceMemberId(workspaceMemberId)
        .startTime(startTime)
        .endTime(endTime)
        .title(title)
        .projectId(projectId)
        .taskId(taskId)
        .confidenceScore(confidenceScore)
        .durationMinutes(durationMinutes)
        .explanation(explanation)
        .status(status != null ? status.name() : null)
        .build();
  }

  public static SuggestedWorkSession fromEntity(SuggestedWorkSessionEntity entity) {

    SuggestedWorkSession suggestion = new SuggestedWorkSession();

    suggestion.setId(entity.getId());
    suggestion.setWorkspaceMemberId(entity.getWorkspaceMemberId());
    suggestion.setStartTime(entity.getStartTime());
    suggestion.setEndTime(entity.getEndTime());
    suggestion.setTitle(entity.getTitle());
    suggestion.setProjectId(entity.getProjectId());
    suggestion.setTaskId(entity.getTaskId());
    suggestion.setConfidenceScore(entity.getConfidenceScore());
    suggestion.setDurationMinutes(entity.getDurationMinutes());
    suggestion.setExplanation(entity.getExplanation());

    if (entity.getStatus() != null) {
      suggestion.setStatus(SuggestionStatus.valueOf(entity.getStatus()));
    }

    return suggestion;
  }
}
