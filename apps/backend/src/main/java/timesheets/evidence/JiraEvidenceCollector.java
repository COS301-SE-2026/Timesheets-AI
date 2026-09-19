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
import timesheets.dto.response.WorklogResponse;
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
    List<EvidenceEvent> evidenceEvents = new ArrayList<EvidenceEvent>();

    for (IssueResponse issue : issues) {
      EvidenceEvent evidenceEvent = new EvidenceEvent();
      evidenceEvent.setId(UUID.randomUUID());
      evidenceEvent.setSource("JIRA");
      evidenceEvent.setWorkspaceMemberId(workspaceMemberId);
      evidenceEvent.setActivityType("ISSUE");

      LocalDateTime timestamp = parseTimestamp(issue.getCreatedAt());
      evidenceEvent.setTimestamp(timestamp);
      evidenceEvent.setDescription(issue.getTitle());

      Map<String, Object> metadata = new HashMap<String, Object>();

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

    // collect Jira worklog evidence
    evidenceEvents.addAll(collectWorklogEvidence(workspaceMemberId, startTime, endTime));

    return evidenceEvents;
  }

  private LocalDateTime parseTimestamp(String timestamp) {
    if (timestamp == null) {
      return null;
    }

    return LocalDateTime.parse(timestamp.substring(0, 19));
  }

  private List<EvidenceEvent> collectWorklogEvidence(
      UUID workspaceMemberId, LocalDateTime startTime, LocalDateTime endTime) {
    List<WorklogResponse> worklogs =
        issueTrackerAdapter.getWorklogs(workspaceMemberId, startTime, endTime);

    List<EvidenceEvent> evidenceEvents = new ArrayList<EvidenceEvent>();

    for (WorklogResponse worklog : worklogs) {
      EvidenceEvent evidenceEvent = new EvidenceEvent();

      evidenceEvent.setId(UUID.randomUUID());
      evidenceEvent.setSource("JIRA");
      evidenceEvent.setWorkspaceMemberId(workspaceMemberId);
      evidenceEvent.setTimestamp(worklog.getStartedAt());
      evidenceEvent.setActivityType("WORKLOG");
      evidenceEvent.setDescription(worklog.getDescription());

      Map<String, Object> metadata = new HashMap<String, Object>();
      metadata.put("issueKey", worklog.getIssueKey());
      metadata.put("worklogId", worklog.getWorklogId());
      metadata.put("authorDisplayName", worklog.getAuthorDisplayName());
      metadata.put("authorEmail", worklog.getAuthorEmail());
      metadata.put("startedAt", worklog.getStartedAt());
      metadata.put("timeSpentSeconds", worklog.getTimeSpentSeconds());
      metadata.put("timeSpentMinutes", worklog.getTimeSpentSeconds() / 60);

      evidenceEvent.setMetadata(metadata);
      evidenceEvents.add(evidenceEvent);
    }

    return evidenceEvents;
  }
}
