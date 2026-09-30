// package timesheets.evidence.correlation;

// import java.time.Duration;
// import java.time.LocalDateTime;
// import java.util.ArrayList;
// import java.util.Comparator;
// import java.util.List;
// import java.util.UUID;
// import org.springframework.stereotype.Service;
// import timesheets.evidence.EvidenceEvent;

// @Service
// public class EvidenceCorrelationServiceTest {

//   private static final long MAX_SESSION_GAP_MINUTES = 30;

//   public List<EvidenceGroup> correlate(UUID workspaceMemberId, List<EvidenceEvent>
// evidenceEvents) {

//     List<EvidenceGroup> groups = new ArrayList<EvidenceGroup>();

//     if (evidenceEvents == null || evidenceEvents.isEmpty()) {
//       return groups;
//     }

//     List<EvidenceEvent> sortedEvents = new ArrayList<EvidenceEvent>(evidenceEvents);

//     sortedEvents.sort(
//         Comparator.comparing(
//             EvidenceEvent::getTimestamp, Comparator.nullsLast(Comparator.naturalOrder())));

//     for (EvidenceEvent event : sortedEvents) {
//       if (event == null) {
//         continue;
//       }

//       if (!workspaceMemberId.equals(event.getWorkspaceMemberId())) {
//         continue;
//       }

//       EvidenceGroup matchingGroup = findMatchingGroup(event, groups);

//       if (matchingGroup != null) {
//         addEventToGroup(matchingGroup, event);
//       } else {
//         groups.add(createGroup(event));
//       }
//     }

//     return groups;
//   }

//   private EvidenceGroup findMatchingGroup(EvidenceEvent event, List<EvidenceGroup> groups) {

//     if (groups.isEmpty()) {
//       return null;
//     }

//     EvidenceGroup latestGroup = groups.get(groups.size() - 1);

//     if (event.getTimestamp() == null || latestGroup.getEndTime() == null) {
//       return null;
//     }

//     LocalDateTime eventStart = event.getTimestamp();
//     LocalDateTime groupEnd = latestGroup.getEndTime();

//     if (!eventStart.isAfter(groupEnd)) {
//       return latestGroup;
//     }

//     long gapMinutes = Duration.between(groupEnd, eventStart).toMinutes();

//     if (gapMinutes <= MAX_SESSION_GAP_MINUTES) {
//       return latestGroup;
//     }

//     return null;
//   }

//   private double calculateCorrelationScore(EvidenceEvent event, EvidenceGroup group) {

//     double projectScore = projectMatchedScore(event, group);
//     double contextScore = contextMatchScore(event, group);

//     return (projectScore * 0.60) + (contextScore * 0.40);
//   }

//   private double projectMatchedScore(EvidenceEvent event, EvidenceGroup group) {

//     if (event.getProjectId() == null) {
//       return 0.0;
//     }

//     for (EvidenceEvent existingEvent : group.getEvidenceEvents()) {
//       if (event.getProjectId().equals(existingEvent.getProjectId())) {
//         return 1.0;
//       }
//     }

//     return 0.0;
//   }

//   private double contextMatchScore(EvidenceEvent event, EvidenceGroup group) {

//     for (EvidenceEvent existingEvent : group.getEvidenceEvents()) {
//       if (hasMatchingIssueKey(event, existingEvent)) {
//         return 1.0;
//       }

//       if (hasMatchingRepository(event, existingEvent)) {
//         return 0.8;
//       }
//     }

//     return 0.0;
//   }

//   private boolean hasMatchingIssueKey(EvidenceEvent first, EvidenceEvent second) {

//     String firstIssueKey = getMetadataString(first, "issueKey");
//     String secondIssueKey = getMetadataString(second, "issueKey");

//     if (firstIssueKey == null || secondIssueKey == null) {
//       return false;
//     }

//     return firstIssueKey.equalsIgnoreCase(secondIssueKey);
//   }

//   private boolean hasMatchingRepository(EvidenceEvent first, EvidenceEvent second) {

//     String firstRepository = getMetadataString(first, "repositoryName");
//     String secondRepository = getMetadataString(second, "repositoryName");

//     if (firstRepository == null || secondRepository == null) {
//       return false;
//     }

//     return firstRepository.equalsIgnoreCase(secondRepository);
//   }

//   private String getMetadataString(EvidenceEvent event, String key) {

//     if (event.getMetadata() == null) {
//       return null;
//     }

//     Object value = event.getMetadata().get(key);

//     if (value == null) {
//       return null;
//     }

//     return value.toString();
//   }

//   private EvidenceGroup createGroup(EvidenceEvent event) {
//     EvidenceGroup group = new EvidenceGroup();

//     group.setId(UUID.randomUUID());
//     group.setWorkspaceMemberId(event.getWorkspaceMemberId());
//     group.setStartTime(event.getTimestamp());
//     group.setEndTime(getEventEndTime(event));
//     group.setCorrelationScore(0.0);
//     group.getEvidenceEvents().add(event);

//     return group;
//   }

//   private void addEventToGroup(EvidenceGroup group, EvidenceEvent event) {

//     double eventScore = calculateCorrelationScore(event, group);

//     group.getEvidenceEvents().add(event);

//     if (event.getTimestamp() != null
//         && (group.getStartTime() == null || event.getTimestamp().isBefore(group.getStartTime())))
// {
//       group.setStartTime(event.getTimestamp());
//     }

//     LocalDateTime eventEndTime = getEventEndTime(event);

//     if (eventEndTime != null
//         && (group.getEndTime() == null || eventEndTime.isAfter(group.getEndTime()))) {
//       group.setEndTime(eventEndTime);
//     }

//     if (eventScore > group.getCorrelationScore()) {
//       group.setCorrelationScore(eventScore);
//     }
//   }

//   private LocalDateTime getEventEndTime(EvidenceEvent event) {
//     if (event.getEndTime() != null) {
//       return event.getEndTime();
//     }

//     return event.getTimestamp();
//   }
// }
