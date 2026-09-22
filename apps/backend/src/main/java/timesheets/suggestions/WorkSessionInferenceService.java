package timesheets.suggestions;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import timesheets.evidence.EvidenceEvent;
import timesheets.evidence.correlation.EvidenceGroup;

@Service
public class WorkSessionInferenceService {
    public List<SuggestedWorkSession> inferWorkSession(List<EvidenceGroup> evidenceGroups){

        List<SuggestedWorkSession> sessions = new ArrayList<SuggestedWorkSession>();

        if ( evidenceGroups == null || evidenceGroups.isEmpty()){
            return sessions;
        }

        for (EvidenceGroup group : evidenceGroups){
            if (group == null || group.getEvidenceEvents() == null || group.getEvidenceEvents().isEmpty()){
                continue;
            }

            // create a new session method 
            SuggestedWorkSession session = createSession(group);
            sessions.add(session);
        }

        return sessions;
    }


}