package timesheets.evidence;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service 
@RequiredArgsConstructor
@Slf4j
public class EvidenceService {
    private final List<EvidenceCollector> evidenceCollectors;

    public List<EvidenceEvent> collect(UUID workspaceMemberId, LocalDateTime startTime, LocalDateTime endTime){
        List<EvidenceEvent> evidenceEvents = new ArrayList<EvidenceEvent>();

        for ( EvidenceCollector collector : evidenceCollectors){
            try {
                List<EvidenceEVent> collectorEvents = collector.collect(workspaceMemberId, startTime, endTime);

                if (collectorEvents !=  null){
                    evidenceEvents.addAll(collectorEvents);
                }
            } catch (Exception e){
                log.error("Failed to collect evidence using", collector.getClass().getSimpleName(), exception);
            }
        }

        evidenceEvents.sort(Comparator.comparing(EvidenceEvent::getTimestamp, Comparator.nullsLast(Comparator.naturalOrder())));

        return evidenceEvents;
    }
}