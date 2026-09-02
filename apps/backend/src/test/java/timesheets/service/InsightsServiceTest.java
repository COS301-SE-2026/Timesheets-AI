package timesheets.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import timesheets.domain.TimeEntry;
import timesheets.domain.User;
import timesheets.dto.request.ProductivityReportRequest;
import timesheets.dto.response.PersonalInsightsResponse;
import timesheets.enums.UserStatus;
import timesheets.repository.TimeEntryRepository;
import timesheets.security.SecurityUtils;

@ExtendWith(MockitoExtension.class)
class InsightsServiceTest {

  @Mock TimeEntryRepository timeEntryRepository;
  @Mock private SecurityUtils securityUtils;
  @InjectMocks InsightsService insightsService;

  private User user;
  private ProductivityReportRequest request;
  private UUID projectId;
  private UUID taskId;
  private UUID userId;

  // this will run before every test function
  @BeforeEach
  void setUp() {
    user =
        User.builder()
            .id(UUID.fromString("00000000-0000-0000-0000-000000000002"))
            .email("bob@momentum.co.za")
            .status(UserStatus.ACTIVE)
            .emailVerified(true)
            .build();

    // setting up the date range from 1st May to May 20
    request = new ProductivityReportRequest();
    request.setFrom(LocalDate.of(2026, 5, 1));
    request.setTo(LocalDate.of(2026, 5, 20));

    projectId = UUID.fromString("00000000-0000-0000-0004-000000000001");
    taskId = UUID.fromString("00000000-0000-0000-0006-000000000001");
  }

  // !helper functions
  private TimeEntry entry(int durationSeconds, LocalDateTime start) {
    // Helper method to create a TimeEntry with specified duration and start time, using fixed
    // project and task IDs
    TimeEntry e = new TimeEntry();
    e.setId(UUID.randomUUID());
    e.setWorkspaceMemberId(UUID.randomUUID());
    e.setProjectId(projectId);
    e.setTaskId(taskId);
    e.setStartTime(start);
    e.setEndTime(start.plusSeconds(durationSeconds));
    e.setDurationSeconds(durationSeconds);
    return e;
  }

  @Test
  @DisplayName("empty lists returned since no entries")
  void getInsightsSummary() {
    // ARRANGE: mocking the security utils to return the user
    when(securityUtils.getCurrentUserId()).thenReturn(userId);
    when(timeEntryRepository.findByUserIdAndDateRange(any(), any(), any()))
        .thenReturn(Collections.emptyList());

    // ACT: get insights summary
    PersonalInsightsResponse response = insightsService.getInsightsSummary(request);

    // ASSERT: expecting all the values to be empty, so it should handle these empty lists properly
    assertThat(response.getTotalHoursLogged()).isEqualTo(0.0);
    assertThat(response.getTotalDaysLogged()).isEqualTo(0);
    assertThat(response.getHoursPerProject()).isEmpty();
    assertThat(response.getHoursPerTask()).isEmpty();
    assertThat(response.getDailyTrend()).isEmpty();
  }

  @Test
  @DisplayName("returns correct total hours, from many entries")
  void getInsightsSummaryTotalHours() {
    // ARRANGE: Create entries for two different days
    LocalDateTime day1 = LocalDateTime.of(2026, 5, 14, 9, 0); // May 14 at 09:00
    LocalDateTime day2 = LocalDateTime.of(2026, 5, 15, 9, 0);

    when(securityUtils.getCurrentUserId()).thenReturn(userId);
    when(timeEntryRepository.findByUserIdAndDateRange(any(), any(), any()))
        .thenReturn(
            List.of(
                entry(28800, day1), entry(18000, day2))); // 8 hours on day 1 and 5 hours on day 2

    // ACT: Get insights summary
    PersonalInsightsResponse response = insightsService.getInsightsSummary(request);

    assertThat(response.getTotalHoursLogged()).isEqualTo(13.0);
    assertThat(response.getTotalDaysLogged()).isEqualTo(2);
  }

  @Test
  @DisplayName("groups hours by projects")
  void getInsightsSummaryProjectHours() {

    // ARRANGE: returning two entries of the same project and those should be grouped together
    LocalDateTime start = LocalDateTime.of(2026, 5, 14, 9, 0); // May 14 at 09:00
    when(timeEntryRepository.findByUserIdAndDateRange(any(), any(), any()))
        .thenReturn(List.of(entry(7200, start), entry(3600, start.plusHours(3))));

    // ACT: calling the insights summary function
    PersonalInsightsResponse response = insightsService.getInsightsSummary(request);

    // ASSERT: checking that the hours were grouped as they should be
    assertThat(response.getHoursPerProject()).hasSize(1);
    assertThat(response.getHoursPerProject().get(0).getHours()).isEqualTo(3.0);
    assertThat(response.getHoursPerProject().get(0).getEntryCount()).isEqualTo(2);
  }

  @Test
  @DisplayName("group hours by tasks")
  void getInsightsSummaryTaskHours() {
    // ARRANGE: creating the entries of a specific task
    LocalDateTime start = LocalDateTime.of(2026, 5, 14, 9, 0);
    when(timeEntryRepository.findByUserIdAndDateRange(any(), any(), any()))
        .thenReturn(List.of(entry(7200, start)));

    // ACT: calling the actual method I am testing
    PersonalInsightsResponse response = insightsService.getInsightsSummary(request);

    // ASSERT: checking that the hours are grouping as I should expect them
    assertThat(response.getHoursPerTask()).hasSize(1);
    assertThat(response.getHoursPerTask().get(0).getTaskId()).isEqualTo(taskId);
    assertThat(response.getHoursPerTask().get(0).getHours()).isEqualTo(2.0);
  }

  @Test
  @DisplayName("return the trend by the date")
  void getInsightsSummaryTrendByDate() {

    // ARRANGE: adding the entries out of order, want to see if they get sorted out
    LocalDateTime day1 = LocalDateTime.of(2026, 5, 15, 9, 0);
    LocalDateTime day2 = LocalDateTime.of(2026, 5, 14, 9, 0);

    when(timeEntryRepository.findByUserIdAndDateRange(any(), any(), any()))
        .thenReturn(List.of(entry(60, day1), entry(60, day2)));

    // ACT
    PersonalInsightsResponse response = insightsService.getInsightsSummary(request);

    // ASSERT:making sure that the dates are sorted as I expect them
    List<PersonalInsightsResponse.DailyTrend> trend = response.getDailyTrend();
    assertThat(trend.get(0).getDate()).isEqualTo("2026-05-14");
    assertThat(trend.get(1).getDate()).isEqualTo("2026-05-15");
  }

  @Test
  @DisplayName("calculates average hours per day")
  void getInsightsSummaryAvgDayHours() {
    // ARRANGE: creating the entries for one day
    LocalDateTime start = LocalDateTime.of(2026, 5, 14, 9, 0);
    when(timeEntryRepository.findByUserIdAndDateRange(any(), any(), any()))
        .thenReturn(List.of(entry(36000, start))); // 10 hours

    when(securityUtils.getCurrentUserId()).thenReturn(userId);

    // ACT
    PersonalInsightsResponse response = insightsService.getInsightsSummary(request);

    // ASSERT: 10 hours over 20 days
    assertThat(response.getAverageHoursPerDay()).isEqualTo(0.5);
  }

  @Test
  @DisplayName("skip null task ID entries")
  void getInsightsSummarySkipNullEntries() {
    // ARRANGE: creating an entry with a task ID thats null
    LocalDateTime start = LocalDateTime.of(2026, 5, 14, 9, 0);

    TimeEntry noTask = entry(60, start);
    noTask.setTaskId(null); // forcing the ID part to be null

    when(securityUtils.getCurrentUserId()).thenReturn(userId);
    when(timeEntryRepository.findByUserIdAndDateRange(any(), any(), any()))
        .thenReturn(List.of(noTask));

    // ACT
    PersonalInsightsResponse response = insightsService.getInsightsSummary(request);

    // ASSERT: we want the null entries to be excluded because they could be soft deleted
    assertThat(response.getHoursPerTask()).isEmpty();
    assertThat(response.getHoursPerProject()).hasSize(1);
  }

  @Test
  @DisplayName("correct date range sent to repo")
  void getInsightsSummaryCorrectDateRange() {
    // ARRANGE: mocking to check and mae sure that the date ranges will be as they should
    when(securityUtils.getCurrentUserId()).thenReturn(userId);
    when(timeEntryRepository.findByUserIdAndDateRange(any(), any(), any()))
        .thenReturn(Collections.emptyList());

    // ACT
    insightsService.getInsightsSummary(request);

    // ASSERT: making sure that the repo is called with the correct dates
    verify(timeEntryRepository)
        .findByUserIdAndDateRange(
            eq(userId),
            eq(LocalDateTime.of(2026, 5, 1, 0, 0, 0)),
            eq(LocalDateTime.of(2026, 5, 20, 23, 59, 59)));
  }
}
