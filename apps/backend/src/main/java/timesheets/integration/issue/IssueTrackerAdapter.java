package timesheets.integration.issue;

import java.util.List;
import java.util.UUID;
import timesheets.dto.IssueDto;

public interface IssueTrackerAdapter {

  String getProvider();

  List<IssueDto> getIssues(UUID workspaceMemberId);

  IssueDto getIssue(UUID workspaceMemberId, String issueKey);
}
