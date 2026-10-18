// this is our coreelation model

package timesheets.evidence.correlation;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.Data;
import timesheets.evidence.EvidenceEvent;

/* if events will treated related if they occur within 60 minutes window but first we will check the workspaceMemberId*/
@Data
public class EvidenceGroup {
  private UUID id; // this identifies the particular group - correlated collection of evidence
  private UUID workspaceMemberId; // tells us which developer belong to the group
  private LocalDateTime startTime;
  private LocalDateTime endTime;
  private List<EvidenceEvent> evidenceEvents = new ArrayList<EvidenceEvent>();
  private double correlationScore;
}
