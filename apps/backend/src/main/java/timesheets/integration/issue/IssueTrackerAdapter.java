package timesheets.integration.issue;

import java.util.List;
import java.util.UUID;
import timesheets.dto.request.CreateJiraIssueRequest;
import timesheets.dto.response.JiraIssueResponse;

public interface IssueTrackerAdapter {

  String getProvider();

  List<JiraIssueResponse> getIssues(UUID workspaceMemberId);

  JiraIssueResponse getIssue(UUID workspaceMemberId, String issueKey);

  // the write operations
  JiraIssueResponse createIssue(UUID workspaceMemberId, CreateJiraIssueRequest request);

  // the link operations
  void linkTaskToIssue(UUID workspaceMemberId, UUID taskId, String issueKey);
}
