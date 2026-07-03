package timesheets.service;

// this integration test class verifies the functionality of the InsightsService's
// getInsightsSummary method,
// by mocking the TimeEntryRepository to return specific time entries for a user within a given date
// range, and asserting that the resulting InsightsSummaryResponse contains the correct total hours
// logged,
// total days logged, hours per project, hours per task, daily trend, and average hours per day,
// ensuring that the insights summary is accurately calculated based on the time entries retrieved
// from the repository.
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import timesheets.domain.TimeEntry;
import timesheets.domain.User;
import timesheets.dto.request.ProductivityReportRequest;
import timesheets.dto.response.InsightsSummaryResponse;
import timesheets.enums.TimeEntryStatus;
import timesheets.enums.UserStatus;
import timesheets.repository.TimeEntryRepository;

@ExtendWith(MockitoExtension.class)
class InsightsServiceTest {

  @Mock TimeEntryRepository timeEntryRepository;
  @InjectMocks InsightsService insightsService;

  private User user;
  private ProductivityReportRequest request;
  private UUID projectId;
  private UUID taskId;

  @BeforeEach
  void setUp() { // Initialize a test user and a sample request before each test case
    user =
        User.builder()
            .id(UUID.fromString("00000000-0000-0000-0000-000000000002"))
            .email("bob@momentum.co.za")
            .status(UserStatus.ACTIVE)
            .emailVerified(true)
            .loginAttempts(0)
            .build();

    request = new ProductivityReportRequest();
    request.setFrom(LocalDate.of(2026, 5, 1));
    request.setTo(LocalDate.of(2026, 5, 20));

    projectId = UUID.fromString("00000000-0000-0000-0004-000000000001");
    taskId = UUID.fromString("00000000-0000-0000-0006-000000000001");
  }

  private TimeEntry entry(int durationMinutes, LocalDateTime start) {
    // Helper method to create a TimeEntry with specified duration and start time, using fixed
    // project and task IDs
    TimeEntry e = new TimeEntry();
    e.setId(UUID.randomUUID());
    e.setWorkspaceMemberId(UUID.randomUUID());
    e.setProjectId(projectId);
    e.setTaskId(taskId);
    e.setStartTime(start);
    e.setEndTime(start.plusMinutes(durationMinutes));
    e.setDurationMinutes(durationMinutes);
    e.setStatus(TimeEntryStatus.SUBMITTED);
    return e;
  }

  @Test
  void getInsightsSummary_returnsZerosForNoEntries() {
    // this test checks that when the TimeEntryRepository returns an empty list of time entries for
    // a user within a specified date range,
    // the InsightsService's getInsightsSummary method returns an InsightsSummaryResponse with total
    // hours logged equal to 0.0,
    // total days logged equal to 0, and empty lists for hours
    when(timeEntryRepository.findByUserIdAndDateRange(any(), any(), any()))
        .thenReturn(Collections.emptyList());

    InsightsSummaryResponse response = insightsService.getInsightsSummary(request, user);

    assertThat(response.getTotalHoursLogged()).isEqualTo(0.0);
    assertThat(response.getTotalDaysLogged()).isEqualTo(0);
    assertThat(response.getHoursPerProject()).isEmpty();
    assertThat(response.getHoursPerTask()).isEmpty();
    assertThat(response.getDailyTrend()).isEmpty();
  }

  @Test
  void getInsightsSummary_correctlyTotalsHours() {
    LocalDateTime day1 = LocalDateTime.of(2026, 5, 14, 9, 0); // May 14, 2026, 9:00 AM
    LocalDateTime day2 = LocalDateTime.of(2026, 5, 15, 9, 0);

    when(timeEntryRepository.findByUserIdAndDateRange(any(), any(), any()))
        .thenReturn(
            List.of(entry(480, day1), entry(300, day2))); // 8 hours on day 1 and 5 hours on day 2

    InsightsSummaryResponse response = insightsService.getInsightsSummary(request, user);

    assertThat(response.getTotalHoursLogged()).isEqualTo(13.0);
    assertThat(response.getTotalDaysLogged()).isEqualTo(2);
  }

  @Test
  void getInsightsSummary_groupsByProject() {
    // this test checks that when the TimeEntryRepository returns multiple time entries for a user
    // that belong to the same project,
    // the InsightsService's getInsightsSummary method correctly groups the hours by project in the
    // resulting InsightsSummaryResponse,
    // by asserting that the hours per project list contains a single entry with the correct total
    // hours and entry count for that project, ensuring that the grouping logic in the service works
    // as
    LocalDateTime start = LocalDateTime.of(2026, 5, 14, 9, 0); // May 14, 2026, 9:00 AM
    when(timeEntryRepository.findByUserIdAndDateRange(any(), any(), any()))
        .thenReturn(List.of(entry(120, start), entry(60, start.plusHours(3))));

    InsightsSummaryResponse response = insightsService.getInsightsSummary(request, user);

    assertThat(response.getHoursPerProject()).hasSize(1);
    assertThat(response.getHoursPerProject().get(0).getHours()).isEqualTo(3.0);
    assertThat(response.getHoursPerProject().get(0).getEntryCount()).isEqualTo(2);
  }

  @Test
  void getInsightsSummary_groupsByTask() {
    // this test checks that when the TimeEntryRepository returns multiple time entries for a user
    // that belong to the same task,
    // the InsightsService's getInsightsSummary method correctly groups the hours by task in the
    // resulting InsightsSummaryResponse,
    // by asserting that the hours per task list contains a single entry with the
    // correct total hours and task ID for that task, ensuring that the grouping logic in the
    // service works as expected for tasks
    LocalDateTime start = LocalDateTime.of(2026, 5, 14, 9, 0);
    when(timeEntryRepository.findByUserIdAndDateRange(any(), any(), any()))
        .thenReturn(List.of(entry(120, start)));

    InsightsSummaryResponse response = insightsService.getInsightsSummary(request, user);

    assertThat(response.getHoursPerTask()).hasSize(1);
    assertThat(response.getHoursPerTask().get(0).getTaskId()).isEqualTo(taskId);
    assertThat(response.getHoursPerTask().get(0).getHours()).isEqualTo(2.0);
  }

  @Test
  void getInsightsSummary_dailyTrendSortedByDate() {
    // this test checks that when the TimeEntryRepository returns multiple time entries for a user
    // on different dates,
    // the InsightsService's getInsightsSummary method correctly calculates the daily trend and
    // sorts it by
    // date in ascending order in the resulting InsightsSummaryResponse,
    // by asserting that the daily trend list contains entries with the correct dates in the
    // expected order,
    // ensuring that the date sorting logic in the service works as intended for the daily trend
    // calculation
    LocalDateTime day1 = LocalDateTime.of(2026, 5, 15, 9, 0);
    LocalDateTime day2 = LocalDateTime.of(2026, 5, 14, 9, 0);

    when(timeEntryRepository.findByUserIdAndDateRange(any(), any(), any()))
        .thenReturn(List.of(entry(60, day1), entry(60, day2)));

    InsightsSummaryResponse response = insightsService.getInsightsSummary(request, user);

    List<InsightsSummaryResponse.DailyTrend> trend = response.getDailyTrend();
    assertThat(trend.get(0).getDate()).isEqualTo("2026-05-14");
    assertThat(trend.get(1).getDate()).isEqualTo("2026-05-15");
  }

  @Test
  void getInsightsSummary_calculatesAverageHoursPerDay() {
    // this test checks that when the TimeEntryRepository returns time entries for a user that span
    // multiple days,
    // the InsightsService's getInsightsSummary method correctly calculates the average hours per
    // day in the
    // resulting InsightsSummaryResponse, by asserting that the average hours per day is equal to
    // the total hours logged divided by the total days logged,
    // ensuring that the average hours per day calculation in the service is accurate based on the
    // time entries retrieved from the repository

    LocalDateTime start = LocalDateTime.of(2026, 5, 14, 9, 0);
    when(timeEntryRepository.findByUserIdAndDateRange(any(), any(), any()))
        .thenReturn(List.of(entry(600, start))); // 10 hours

    InsightsSummaryResponse response = insightsService.getInsightsSummary(request, user);

    // 10 hours over 20 days = 0.5 avg
    assertThat(response.getAverageHoursPerDay()).isEqualTo(0.5);
  }

  @Test
  void getInsightsSummary_skipsEntriesWithNullTaskId() {
    // this test checks that when the TimeEntryRepository returns a time entry for a user that has a
    // null task ID,
    // the InsightsService's getInsightsSummary method correctly skips that entry when calculating
    // hours per task
    // in the resulting InsightsSummaryResponse, by asserting that the hours per task list does not
    // contain an entry for the null task ID,
    // while still including the hours for that entry in the total hours logged and hours per
    // project calculations,
    // ensuring that the service properly handles time entries with null task IDs without causing
    // errors
    // Create a time entry with a null task ID
    LocalDateTime start = LocalDateTime.of(2026, 5, 14, 9, 0);
    TimeEntry noTask = entry(60, start);
    noTask.setTaskId(null);

    when(timeEntryRepository.findByUserIdAndDateRange(any(), any(), any()))
        .thenReturn(List.of(noTask));

    InsightsSummaryResponse response = insightsService.getInsightsSummary(request, user);

    assertThat(response.getHoursPerTask()).isEmpty();
    assertThat(response.getHoursPerProject()).hasSize(1);
  }
}

//   @Test
//   void getInsightsSummary_passesCorrectDateRangeToRepository() {
//     // this test checks that when the InsightsService's getInsightsSummary method
//     // is called with a ProductivityReportRequest containing a specific date range,
//     // the method correctly converts the LocalDate range to LocalDateTime range
//     // with the appropriate start and end times, and calls the TimeEntryRepository's
//     // findByUserIdAndDateRange method with
//     // the correct user ID and date range parameters, ensuring that the service properly
//     // translates the request's date range into the format expected by the repository when
//     // retrieving time entries
//     when(timeEntryRepository.findByUserIdAndDateRange(any(), any(), any()))
//         .thenReturn(Collections.emptyList());

//     insightsService.getInsightsSummary(request, user);

//     verify(timeEntryRepository)
//         .findByUserIdAndDateRange(
//             eq(user.getId()),
//             eq(LocalDateTime.of(2026, 5, 1, 0, 0, 0)),
//             eq(LocalDateTime.of(2026, 5, 20, 23, 59, 59)));
//   }
