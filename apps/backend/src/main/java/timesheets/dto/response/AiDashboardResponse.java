/*
This file handles mapping the Dashboard response schema 1:1

Author: Zamokuhle Zwane
Date: 02/09/2026

Patch: added resolved, resolvedAt, resolvedByWorkspaceMemberId to Insight (V19)
Patch: added period, productivityTrend, timeAllocation, estimateVsActual,
scoreCards, timeSplitByTask for Developer Insights v2. Added alignmentScore
to GithubActivity
*/

package timesheets.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AiDashboardResponse {

  private UUID workspaceMemberId;
  private List<Insight> insights;
  private GithubActivity github;
  private String period;
  private ProductivityTrend productivityTrend;
  private TimeAllocation timeAllocation;
  private EstimateVsActual estimateVsActual;
  private List<ScoreCard> scoreCards;
  private TimeSplitByTask timeSplitByTask;
  private TaskSwitchingByDay taskSwitchingByDay;

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class Insight {

    private UUID id;
    private String insightType;
    private String scope;
    private Double score;
    private Double confidence;
    private String description;
    private String recommendation;
    private String narrative;
    private UUID projectId;
    private String projectName;
    private UUID workspaceMemberId;
    private String memberName;
    private UUID workspaceId;
    private LocalDateTime createdAt;
    private boolean resolved;
    private LocalDateTime resolvedAt;
    private UUID resolvedByWorkspaceMemberId;
  }

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class GithubActivity {
    private boolean connected;
    private double hoursLogged;
    private int commitCount;
    private double commitsPerHour;
    private int additions;
    private int deletions;
    private int activeRepositories;
    private int activeDays;
    private String alignment;
    private Double alignmentScore;
    private String explanation;
  }

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class ProductivityTrendPoint {
    private String weekLabel;
    private Double userScore;
    private Double teamAverage;
  }

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class ProductivityTrend {
    private List<ProductivityTrendPoint> points;

    @JsonProperty("is_synthetic")
    private boolean isSynthetic;
  }

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class TimeAllocationCategory {
    private String category;
    private double hours;
    private double percentage;
  }

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class TimeAllocation {
    private double totalHours;
    private List<TimeAllocationCategory> categories;
  }

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class EstimateVsActualCategory {
    private String category;
    private double estimatedHours;
    private double actualHours;
  }

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class EstimateVsActual {
    private List<EstimateVsActualCategory> categories;
  }

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class ScoreCard {
    private String insightType;
    private Double score;
    private String status;
    private Double deltaVsPrevious;
    private String recommendation;
    private List<Double> sparkline;
  }

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class TimeSplitByTaskItem {
    private String taskTitle;
    private double hours;
    private double percentage;
  }

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class TimeSplitByTask {
    private double totalHours;
    private List<TimeSplitByTaskItem> tasks;
  }

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class TaskSwitchingByDayItem {
    private String dayLabel;
    private int switches;
  }

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class TaskSwitchingByDay {
    private List<TaskSwitchingByDayItem> days;
  }
}
