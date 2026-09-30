package timesheets.suggestions;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

    List<EvidenceEvent> evidenceEvents = new ArrayList<EvidenceEvent>(group.getEvidenceEvents());

    suggestion.setWorkspaceMemberId(group.getWorkspaceMemberId());
    suggestion.setStartTime(group.getStartTime());
    suggestion.setEndTime(group.getEndTime());
    suggestion.setEvidenceEvents(evidenceEvents);
    suggestion.setConfidenceScore(group.getCorrelationScore());

    Integer durationSeconds = calculateDurationSeconds(evidenceEvents);

    suggestion.setDurationSeconds(durationSeconds);

    if (durationSeconds != null) {
      suggestion.setDurationMinutes(durationSeconds / 60);
    } else {
      suggestion.setDurationMinutes(null);
    }

    suggestion.setProjectId(findProjectId(evidenceEvents));
    suggestion.setTaskId(findTaskId(evidenceEvents));
    suggestion.setTitle(generateTitle(evidenceEvents));
    suggestion.setDescription(generateDescription(evidenceEvents));
    suggestion.setExplanation(generateExplanation(group));
    suggestion.setEntryType("AI_SUGGESTION");
    suggestion.setStatus(SuggestionStatus.PENDING);

    return suggestion;
  }

  public List<SuggestedWorkSession> generateSuggestions(List<EvidenceGroup> groups) {

    List<SuggestedWorkSession> suggestions = new ArrayList<SuggestedWorkSession>();

    if (groups == null || groups.isEmpty()) {
      return suggestions;
    }

    for (EvidenceGroup group : groups) {
      if (group == null) {
        continue;
      }

      SuggestedWorkSession suggestion = generateSuggestion(group);
      suggestions.add(suggestion);
    }

    return suggestions;
  }

  private Integer calculateDurationSeconds(List<EvidenceEvent> evidenceEvents) {

    if (evidenceEvents == null || evidenceEvents.isEmpty()) {
      return null;
    }

    Integer knownDuration = calculateKnownDurationSeconds(evidenceEvents);

    if (knownDuration != null && knownDuration > 0) {
      return knownDuration;
    }

    if (evidenceEvents.size() < 2) {
      return null;
    }

    LocalDateTime earliest = null;
    LocalDateTime latest = null;

    for (EvidenceEvent event : evidenceEvents) {
      if (event == null || event.getTimestamp() == null) {
        continue;
      }

      LocalDateTime eventStart = event.getTimestamp();
      LocalDateTime eventEnd = getEventEndTime(event);

      if (earliest == null || eventStart.isBefore(earliest)) {
        earliest = eventStart;
      }

      if (eventEnd != null && (latest == null || eventEnd.isAfter(latest))) {
        latest = eventEnd;
      }
    }

    if (earliest == null || latest == null || !latest.isAfter(earliest)) {
      return null;
    }

    long duration = Duration.between(earliest, latest).getSeconds();

    if (duration <= 0) {
      return null;
    }

    return (int) duration;
  }

  private Integer calculateKnownDurationSeconds(List<EvidenceEvent> evidenceEvents) {

    int totalSeconds = 0;

    for (EvidenceEvent event : evidenceEvents) {
      if (event == null || event.getTimestamp() == null || event.getEndTime() == null) {
        continue;
      }

      if (!event.getEndTime().isAfter(event.getTimestamp())) {
        continue;
      }

      long seconds = Duration.between(event.getTimestamp(), event.getEndTime()).getSeconds();

      totalSeconds += (int) seconds;
    }

    if (totalSeconds <= 0) {
      return null;
    }

    return totalSeconds;
  }

  private LocalDateTime getEventEndTime(EvidenceEvent event) {

    if (event.getEndTime() != null) {
      return event.getEndTime();
    }

    return event.getTimestamp();
  }

  private UUID findProjectId(List<EvidenceEvent> evidenceEvents) {
    Map<UUID, Integer> projectCounts = new HashMap<>();

    for (EvidenceEvent event : evidenceEvents) {
      if (event != null && event.getProjectId() != null) {
        projectCounts.merge(event.getProjectId(), 1, Integer::sum);
      }
    }

    return findDominantId(projectCounts);
  }

  private UUID findTaskId(List<EvidenceEvent> evidenceEvents) {
    Map<UUID, Integer> taskCounts = new HashMap<>();

    for (EvidenceEvent event : evidenceEvents) {
      if (event != null && event.getTaskId() != null) {
        taskCounts.merge(event.getTaskId(), 1, Integer::sum);
      }
    }

    return findDominantId(taskCounts);
  }

  private UUID findDominantId(Map<UUID, Integer> counts) {
    if (counts.isEmpty()) {
      return null;
    }

    UUID dominantId = null;
    int highestCount = 0;
    boolean tie = false;

    for (Map.Entry<UUID, Integer> entry : counts.entrySet()) {
      int count = entry.getValue();

      if (count > highestCount) {
        dominantId = entry.getKey();
        highestCount = count;
        tie = false;
      } else if (count == highestCount) {
        tie = true;
      }
    }

    return tie ? null : dominantId;
  }

  private String generateTitle(List<EvidenceEvent> evidenceEvents) {

    String taskTitle = findTaskTitle(evidenceEvents);

    if (taskTitle != null) {
      return taskTitle;
    }

    String jiraTitle = findJiraIssueTitle(evidenceEvents);

    if (jiraTitle != null) {
      return jiraTitle;
    }

    String calendarTitle = findCalendarTitle(evidenceEvents);

    if (calendarTitle != null) {
      return calendarTitle;
    }

    String commitMessage = findCommitMessage(evidenceEvents);

    if (commitMessage != null) {
      return commitMessage;
    }

    return "Suggested work session";
  }

  private String findTaskTitle(List<EvidenceEvent> evidenceEvents) {

    for (EvidenceEvent event : evidenceEvents) {
      String title = getMetadataString(event, "taskTitle");

      if (isValidText(title)) {
        return title;
      }
    }

    return null;
  }

  private String findJiraIssueTitle(List<EvidenceEvent> evidenceEvents) {
    for (EvidenceEvent event : evidenceEvents) {
      if ("JIRA".equalsIgnoreCase(event.getSource())) {
        String title = getMetadataString(event, "title");
        if (isValidText(title)) {
          return title;
        }
      }
    }
    return null;
  }

  private String findCalendarTitle(List<EvidenceEvent> evidenceEvents) {
    for (EvidenceEvent event : evidenceEvents) {
      if ("CALENDAR".equalsIgnoreCase(event.getSource())) {
        String title = getMetadataString(event, "title");
        if (isValidText(title)) {
          return title;
        }
      }
    }
    return null;
  }

  private String findCommitMessage(List<EvidenceEvent> evidenceEvents) {
    for (EvidenceEvent event : evidenceEvents) {
      if ("GITHUB".equalsIgnoreCase(event.getSource()) && isValidText(event.getDescription())) {
        return event.getDescription();
      }
    }
    return null;
  }

  private String generateDescription(List<EvidenceEvent> evidenceEvents) {
    List<String> eventSummaries = new ArrayList<>();

    for (EvidenceEvent event : evidenceEvents) {
      if (event == null) {
        continue;
      }
      if (isValidText(event.getDescription())) {
        eventSummaries.add(event.getDescription());
      }
    }

    if (eventSummaries.isEmpty()) {
      return null;
    }

    return String.join(" | ", eventSummaries);
  }

  private String generateExplanation(EvidenceGroup group) {
    int evidenceCount = group.getEvidenceEvents().size();
    return "This suggestion was generated from " + evidenceCount + " related evidence events.";
  }

  private String getMetadataString(EvidenceEvent event, String key) {
    if (event == null || event.getMetadata() == null) {
      return null;
    }

    Object value = event.getMetadata().get(key);
    return value != null ? value.toString() : null;
  }

  private boolean isValidText(String value) {
    return value != null && !value.trim().isEmpty();
  }
}
