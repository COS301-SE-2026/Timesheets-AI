/*
This file is the provider-independent contract for github evidence it mirrors CalendarAdapter's
pattern so the Evidence Engine's GitHubEvidenceCollector depends on this interface, never on GitHubService or the raw github api directly

Author: Zamokuhle Zwane
Date: 19/09/2026
*/
package timesheets.integration.github;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import timesheets.integration.IntegrationAdapter;

public interface GitHubAdapter extends IntegrationAdapter {

  List<GitCommitActivity> getCommits(
      UUID workspaceMemberId, LocalDateTime startTime, LocalDateTime endTime);
}
