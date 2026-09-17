package timesheets.integration.issue;

import java.util.List;
import java.util.UUID;
import timesheets.dto.request.CreateJiraIssueRequest;
import timesheets.dto.response.JiraIssueResponse;

public interface IssueTrackerAdapter {

  String getProvider();

  List<JiraIssueResponse> getIssues(UUID workspaceMemberId);

  IssueResponse getIssue(UUID workspaceMemberId, String issueKey);

  // the write operations
  IssueResponse createIssue(UUID workspaceMemberId, CreateIssueRequest request);

  // the link operations
  void linkTaskToIssue(UUID workspaceMemberId, UUID taskId, String issueKey);
}
