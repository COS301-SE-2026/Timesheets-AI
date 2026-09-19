package timesheets.integration.issue;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import timesheets.dto.request.CreateIssueRequest;
import timesheets.dto.response.IssueResponse;

public interface IssueTrackerAdapter {

  String getProvider();

  List<IssueResponse> getIssues(UUID workspaceMemberId);

  IssueResponse getIssue(UUID workspaceMemberId, String issueKey);

  // getting issue within time window
  List<IssueResponse> getIssues(
      UUID workspaceMemberId, LocalDateTime startTime, LocalDateTime endTime);

  // the write operations
  IssueResponse createIssue(UUID workspaceMemberId, CreateIssueRequest request);

  // the link operations
  void linkTaskToIssue(UUID workspaceMemberId, UUID taskId, String issueKey);
}
