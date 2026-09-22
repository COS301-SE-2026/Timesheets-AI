package timesheets.suggestions;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import timesheets.evidence.EvidenceEvent;
import timesheets.evidence.correlation.EvidenceGroup;

@Service
public class WorkSessionInferenceService {
  public List<SuggestedWorkSession> inferWorkSession(List<EvidenceGroup> evidenceGroups) {

    List<SuggestedWorkSession> sessions = new ArrayList<SuggestedWorkSession>();

    if (evidenceGroups == null || evidenceGroups.isEmpty()) {
      return sessions;
    }

    for (EvidenceGroup group : evidenceGroups) {
      if (group == null
          || group.getEvidenceEvents() == null
          || group.getEvidenceEvents().isEmpty()) {
        continue;
      }

      SuggestedWorkSession session = createSession(group);
      sessions.add(session);
    }

    return sessions;
  }

  private SuggestedWorkSession createSession(EvidenceGroup group) {
    SuggestedWorkSession session = new SuggestedWorkSession();

    session.setId(UUID.randomUUID());
    session.setWorkspaceMemberId(group.getWorkspaceMeberMemberId());
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

  private double calculateConfidence(EvidenceGroup group) {
    int evidenceCount = group.getEvidenceEvents().size();

    if (evidenceCount >= 4) {
      return 0.90;
    }

    if (evidenceCount == 3) {
      return 0.80;
    }

    if (evidenceCount == 2) {
      return 0.70;
    }

    return 0.50;
  }

  private Integer calculateDurationMinutes(EvidenceGroup group) {

    if (group.getStartTime() == null || group.getEndTime() == null) {
      return 0;
    }

    long minutes = java.time.Duration.between(group.getStartTime(), group.getEndTime()).toMinutes();

    return (int) minutes;
  }

}
