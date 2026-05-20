package timesheets.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

//dto for insights summary response, contains total hours logged, average hours per day,
//total days logged, hours per project, hours per task and daily trend
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InsightsSummaryResponse {
    //total hours logged in the date range
    private Double totalHoursLogged;
    private Double averageHoursPerDay;
    private Integer totalDaysLogged;
    private List<ProjectHours> hoursPerProject;
    private List<TaskHours> hoursPerTask;
    private List<DailyTrend> dailyTrend;

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class ProjectHours {
        //project id, name, total hours logged and entry count for the project
        private UUID projectId;
        private String projectName;
        private Double hours;
        private Integer entryCount;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class TaskHours {
        //task id, name, total hours logged and entry count for the task
        private UUID taskId;
        private String taskTitle;
        private Double hours;
        private String status;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class DailyTrend {
        //date, total hours logged and entry count for the day
        private String date;
        private Double hours;
        private Integer entryCount;
    }
}
