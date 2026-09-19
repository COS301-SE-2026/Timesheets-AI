package timesheets.evidence;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import timesheets.dto.response.IssueResponse;
import timesheets.integration.issue.IssueTrackerAdapter;

@Component
@RequiredArgsConstructor
public class JiraEvidenceCollector implements EvidenceCollector {

  private final IssueTrackerAdapter issueTrackerAdapter;

  @Override
  public List<EvidenceEvent> collect(
      UUID workspaceMemberId, LocalDateTime startTime, LocalDateTime endTime) {

    List<IssueResponse> issues =
        issueTrackerAdapter.getIssues(workspaceMemberId, startTime, endTime);
    List<EvidenceEvent> evidenceEvents = new ArrayList<>();

    for (IssueResponse issue : issues) {
      EvidenceEvent evidenceEvent = new EvidenceEvent();
      evidenceEvent.setId(UUID.randomUUID());
      evidenceEvent.setSource("JIRA");
      evidenceEvent.setWorkspaceMemberId(workspaceMemberId);
      evidenceEvent.setActivityType("ISSUE");

      LocalDateTime timestamp = parseTimestamp(issue.getCreatedAt());
      evidenceEvent.setTimestamp(timestamp);
      evidenceEvent.setDescription(issue.getTitle());

      Map<String, Object> metadata = new HashMap<>();

      metadata.put("issueKey", issue.getKey());
      metadata.put("status", issue.getStatus());
      metadata.put("issueType", issue.getIssueType());
      metadata.put("projectKey", issue.getProjectKey());
      metadata.put("projectName", issue.getProjectName());
      metadata.put("description", issue.getDescription());
      metadata.put("priority", issue.getPriority());
      metadata.put("assigneeEmail", issue.getAssigneeEmail());
      metadata.put("assigneeDisplayName", issue.getAssigneeDisplayName());
      metadata.put("createdAt", issue.getCreatedAt());
      metadata.put("updatedAt", issue.getUpdatedAt());
      metadata.put("dueDate", issue.getDueDate());
      metadata.put("localTaskId", issue.getLocalTaskId());

      evidenceEvent.setMetadata(metadata);
      evidenceEvents.add(evidenceEvent);
    }

    return evidenceEvents;
  }

  private LocalDateTime parseTimestamp(String timestamp) {
    if (timestamp == null) {
      return null;
    }

    return LocalDateTime.parse(timestamp.substring(0, 19));
  }

  
}
