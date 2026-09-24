package timesheets.suggestions;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import timesheets.evidence.EvidenceEvent;
import timesheets.evidence.correlation.EvidenceGroup;

@Service
@RequiredArgsConstructor
public class SuggestionGenerationService {
  public SuggestedWorkSession generateSuggestion(EvidenceGroup group) {

    SuggestedWorkSession suggestion = new SuggestedWorkSession();

    suggestion.setWorkspaceMemberId(group.getWorkspaceMemberId());

    suggestion.setStartTime(group.getStartTime());
    suggestion.setEndTime(group.getEndTime());

    suggestion.setEvidenceEvents(new ArrayList<EvidenceEvent>(group.getEvidenceEvents()));
    suggestion.setConfidenceScore(group.getCorrelationScore());
    suggestion.setDurationMinutes(
        calculateDurationMinutes(group.getStartTime(), group.getEndTime()));
    suggestion.setProjectId(findProjectId(group.getEvidenceEvents()));
    suggestion.setTaskId(findTaskId(group.getEvidenceEvents()));
    suggestion.setTitle(generateTitle(group.getEvidenceEvents()));
    suggestion.setExplanation(generateExplanation(group));
    suggestion.setStatus(SuggestionStatus.PENDING);

    return suggestion;
  }

  public List<SuggestedWorkSession> generateSuggestions(List<EvidenceGroup> groups) {

    List<SuggestedWorkSession> suggestions = new ArrayList<SuggestedWorkSession>();

    for (EvidenceGroup group : groups) {

      SuggestedWorkSession suggestion = generateSuggestion(group);

      suggestions.add(suggestion);
    }

    return suggestions;
  }

  private Integer calculateDurationMinutes(LocalDateTime startTime, LocalDateTime endTime) {

    if (startTime == null || endTime == null) {
      return 0;
    }

    return (int) Duration.between(startTime, endTime).toMinutes();
  }

  private UUID findProjectId(List<EvidenceEvent> evidenceEvents) {

    for (EvidenceEvent event : evidenceEvents) {
      if (event.getProjectId() != null) {
        return event.getProjectId();
      }
    }

    return null;
  }

  private UUID findTaskId(List<EvidenceEvent> evidenceEvents) {

    for (EvidenceEvent event : evidenceEvents) {
      if (event.getTaskId() != null) {
        return event.getTaskId();
      }
    }

    return null;
  }

  private String generateTitle(List<EvidenceEvent> evidenceEvents) {

    if (evidenceEvents.isEmpty()) {
      return "Suggested work session";
    }

    EvidenceEvent firstEvent = evidenceEvents.get(0);

    if (firstEvent.getDescription() != null && !firstEvent.getDescription().isBlank()) {

      return firstEvent.getDescription();
    }

    return "Suggested work session";
  }

  private String generateExplanation(EvidenceGroup group) {

    int evidenceCount = group.getEvidenceEvents().size();

    return "This suggestion was generated from " + evidenceCount + " related evidence events.";
  }
}
