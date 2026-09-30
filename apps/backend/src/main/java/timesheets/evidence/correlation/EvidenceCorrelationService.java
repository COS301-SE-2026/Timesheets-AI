/* For every evidence event:
- find existing groups belonging to the same developer
- if event is within correlation window of an existing group
- calculate relationship strength
- if strong enough: add event to group
- otherwise: create a new group

BUT before all this, we need to check if it is the same developer
project + task relationship strengthens correlation
Jira + GitHub relationship strengthens correlation


EvidenceEvent represents one individual piece of evidence

e.g.
GitHub commit
09:10
"Implement JWT validation"

is one EvidenceEvent

Jira issue update
08:30
"Add JWT tests"

is another EvidenceEvent

so the system would sees it as individual events.

*/

package timesheets.evidence.correlation;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import timesheets.evidence.EvidenceEvent;

@Service
public class EvidenceCorrelationService {
  // this is for temporal correlation - max time
  private static final long MAX_SESSION_GAP_MINUTES = 120;
  private static final double MIN_CORRELATION_THRESHOLD = 0.40;

  // create a lists of evidene events for a specific member into clustered evidence groups
  public List<EvidenceGroup> correlate(UUID workspaceMemberId, List<EvidenceEvent> evidenceEvents) {
    List<EvidenceGroup> groups = new ArrayList<EvidenceGroup>();

    if (evidenceEvents == null || evidenceEvents.isEmpty()) {
      return groups;
    }

    List<EvidenceEvent> sortedEvents = new ArrayList<EvidenceEvent>(evidenceEvents);

    sortedEvents.sort(
        Comparator.comparing(
            EvidenceEvent::getTimestamp, Comparator.nullsLast(Comparator.naturalOrder())));

    for (EvidenceEvent event : sortedEvents) {
      if (event == null) {
        continue;
      }

      // filer out events that do not belong to requested workspace member
      if (!workspaceMemberId.equals(event.getWorkspaceMemberId())) {
        continue;
      }

      // attempt to find the best existing group for this event based on weighted score
      EvidenceGroup matchingGroup = findMatchingGroup(event, groups);
      if (matchingGroup != null) {
        addEventToGroup(matchingGroup, event);
      } else {
        groups.add(createGroup(event));
      }
    }

    return groups;
  }

  // find the highest scoring existing group (best group )
  private EvidenceGroup findMatchingGroup(EvidenceEvent event, List<EvidenceGroup> groups) {
    if (groups.isEmpty() || event.getTimestamp() == null) {
      return null;
    }

    EvidenceGroup bestGroup = null;
    double maxScore = -1.0;

    for (EvidenceGroup group : groups) {
      if (group.getEndTime() == null) {
        continue;
      }

      long gapMinutes = Duration.between(group.getEndTime(), event.getTimestamp()).toMinutes();

      // 30 MIN MAX!!!
      if (gapMinutes > MAX_SESSION_GAP_MINUTES) {
        continue;
      }

      double score = calculateCorrelationScore(event, group);
      if (score >= MIN_CORRELATION_THRESHOLD && score > maxScore) {
        maxScore = score;
        bestGroup = group;
      }
    }

    return bestGroup;
  }

  // calculates a weighted correlation score between an event and an existing group
  /*
    temporal closeness (50%)
    shared project (30%)
    shared metadata details like Jira and GitHub repos (20%)
  */

  // the 30minute is LIMIT despite the correlation score otherwise it will include events from
  // different days
  private double calculateCorrelationScore(EvidenceEvent event, EvidenceGroup group) {
    if (group.getStartTime() == null
        || group.getEndTime() == null
        || event.getTimestamp() == null) {
      return 0.0;
    }

    LocalDateTime eventTime = event.getTimestamp();
    LocalDateTime groupEnd = group.getEndTime();
    long gapMinutes = Duration.between(groupEnd, eventTime).toMinutes();

    if (gapMinutes > MAX_SESSION_GAP_MINUTES) {
      return 0.0;
    }

    double temporalScore;

    if (gapMinutes <= 0) {
      temporalScore = 1.0;
    } else {
      temporalScore = 1.0 - ((double) gapMinutes / MAX_SESSION_GAP_MINUTES);
    }

    double projectScore = projectMatchedScore(event, group);

    double contextScore = contextMatchScore(event, group);

    return (temporalScore * 0.50) + (projectScore * 0.30) + (contextScore * 0.20);
  }

  //  check if there is shared project id with any event in the group

  private double projectMatchedScore(EvidenceEvent event, EvidenceGroup group) {
    if (event.getProjectId() == null) {
      return 0.0;
    }

    for (EvidenceEvent existingEvent : group.getEvidenceEvents()) {
      if (event.getProjectId().equals(existingEvent.getProjectId())) {
        return 1.0;
      }
    }

    return 0.0;
  }

  // check for similar metadata info
  private double contextMatchScore(EvidenceEvent event, EvidenceGroup group) {
    for (EvidenceEvent existingEvent : group.getEvidenceEvents()) {
      if (hasMatchingIssueKey(event, existingEvent)) {
        return 1.0;
      }
      if (hasMatchingRepository(event, existingEvent)) {
        return 0.8;
      }
    }
    return 0.0;
  }

  private boolean hasMatchingIssueKey(EvidenceEvent first, EvidenceEvent second) {
    String firstIssueKey = getMetadataString(first, "issueKey");
    String secondIssueKey = getMetadataString(second, "issueKey");

    if (firstIssueKey == null || secondIssueKey == null) {
      return false;
    }

    return firstIssueKey.equalsIgnoreCase(secondIssueKey);
  }

  private boolean hasMatchingRepository(EvidenceEvent first, EvidenceEvent second) {
    String firstRepository = getMetadataString(first, "repositoryName");
    String secondRepository = getMetadataString(second, "repositoryName");

    if (firstRepository == null || secondRepository == null) {
      return false;
    }

    return firstRepository.equalsIgnoreCase(secondRepository);
  }

  private String getMetadataString(EvidenceEvent event, String key) {
    if (event.getMetadata() == null) {
      return null;
    }

    Object value = event.getMetadata().get(key);
    if (value == null) {
      return null;
    }

    return value.toString();
  }

  private EvidenceGroup createGroup(EvidenceEvent event) {
    EvidenceGroup group = new EvidenceGroup();
    group.setId(UUID.randomUUID());
    group.setWorkspaceMemberId(event.getWorkspaceMemberId());
    group.setStartTime(event.getTimestamp());
    group.setEndTime(getEventEndTime(event));
    group.setCorrelationScore(1.0);
    group.getEvidenceEvents().add(event);

    return group;
  }

  private void addEventToGroup(EvidenceGroup group, EvidenceEvent event) {
    double eventScore = calculateCorrelationScore(event, group);

    group.getEvidenceEvents().add(event);

    if (event.getTimestamp() != null
        && (group.getStartTime() == null || event.getTimestamp().isBefore(group.getStartTime()))) {
      group.setStartTime(event.getTimestamp());
    }

    LocalDateTime eventEndTime = getEventEndTime(event);

    if (eventEndTime != null
        && (group.getEndTime() == null || eventEndTime.isAfter(group.getEndTime()))) {
      group.setEndTime(eventEndTime);
    }

    if (eventScore > group.getCorrelationScore()) {
      group.setCorrelationScore(eventScore);
    }
  }

  private LocalDateTime getEventEndTime(EvidenceEvent event) {
    if (event.getEndTime() != null) {
      return event.getEndTime();
    }

    return event.getTimestamp();
  }
}
