package timesheets.dto.response;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectForecastEvidenceResponse {

  // this is the project that the evidence is for
  private UUID projectId;
  private ExternalEvidenceSummary github;
  private ExternalEvidenceSummary jira;

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class ExternalEvidenceSummary {

    // this will be for if evidence has been found for this source
    private boolean available;
    private int activityCount;
    private LocalDateTime latestActivity;
    private List<ActivityCount> activities;
  }

  // this class will be the number of times a certain type of  activity has been found in the
  // external evidence
  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class ActivityCount {

    // will be whether it is a COMMIT or an ISSUe that kinda thing
    private String activityType;
    private int count;
  }
}
