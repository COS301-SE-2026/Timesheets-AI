package timesheets.evidence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class EvidenceServiceTest {

  @Test
  void shouldCombineEvidenceFromAllCollectors() {

    EvidenceCollector githubCollector = new MockEvidenceCollector("GITHUB", "COMMIT");

    EvidenceCollector jiraCollector = new MockEvidenceCollector("JIRA", "ISSUE");

    EvidenceService evidenceService =
        new EvidenceService(Arrays.asList(githubCollector, jiraCollector));

    UUID workspaceMemberId = UUID.randomUUID();

    LocalDateTime startTime = LocalDateTime.of(2026, 9, 20, 0, 0);

    LocalDateTime endTime = LocalDateTime.of(2026, 9, 20, 23, 59);

    List<EvidenceEvent> evidenceEvents =
        evidenceService.collect(workspaceMemberId, startTime, endTime);

    assertNotNull(evidenceEvents);

    assertEquals(2, evidenceEvents.size());

    assertEquals("GITHUB", evidenceEvents.get(0).getSource());

    assertEquals("JIRA", evidenceEvents.get(1).getSource());
  }

  private static class MockEvidenceCollector implements EvidenceCollector {

    private final String source;

    private final String activityType;

    MockEvidenceCollector(String source, String activityType) {

      this.source = source;
      this.activityType = activityType;
    }

    @Override
    public List<EvidenceEvent> collect(
        UUID workspaceMemberId, LocalDateTime startTime, LocalDateTime endTime) {

      EvidenceEvent event = new EvidenceEvent();

      event.setId(UUID.randomUUID());

      event.setSource(source);

      event.setWorkspaceMemberId(workspaceMemberId);

      event.setActivityType(activityType);

      event.setTimestamp(LocalDateTime.of(2026, 9, 20, 10, 0));

      return Arrays.asList(event);
    }
  }

  @Test
  void shouldSortEvidenceByTimestamp() {

    EvidenceCollector collector =
        new EvidenceCollector() {

          @Override
          public List<EvidenceEvent> collect(
              UUID workspaceMemberId, LocalDateTime startTime, LocalDateTime endTime) {

            EvidenceEvent laterEvent = new EvidenceEvent();

            laterEvent.setId(UUID.randomUUID());

            laterEvent.setSource("JIRA");

            laterEvent.setActivityType("COMMENT");

            laterEvent.setTimestamp(LocalDateTime.of(2026, 9, 20, 15, 0));

            EvidenceEvent earlierEvent = new EvidenceEvent();

            earlierEvent.setId(UUID.randomUUID());

            earlierEvent.setSource("GITHUB");

            earlierEvent.setActivityType("COMMIT");

            earlierEvent.setTimestamp(LocalDateTime.of(2026, 9, 20, 9, 0));

            return Arrays.asList(laterEvent, earlierEvent);
          }
        };

    EvidenceService evidenceService = new EvidenceService(Arrays.asList(collector));

    List<EvidenceEvent> events =
        evidenceService.collect(
            UUID.randomUUID(),
            LocalDateTime.of(2026, 9, 20, 0, 0),
            LocalDateTime.of(2026, 9, 20, 23, 59));

    assertEquals(LocalDateTime.of(2026, 9, 20, 9, 0), events.get(0).getTimestamp());

    assertEquals(LocalDateTime.of(2026, 9, 20, 15, 0), events.get(1).getTimestamp());
  }
}
