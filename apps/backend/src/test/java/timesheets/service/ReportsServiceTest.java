// package timesheets.service;

// // integration tests for the ReportsService to verify that it correctly
// // generates productivity reports based on time entries for a user within a specified date range
// // ensuring that the report summary and breakdowns are accurate and correctly calculated based
// // on the time entries retrieved from the TimeEntryRepository, and that the report period and
// // generation timestamp are set correctly

// import static org.assertj.core.api.Assertions.*;
// import static org.mockito.ArgumentMatchers.*;
// import static org.mockito.Mockito.*;

// import java.time.LocalDate;
// import java.time.LocalDateTime;
// import java.util.*;
// import org.junit.jupiter.api.BeforeEach;
// import org.junit.jupiter.api.Test;
// import org.junit.jupiter.api.extension.ExtendWith;
// import org.mockito.*;
// import org.mockito.junit.jupiter.MockitoExtension;
// import timesheets.domain.TimeEntry;
// import timesheets.domain.User;
// import timesheets.dto.request.ProductivityReportRequest;
// import timesheets.dto.response.ProductivityReportResponse;
// import timesheets.enums.TimeEntryStatus;
// import timesheets.enums.UserStatus;
// import timesheets.repository.TimeEntryRepository;

// @ExtendWith(MockitoExtension.class)
// class ReportsServiceTest {

//   @Mock TimeEntryRepository timeEntryRepository;
//   // mocking the TimeEntryRepository to control the data returned during tests,
//   // allowing us to simulate different scenarios and verify that the ReportsService processes
//   // the time entries correctly when generating productivity reports

//   @InjectMocks ReportsService reportsService;

//   private User user;
//   private ProductivityReportRequest request;
//   private UUID projectId;
//   private UUID taskId1;
//   private UUID taskId2;

//   @BeforeEach
//   void setUp() {
//     user =
//         User.builder() // builds sample user with necessary fields for report generation
//             .id(UUID.fromString("00000000-0000-0000-0000-000000000002"))
//             .email("bob@momentum.co.za")
//             .status(UserStatus.ACTIVE)
//             .emailVerified(true)
//             .loginAttempts(0)
//             .build();

//     request = new ProductivityReportRequest();
//     request.setFrom(LocalDate.of(2026, 5, 1));
//     request.setTo(LocalDate.of(2026, 5, 20));

//     projectId = UUID.fromString("00000000-0000-0000-0004-000000000001");
//     taskId1 = UUID.fromString("00000000-0000-0000-0006-000000000001");
//     taskId2 = UUID.fromString("00000000-0000-0000-0006-000000000002");
//   }

//   private TimeEntry entry(UUID taskId, int durationMinutes, LocalDateTime start) {
//     // helper method to create TimeEntry objects with specified task ID, duration
//     // and start time, simplifying the creation of test data for the time entries used in the
//     // productivity report tests
//     TimeEntry e = new TimeEntry();
//     e.setId(UUID.randomUUID());
//     e.setProjectId(projectId);
//     e.setTaskId(taskId);
//     e.setStartTime(start);
//     e.setEndTime(start.plusMinutes(durationMinutes));
//     e.setDurationMinutes(durationMinutes);
//     e.setStatus(TimeEntryStatus.SUBMITTED);
//     return e;
//   }

//   @Test
//   void generateProductivityReport_returnsZeroSummaryForNoEntries() {
//     // this test checks the generateProductivityReport method to ensure that when there are no
// time
//     // entries for the user within the specified date range
//     // the generated report contains a summary with all metrics (total hours logged
//     // total entries logged, tasks worked on, projects worked on) equal to zero
//     // and that the breakdowns by task and by week are empty lists
//     // verifying that the service correctly handles cases with no data and does not produce
//     // erroneous summaries or breakdowns

//     when(timeEntryRepository.findByUserIdAndDateRange(any(), any(), any()))
//         .thenReturn(Collections.emptyList());

//     ProductivityReportResponse report = reportsService.generateProductivityReport(request, user);
//     // asserting that the report summary metrics are all zero and that the breakdowns are empty,
//     // confirming that the service correctly generates a report with no entries
//     assertThat(report.getSummary().getTotalHoursLogged()).isEqualTo(0.0);
//     assertThat(report.getSummary().getTotalEntriesLogged()).isEqualTo(0);
//     assertThat(report.getSummary().getTasksWorkedOn()).isEqualTo(0);
//     assertThat(report.getSummary().getProjectsWorkedOn()).isEqualTo(0);
//     assertThat(report.getByTask()).isEmpty();
//     assertThat(report.getByWeek()).isEmpty();
//   }

//   @Test
//   void generateProductivityReport_correctlySummarisesEntries() {
//     LocalDateTime day1 = LocalDateTime.of(2026, 5, 14, 9, 0);
//     LocalDateTime day2 = LocalDateTime.of(2026, 5, 15, 9, 0);

//     when(timeEntryRepository.findByUserIdAndDateRange(any(), any(), any()))
//         .thenReturn(List.of(entry(taskId1, 480, day1), entry(taskId2, 300, day2)));

//     ProductivityReportResponse report = reportsService.generateProductivityReport(request, user);
//     // asserting that the report summary metrics are correctly calculated based on the provided
// time
//     // entries, confirming that the service accurately summarizes the total hours logged, total
//     // entries, tasks worked on, and projects worked on
//     assertThat(report.getSummary().getTotalHoursLogged()).isEqualTo(13.0);
//     assertThat(report.getSummary().getTotalEntriesLogged()).isEqualTo(2);
//     assertThat(report.getSummary().getTasksWorkedOn()).isEqualTo(2);
//     assertThat(report.getSummary().getProjectsWorkedOn()).isEqualTo(1);
//   }

//   @Test
//   void generateProductivityReport_taskBreakdownSortedByHoursDescending() {
//     // this test checks the generateProductivityReport method to ensure that the breakdown of
// hours
//     // logged
//     // by task in the generated report is sorted in descending order based on the total hours
// logged
//     // for each task
//     LocalDateTime start = LocalDateTime.of(2026, 5, 14, 9, 0);
//     when(timeEntryRepository.findByUserIdAndDateRange(any(), any(), any()))
//         .thenReturn(List.of(entry(taskId1, 120, start), entry(taskId2, 480,
// start.plusHours(3))));

//     ProductivityReportResponse report = reportsService.generateProductivityReport(request, user);
//     // asserting that the task breakdown in the generated report is sorted in descending order
// based
//     // on hours logged, confirming that the service correctly orders the tasks in the breakdown
// to
//     // show the most time-consuming tasks first
//     assertThat(report.getByTask().get(0).getHoursLogged())
//         .isGreaterThan(report.getByTask().get(1).getHoursLogged());
//   }

//   @Test
//   void generateProductivityReport_weeklyBreakdownSortedChronologically() {
//     // this test checks the generateProductivityReport method to ensure that the breakdown of
// hours
//     // logged
//     // by week in the generated report is sorted in chronological order based on the week start
// date
//     LocalDateTime week20 = LocalDateTime.of(2026, 5, 14, 9, 0);
//     LocalDateTime week19 = LocalDateTime.of(2026, 5, 7, 9, 0);

//     when(timeEntryRepository.findByUserIdAndDateRange(any(), any(), any()))
//         .thenReturn(List.of(entry(taskId1, 60, week20), entry(taskId1, 60, week19)));

//     ProductivityReportResponse report = reportsService.generateProductivityReport(request, user);
//     // asserting that the weekly breakdown in the generated report is sorted in chronological
// order
//     // based on the week start date, confirming that the service correctly orders the weeks
//     // in the breakdown to show the earliest week first
//
// assertThat(report.getByWeek().get(0).getWeek()).isLessThan(report.getByWeek().get(1).getWeek());
//   }

//   @Test
//   void generateProductivityReport_excludesEntriesWithNullTaskFromTaskBreakdown() {
//     // this checks that entries with Null tasks are excluded
//     LocalDateTime start = LocalDateTime.of(2026, 5, 14, 9, 0);
//     TimeEntry noTask = entry(null, 60, start);

//     when(timeEntryRepository.findByUserIdAndDateRange(any(), any(), any()))
//         .thenReturn(List.of(noTask));

//     ProductivityReportResponse report = reportsService.generateProductivityReport(request, user);
//     // asserting that time entries with a null task ID are excluded from the task breakdown in
// the
//     // generated report
//     // confirming that the service correctly filters out entries without associated tasks when
//     // generating the breakdown by task
//     // while still including them in the overall summary metrics
//     assertThat(report.getByTask()).isEmpty();
//     assertThat(report.getSummary().getTotalEntriesLogged()).isEqualTo(1);
//   }

//   @Test
//   void generateProductivityReport_setPeriodCorrectly() {
//     // checks that the date range is correct
//     when(timeEntryRepository.findByUserIdAndDateRange(any(), any(), any()))
//         .thenReturn(Collections.emptyList());

//     ProductivityReportResponse report = reportsService.generateProductivityReport(request, user);
//     assertThat(report.getPeriod().getFrom()).isEqualTo(LocalDate.of(2026, 5, 1));
//     assertThat(report.getPeriod().getTo()).isEqualTo(LocalDate.of(2026, 5, 20));
//   }

//   @Test
//   void generateProductivityReport_generatedAtIsNotNull() {
//     // makes sure that the report isnt null
//     when(timeEntryRepository.findByUserIdAndDateRange(any(), any(), any()))
//         .thenReturn(Collections.emptyList());

//     ProductivityReportResponse report = reportsService.generateProductivityReport(request, user);

//     assertThat(report.getGeneratedAt()).isNotNull();
//   }
// }
