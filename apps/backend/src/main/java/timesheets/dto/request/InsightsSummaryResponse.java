package timesheets.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

//dto for insights summary response, 
//contains totalHoursLogged, averageHoursPerDay,
//taskCompletionRate, hoursPerProject and weeklyTrend

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InsightsSummaryResponse { 
    //dto for insights summary response, contains overall summary of time entries for a given period,
    //as well as breakdowns by project, task, and daily trends
    private Double totalHoursLogged;
    private Double averageHoursPerDay;
    private Double taskCompletionRate;
    private List<TopContributor> topContributors; //this will be for the managers insights summary, showing the top contributors in terms of hours logged
    private List<ProjectHours> hoursPerProject;
    private List<WeeklyTrend> weeklyTrend;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TopContributor { //this is for the managers insights summary, showing the top contributors in terms of hours logged
        private UUID userId;
        private String name;
        private Double hours;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProjectHours { //this is for project hours breakdown, contains project id, name, total hours logged, and entry count
        private UUID projectId;
        private String name;
        private Double hours;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WeeklyTrend { //this is for weekly trend breakdown, contains week (in format "2026-W18") and total hours logged that week
        private String week; //format: "2026-W18"
        private Double hours;
    }
}