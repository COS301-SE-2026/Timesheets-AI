/*
This mirrors ai-service's ManagerDashboardResponse 1:1, snake_case mapped by AiServiceClient's snakeCaseMapper the same way AiDashboardResponse is.

Author: Zamokuhle Zwane
Date: 26/09/2026
*/

package timesheets.dto.response;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ManagerDashboardResponse {

  private ProjectCompletionForecast projectCompletionForecast;
  private TeamLoggedHours teamLoggedHours;
  private List<FlaggedBurnoutMember> flaggedBurnoutMembers;
  private TaskOverview taskOverview;
  private Velocity velocity;
  private ProjectHealth projectHealth;
  private List<GithubActivityByMember> githubActivityByMember;
  private JiraTicketBreakdown jiraTicketBreakdown;
  private List<ManagedProject> managedProjects;
  private UUID scopedProjectId;
  private String period;

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class WeeklyProgressPoint {
    private String weekLabel;
    private double percent;
  }

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class ProjectCompletionForecast {
    private List<WeeklyProgressPoint> weeklyProgress;
    private double currentProgressPercent;
    private LocalDate forecastCompletionDate;
  }

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class TeamLoggedHoursMember {
    private String memberName;
    private double hours;
  }

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class TeamLoggedHours {
    private List<TeamLoggedHoursMember> members;
  }

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class FlaggedBurnoutMember {
    private UUID workspaceMemberId;
    private double riskScore;
    private String statusBand; // HIGH_RISK / AT_RISK
    private String reason;
    private UUID insightId;
    // filled in on the java side, not by ai-service, since ai-service only knows the member id
    private String memberName;
    private String memberRole;
  }

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class TaskOverview {
    private int total;
    private int todo;
    private int inProgress;
    private int done;
    private int blocked;
    private String narrative;
  }

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class VelocityWeek {
    private String weekLabel;
    private int completed;
  }

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class Velocity {
    private List<VelocityWeek> weeks;
    private double rollingAverage;
  }

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class ProjectHealth {
    private int score;
    private String statusLabel;
    private double onTrackPercent;
    private double atRiskPercent;
    private double behindPercent;
  }

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class GithubActivityByMember {
    private UUID workspaceMemberId;
    private String memberName;
    private double hoursLogged;
    private int commitCount;
  }

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class JiraTypeCount {
    private String issueType;
    private int count;
    private double percentage;
  }

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class JiraTicketBreakdown {
    private int total;
    private List<JiraTypeCount> byType;
  }

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class ManagedProject {
    private UUID projectId;
    private String projectName;
  }
}
