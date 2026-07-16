package timesheets.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/*  dto for productivity report response, contains overall summary of time entries for the given period, as well as breakdowns by task and weekly trends */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductivityReportResponse {
  // this is the response dto for the productivity report, it contains overall
  // summary of time
  // entries for the given period,
  // as well as breakdowns by task and weekly trends
  private LocalDateTime generatedAt;
  private Period period;
  private Summary summary;
  private List<TaskBreakdown> byTask;
  private List<WeeklyBreakdown> byWeek;

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class Period {
    // this is the period for which the report was generated, contains from and to
    // dates
    private LocalDate from;
    private LocalDate to;
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class Summary {
    // this is the overall summary of time entries for the given period
    // contains total hours logged, total entries logged, number of unique tasks
    // worked on, and
    // number of unique projects worked on
    private Double totalHoursLogged;
    private Integer totalEntriesLogged;
    private Integer tasksWorkedOn;
    private Integer projectsWorkedOn;
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class TaskBreakdown {
    // this is the breakdown of time entries by task, contains task id, title,
    // project name, total
    // hours logged, and entry count
    private UUID taskId;
    private String taskTitle;
    private String projectName;
    private Double hoursLogged;
    private Integer entryCount;
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class WeeklyBreakdown {
    // this is the breakdown of time entries by week, contains week identifier,
    // total hours logged,
    // and entry count
    private String week;
    private Double hours;
    private Integer entryCount;
  }
}
