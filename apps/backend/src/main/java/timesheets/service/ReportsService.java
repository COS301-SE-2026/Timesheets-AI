package timesheets.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.IsoFields;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import timesheets.domain.TimeEntry;
import timesheets.domain.User;
import timesheets.dto.request.ProductivityReportRequest;
import timesheets.dto.response.ProductivityReportResponse;
import timesheets.repository.TimeEntryRepository;

// service for generating various reports, currently implements productivity report generation based
// on time entries for a given user and date range
// the service will fetch the relevant time entries from the database, perform calculations to
// summarize the data, and return a structured response that can be used by the frontend to display
// the report
// we could easily extend this service in the future to add more types of reports, such as
// project-based reports, team reports, etc.
// I'm thinking we could create a pdf and excel report generation service as well,
// that would take the same data and generate downloadable reports for users who want to keep
// records or share with others
// it'll need to be able to generate well-formatted reports that include charts and tables to
// visualize the data effectively

@Service
@RequiredArgsConstructor
public class ReportsService {

  private final TimeEntryRepository timeEntryRepository;

  // we could inject other repositories here as needed, for example if we want to join with projects
  // or tasks to get more detailed report data
  public ProductivityReportResponse generateProductivityReport(
      ProductivityReportRequest request, User currentUser) {

    // fetch all time entries for the developer in the date range
    List<TimeEntry> entries =
        timeEntryRepository.findByUserIdAndDateRange(
            currentUser.getId(),
            request.getFrom().atStartOfDay(),
            request.getTo().atTime(23, 59, 59));

    // calculate summary
    double totalHours =
        entries.stream().mapToDouble(entry -> entry.getDurationMinutes() / 60.0).sum();

    int totalEntries = entries.size();

    long uniqueTasks =
        entries.stream()
            .map(TimeEntry::getTaskId)
            .filter(taskId -> taskId != null)
            .distinct()
            .count();

    long uniqueProjects = entries.stream().map(TimeEntry::getProjectId).distinct().count();

    // group by task
    List<ProductivityReportResponse.TaskBreakdown> taskBreakdown =
        entries.stream() // filter out entries without a task, then group by task ID
            .filter(entry -> entry.getTaskId() != null)
            .collect(Collectors.groupingBy(TimeEntry::getTaskId))
            .entrySet()
            .stream()
            .map(
                entry -> {
                  double hours =
                      entry.getValue().stream()
                          .mapToDouble(e -> e.getDurationMinutes() / 60.0)
                          .sum();

                  TimeEntry sample = entry.getValue().get(0);

                  return ProductivityReportResponse.TaskBreakdown.builder()
                      .taskId(entry.getKey())
                      .taskTitle("Task " + entry.getKey()) // TODO: join with tasks table
                      .projectName("Project") // TODO: join with projects table
                      .hoursLogged(hours)
                      .entryCount(entry.getValue().size())
                      .build();
                })
            .sorted((a, b) -> Double.compare(b.getHoursLogged(), a.getHoursLogged()))
            .collect(Collectors.toList());

    // group by week
    Map<String, List<TimeEntry>> weeklyGroups =
        entries
            .stream() // calculate the week for each entry based on start time, then group by that
            // week
            .collect(
                Collectors.groupingBy(
                    entry -> {
                      LocalDate date = entry.getStartTime().toLocalDate();
                      int year = date.getYear();
                      int week = date.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR);
                      return String.format("%d-W%02d", year, week);
                    }));

    List<ProductivityReportResponse.WeeklyBreakdown> weeklyBreakdown =
        weeklyGroups
            .entrySet()
            .stream() // for each week group, calculate total hours and entry count, and create a
            // WeeklyBreakdown object
            .map(
                entry -> {
                  double hours =
                      entry.getValue().stream()
                          .mapToDouble(e -> e.getDurationMinutes() / 60.0)
                          .sum();

                  return ProductivityReportResponse.WeeklyBreakdown.builder()
                      .week(entry.getKey())
                      .hours(hours)
                      .entryCount(entry.getValue().size())
                      .build();
                })
            .sorted((a, b) -> a.getWeek().compareTo(b.getWeek()))
            .collect(Collectors.toList());

    return ProductivityReportResponse.builder()
        .generatedAt(LocalDateTime.now())
        .period(
            ProductivityReportResponse.Period.builder()
                .from(request.getFrom())
                .to(request.getTo())
                .build())
        .summary(
            ProductivityReportResponse.Summary.builder()
                .totalHoursLogged(totalHours)
                .totalEntriesLogged(totalEntries)
                .tasksWorkedOn((int) uniqueTasks)
                .projectsWorkedOn((int) uniqueProjects)
                .build())
        .byTask(taskBreakdown)
        .byWeek(weeklyBreakdown)
        .build();
  }
}
