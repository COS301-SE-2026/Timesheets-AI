package timesheets.evidence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import timesheets.dto.response.CommentResponse;
import timesheets.dto.response.IssueResponse;
import timesheets.dto.response.StatusChangeResponse;
import timesheets.dto.response.WorklogResponse;
import timesheets.integration.issue.IssueTrackerAdapter;

class JiraEvidenceCollectorTest {

  @Test
  void shouldCollectAllJiraEvidenceTypes() {

    IssueTrackerAdapter adapter = Mockito.mock(IssueTrackerAdapter.class);
    JiraEvidenceCollector collector = new JiraEvidenceCollector(adapter);

    UUID workspaceMemberId = UUID.randomUUID();
    LocalDateTime startTime = LocalDateTime.of(2026, 9, 20, 0, 0);
    LocalDateTime endTime = LocalDateTime.of(2026, 9, 20, 23, 59);

    IssueResponse issue = new IssueResponse();
    issue.setKey("MOM-123");
    issue.setTitle("Implement evidence engine");
    issue.setStatus("In Progress");
    issue.setIssueType("Task");
    issue.setProjectKey("MOM");
    issue.setProjectName("Momently");
    issue.setCreatedAt("2026-09-20T09:00:00.000+0200");

    WorklogResponse worklog = new WorklogResponse();
    worklog.setIssueKey("MOM-123");
    worklog.setWorklogId("10001");
    worklog.setStartedAt(LocalDateTime.of(2026, 9, 20, 10, 0));
    worklog.setTimeSpentSeconds(3600);
    worklog.setDescription("Worked on evidence engine");

    CommentResponse comment = new CommentResponse();
    comment.setIssueKey("MOM-123");
    comment.setCommentId("20001");
    comment.setCreatedAt(LocalDateTime.of(2026, 9, 20, 11, 0));
    comment.setBody("Evidence collection is working");

    StatusChangeResponse statusChange = new StatusChangeResponse();
    statusChange.setIssueKey("MOM-123");
    statusChange.setChangeLogId("30001");
    statusChange.setChangedAt(LocalDateTime.of(2026, 9, 20, 12, 0));
    statusChange.setFromStatus("To Do");
    statusChange.setToStatus("In Progress");

    when(adapter.getIssues(workspaceMemberId, startTime, endTime)).thenReturn(Arrays.asList(issue));
    when(adapter.getWorklogs(workspaceMemberId, startTime, endTime))
        .thenReturn(Arrays.asList(worklog));
    when(adapter.getComments(workspaceMemberId, startTime, endTime))
        .thenReturn(Arrays.asList(comment));
    when(adapter.getStatusChanges(workspaceMemberId, startTime, endTime))
        .thenReturn(Arrays.asList(statusChange));

    List<EvidenceEvent> events = collector.collect(workspaceMemberId, startTime, endTime);

    assertEquals(4, events.size());
    assertTrue(events.stream().anyMatch(event -> "ISSUE".equals(event.getActivityType())));
    assertTrue(events.stream().anyMatch(event -> "WORKLOG".equals(event.getActivityType())));
    assertTrue(events.stream().anyMatch(event -> "COMMENT".equals(event.getActivityType())));
    assertTrue(events.stream().anyMatch(event -> "STATUS_CHANGE".equals(event.getActivityType())));
  }
}
