/*
This file response generating insights tells the frontend whether GitHub and Jira came back live (fresh sync worked) or cached (sync failed, serving
whatever's already in git_commits/jira_tickets from the last good sync).

Author: Zamokuhle Zwane
Date: 27/09/2026
*/

package timesheets.dto.response;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GenerateInsightsResponse {
  private SyncResult github;
  private SyncResult jira;

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class SyncResult {
    private String source; // "LIVE" or "CACHED"
    private LocalDateTime lastSyncedAt;
  }
}
