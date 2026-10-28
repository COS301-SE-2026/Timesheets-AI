package timesheets.managerassistant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import timesheets.domain.Timesheet;
import timesheets.evidence.EvidenceEvent;
import timesheets.evidence.JiraEvidenceCollector;
import timesheets.integration.calendar.CalendarService;
import timesheets.integration.github.GitHubAdapter;
import timesheets.integration.github.GitHubService;
import timesheets.repository.TimeEntryRepository;

// the jira part of the score now reads tickets through JiraEvidenceCollector, the same collector
// the project forecast uses, so these tests pin down what the manager sees for the JIRA row
@ExtendWith(MockitoExtension.class)
class ManagerAssistantScoringServiceTest {

  @Mock private GitHubAdapter gitHubAdapter;
  @Mock private CalendarService calendarService;
  @Mock private JiraEvidenceCollector jiraEvidenceCollector;
  @Mock private TimeEntryRepository timeEntryRepository;
  @Mock private GitHubService gitHubService;

  @InjectMocks private ManagerAssistantScoringService scoringService;

  private Timesheet timesheetFor(UUID memberId) {
    return Timesheet.builder()
        .id(UUID.randomUUID())
        .workspaceMemberId(memberId)
        .periodStart(LocalDate.of(2026, 9, 14))
        .periodEnd(LocalDate.of(2026, 9, 20))
        .build();
  }

  private EvidenceEvent issueEvent(UUID memberId, String issueKey) {
    EvidenceEvent event = new EvidenceEvent();
    event.setSource("JIRA");
    event.setActivityType("ISSUE");
    event.setWorkspaceMemberId(memberId);
    event.setMetadata(Map.of("issueKey", issueKey));
    return event;
  }

  private ManagerAssistantEvidenceResult jiraRow(ManagerAssistantReview review) {
    return review.evidenceSources().stream()
        .filter(result -> "JIRA".equals(result.sourceName()))
        .findFirst()
        .orElseThrow();
  }

  @Test
  void scoreTimesheet_countsDistinctJiraTicketsFromTheCollector() {
    UUID memberId = UUID.randomUUID();
    when(jiraEvidenceCollector.collectIssueEvidence(eq(memberId), any(), any()))
        .thenReturn(
            List.of(
                issueEvent(memberId, "MOM-1"),
                issueEvent(memberId, "MOM-1"),
                issueEvent(memberId, "MOM-2")));

    ManagerAssistantReview review = scoringService.scoreTimesheet(timesheetFor(memberId));

    ManagerAssistantEvidenceResult jira = jiraRow(review);
    assertThat(jira.matchCount()).isEqualTo(2);
    assertThat(jira.subScore()).isEqualTo(1.0);
    assertThat(jira.detail()).contains("2 tickets");
  }

  @Test
  void scoreTimesheet_scoresJiraZeroWhenNoTicketsTouchedInThePeriod() {
    UUID memberId = UUID.randomUUID();
    when(jiraEvidenceCollector.collectIssueEvidence(eq(memberId), any(), any()))
        .thenReturn(List.of());

    ManagerAssistantReview review = scoringService.scoreTimesheet(timesheetFor(memberId));

    ManagerAssistantEvidenceResult jira = jiraRow(review);
    assertThat(jira.matchCount()).isZero();
    assertThat(jira.subScore()).isZero();
  }

  @Test
  void scoreTimesheet_treatsJiraAsNotConnectedWhenTheCollectorThrows() {
    UUID memberId = UUID.randomUUID();
    when(jiraEvidenceCollector.collectIssueEvidence(eq(memberId), any(), any()))
        .thenThrow(new RuntimeException("Jira is not connected."));

    ManagerAssistantReview review = scoringService.scoreTimesheet(timesheetFor(memberId));

    ManagerAssistantEvidenceResult jira = jiraRow(review);
    assertThat(jira.matchCount()).isZero();
    assertThat(jira.detail()).isEqualTo("jira not connected");
  }
}
