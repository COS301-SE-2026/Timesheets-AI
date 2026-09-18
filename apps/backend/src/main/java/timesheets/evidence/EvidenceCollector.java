package timesheets.evidence;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface EvidenceCollector {

  /* here is the base class for
      - GitHubEvidenceCollector - thinks of it as "Turn useful GitHub activity into EvidenceEvents"
      - JiraEvidenceCollector
      - CalendarEvidenceCollector

      Take information from one particular evidence source and turn it into a list of EvidenceEvent objects
  */

  // Collect the evidence availiable to this developer
  List<EvidenceEvent> collect(
      UUID workspaceMemberId, LocalDateTime startTime, LocalDateTime endTime);
}
