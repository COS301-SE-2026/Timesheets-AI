package timesheets.service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import timesheets.client.AiServiceClient;
import timesheets.domain.TimeEntry;
import timesheets.dto.request.ProductivityReportRequest;
import timesheets.dto.response.AiDashboardResponse;
import timesheets.dto.response.PersonalInsightsResponse;
import timesheets.repository.TimeEntryRepository;
import timesheets.security.SecurityUtils;

// service for generating insights and analytics based on time entries,
// currently implements a summary report with various metrics and breakdowns
// these are currently for just the developer's own time entries, but could be extended to include
// team-level insights in the future after demo 1
@Service
@RequiredArgsConstructor
public class InsightsService {

  private final TimeEntryRepository timeEntryRepository;
  private final SecurityUtils securityUtils;
  private final AiServiceClient aiServiceClient;

  public PersonalInsightsResponse getInsightsSummary(ProductivityReportRequest request) {

    // get user ID from the security context
    UUID userId = securityUtils.getCurrentUserId();

    // fetch all time entries for the developer in the date range
    List<TimeEntry> entries =
        timeEntryRepository.findByUserIdAndDateRange(
            userId,
            request.getFrom().atStartOfDay(),
            request.getTo().atTime(23, 59, 59) // include entire end day
            );

    // total hours logged
    double totalHours =
        entries.stream().mapToDouble(entry -> entry.getDurationSeconds() / 3600.0).sum();

    // count unique days with entries
    long uniqueDays =
        entries.stream().map(entry -> entry.getStartTime().toLocalDate()).distinct().count();

    // average hours per day
    long daysBetween = ChronoUnit.DAYS.between(request.getFrom(), request.getTo()) + 1;
    double avgHoursPerDay = daysBetween > 0 ? totalHours / daysBetween : 0;

    // hours per day breakdown
    // Map<LocalDate, Double> hoursPerDay = entries.stream()
    // .collect(Collectors.groupingBy(
    //         entry -> entry.getStartTime().toLocalDate(),
    //         Collectors.summingDouble(entry -> entry.getDurationSeconds() / 60.0)
    // ));

    // hours per project
    List<PersonalInsightsResponse.ProjectHours> hoursPerProject =
        entries.stream().collect(Collectors.groupingBy(TimeEntry::getProjectId)).entrySet().stream()
            .map(
                entry -> {
                  double hours =
                      entry.getValue().stream()
                          .mapToDouble(e -> e.getDurationSeconds() / 3600.0)
                          .sum();

                  return PersonalInsightsResponse.ProjectHours.builder()
                      .projectId(entry.getKey())
                      .projectName("Project " + entry.getKey())
                      .hours(hours)
                      .entryCount(entry.getValue().size())
                      .build();
                })
            .sorted((a, b) -> Double.compare(b.getHours(), a.getHours()))
            .collect(Collectors.toList());

    // hours per task
    List<PersonalInsightsResponse.TaskHours> hoursPerTask =
        entries.stream()
            .filter(entry -> entry.getTaskId() != null)
            .collect(Collectors.groupingBy(TimeEntry::getTaskId))
            .entrySet()
            .stream()
            .map(
                entry -> {
                  double hours =
                      entry.getValue().stream()
                          .mapToDouble(e -> e.getDurationSeconds() / 3600.0)
                          .sum();

                  return PersonalInsightsResponse.TaskHours.builder()
                      .taskId(entry.getKey())
                      .taskTitle("Task " + entry.getKey())
                      .hours(hours)
                      .status("TODO")
                      .build();
                })
            .sorted((a, b) -> Double.compare(b.getHours(), a.getHours()))
            .limit(10)
            .collect(Collectors.toList());

    // daily trend breakdown for line chart, group by date and sum hours
    Map<LocalDate, List<TimeEntry>> dailyGroups =
        entries.stream()
            .collect(Collectors.groupingBy(entry -> entry.getStartTime().toLocalDate()));

    List<PersonalInsightsResponse.DailyTrend> dailyTrend =
        dailyGroups.entrySet().stream()
            .map(
                entry -> {
                  double hours =
                      entry.getValue().stream()
                          .mapToDouble(e -> e.getDurationSeconds() / 3600.0)
                          .sum();

                  return PersonalInsightsResponse.DailyTrend.builder()
                      .date(entry.getKey().format(DateTimeFormatter.ISO_DATE))
                      .hours(hours)
                      .entryCount(entry.getValue().size())
                      .build();
                })
            .sorted((a, b) -> a.getDate().compareTo(b.getDate()))
            .collect(Collectors.toList());
    // build and return response
    return PersonalInsightsResponse.builder()
        .totalHoursLogged(totalHours)
        .averageHoursPerDay(avgHoursPerDay)
        .totalDaysLogged((int) uniqueDays)
        // .hoursPerDay(hoursPerDay)
        .hoursPerProject(hoursPerProject)
        .hoursPerTask(hoursPerTask)
        .dailyTrend(dailyTrend)
        .build();
  }

  public AiDashboardResponse getAiDashboard() {
    UUID workspaceMemberId = securityUtils.getDefaultWorkspaceMemberId();
    return aiServiceClient.getDashboardInsights(workspaceMemberId);
  }
}
