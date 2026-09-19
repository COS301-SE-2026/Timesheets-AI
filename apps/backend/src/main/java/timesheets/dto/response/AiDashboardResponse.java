/*
This file handles mapping the Dashboard response schema 1:1

Author: Zamokuhle Zwane
Date: 02/09/2026
*/

package timesheets.dto.response;

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
    private String explanation;
  }
}
