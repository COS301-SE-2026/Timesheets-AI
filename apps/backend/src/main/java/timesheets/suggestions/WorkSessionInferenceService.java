package timesheets.suggestions;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import timesheets.evidence.EvidenceEvent;
import timesheets.evidence.correlation.EvidenceGroup;

@Service
@RequiredArgsConstructor
public class WorkSessionInferenceService {

  private final SuggestionGenerationService suggestionGenerationService;
  private final SuggestionService suggestionService;

  public List<SuggestedWorkSession> inferWorkSession(List<EvidenceGroup> evidenceGroups) {

    List<SuggestedWorkSession> sessions = new ArrayList<SuggestedWorkSession>();

    if (evidenceGroups == null || evidenceGroups.isEmpty()) {
      return sessions;
    }

    List<SuggestedWorkSession> generatedSuggestions =
        suggestionGenerationService.generateSuggestions(evidenceGroups);

    for (SuggestedWorkSession suggestion : generatedSuggestions) {
      SuggestedWorkSession saved = suggestionService.save(suggestion);

      sessions.add(saved);
    }
    return sessions;
  }

  private SuggestedWorkSession createSession(EvidenceGroup group) {
    SuggestedWorkSession session = new SuggestedWorkSession();

    session.setId(UUID.randomUUID());
    session.setWorkspaceMemberId(group.getWorkspaceMemberId());
    session.setStartTime(group.getStartTime());
    session.setEndTime(group.getEndTime());
    session.setEvidenceEvents(new ArrayList<EvidenceEvent>(group.getEvidenceEvents()));
    session.setProjectId(findProjectId(group));
    session.setTaskId(findTaskId(group));
    session.setTitle(generateTitle(group));

    double confidence = calculateConfidence(group);
    session.setConfidenceScore(confidence);
    session.setDurationMinutes(calculateDurationMinutes(group));
    session.setDurationMinutes(calculateDurationMinutes(group));
    session.setExplanation(generateExplanation(group, confidence));
    session.setStatus(SuggestionStatus.PENDING);

    return session;
  }

  private UUID findProjectId(EvidenceGroup group) {
    for (EvidenceEvent event : group.getEvidenceEvents()) {
      if (event.getProjectId() != null) {
        return event.getProjectId();
      }
    }

    return null;
  }

  private UUID findTaskId(EvidenceGroup group) {
    for (EvidenceEvent event : group.getEvidenceEvents()) {
      if (event.getTaskId() != null) {
        return event.getTaskId();
      }
    }

    return null;
  }

  private String generateTitle(EvidenceGroup group) {
    for (EvidenceEvent event : group.getEvidenceEvents()) {

      if (event.getDescription() != null && !event.getDescription().trim().isEmpty()) {
        return event.getDescription();
      }
    }

    return "Suggested work session";
  }

  private Integer calculateDurationMinutes(EvidenceGroup group) {

    if (group.getStartTime() == null || group.getEndTime() == null) {
      return 0;
    }

    long minutes = java.time.Duration.between(group.getStartTime(), group.getEndTime()).toMinutes();

    return (int) minutes;
  }

  private double calculateConfidence(EvidenceGroup group) {

    int evidenceCount = group.getEvidenceEvents().size();

    boolean hasGitHub = false;
    boolean hasJira = false;
    boolean hasCalendar = false;

    for (EvidenceEvent event : group.getEvidenceEvents()) {

      if ("GITHUB".equals(event.getSource())) {
        hasGitHub = true;
      }

      if ("JIRA".equals(event.getSource())) {
        hasJira = true;
      }

      if ("CALENDAR".equals(event.getSource())) {
        hasCalendar = true;
      }
    }

    double score = 0.0;

    if (hasGitHub) {
      score += 0.30;
    }

    if (hasJira) {
      score += 0.30;
    }

    if (hasCalendar) {
      score += 0.15;
    }

    if (evidenceCount >= 2) {
      score += 0.10;
    }

    if (evidenceCount >= 4) {
      score += 0.10;
    }

    score += group.getCorrelationScore() * 0.05;

    if (score > 1.0) {
      score = 1.0;
    }

    return Math.round(score * 100.0) / 100.0;
  }

  private String generateExplanation(EvidenceGroup group, double confidence) {

    int githubCount = 0;
    int jiraCount = 0;
    int calendarCount = 0;

    for (EvidenceEvent event : group.getEvidenceEvents()) {

      if ("GITHUB".equals(event.getSource())) {
        githubCount++;
      }

      if ("JIRA".equals(event.getSource())) {
        jiraCount++;
      }

      if ("CALENDAR".equals(event.getSource())) {
        calendarCount++;
      }
    }

    StringBuilder explanation = new StringBuilder();

    explanation.append("Hey, Here is the suggested from ");
    explanation.append(group.getEvidenceEvents().size());
    explanation.append(" pieces of evidence");

    if (githubCount > 0) {
      explanation.append(", including ").append(githubCount).append(" GitHub activity");
    }

    if (jiraCount > 0) {
      explanation.append(", ").append(jiraCount).append(" Jira activity");
    }

    if (calendarCount > 0) {
      explanation.append(", and ").append(calendarCount).append(" Calendar activity");
    }

    explanation.append(". Confidence: ").append(confidence);

    return explanation.toString();
  }
}
