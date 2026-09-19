package timesheets.integration.issue;

import java.util.List;
import java.util.UUID;
import timesheets.dto.request.CreateIssueRequest;
import timesheets.dto.response.IssueResponse;

public interface IssueTrackerAdapter {

  String getProvider();

  List<IssueResponse> getIssues(UUID workspaceMemberId);

  IssueResponse getIssue(UUID workspaceMemberId, String issueKey);

  // the write operations
  IssueResponse createIssue(UUID workspaceMemberId, CreateIssueRequest request);

  // the link operations
  void linkTaskToIssue(UUID workspaceMemberId, UUID taskId, String issueKey);
}
