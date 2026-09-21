package timesheets.evidence;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/*
this file gets all GitHubEvidence, JiraEvidenceCollector, CalendarEvideceCollector
*/
@Slf4j
@Service
@RequiredArgsConstructor
public class EvidenceService {
  private final List<EvidenceCollector> evidenceCollectors;

  public List<EvidenceEvent> collect(
      UUID workspaceMemberId, LocalDateTime startTime, LocalDateTime endTime) {
    List<EvidenceEvent> evidenceEvents = new ArrayList<EvidenceEvent>();

    for (EvidenceCollector collector : evidenceCollectors) {
      List<EvidenceEvent> collectorEvents =
          collector.collect(workspaceMemberId, startTime, endTime);

      if (collectorEvents != null) {
        evidenceEvents.addAll(collectorEvents);
      }
    }

    evidenceEvents.sort(
        Comparator.comparing(
            EvidenceEvent::getTimestamp, Comparator.nullsLast(Comparator.naturalOrder())));

    log.info("Evidence collection completed. Total events:", evidenceEvents.size());
    return evidenceEvents;
  }
}
