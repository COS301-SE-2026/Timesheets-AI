package timesheets.evidence;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import lombok.Data;

/* 
The Evidence Engine is a translation and layer between your integrations and AI features

We have different sources of information: GitHub, Jira, Calendar and Timesheets which gives data in a different format 
Github gives commits, Jira gives issues and updates, Calendar gives meetings, Timesheets gives recorded work

Purpose:
Represent one piece of work-related evidence in a standard format.

GitHub would use timestamp for commit time
Calendar stil need endTime so we will put it in metadata

*/

@Data
public class EvidenceEvent {
    private UUID id;
    private String source;
    private LocalDateTime timestamp; 
    private String projectId;
    private UUID workspaceMemberId;
    private String taskId;
    private String activityType;
    private String description;
    private LocalDateTime endTime;
    private Map<String, Object> metadata;
}