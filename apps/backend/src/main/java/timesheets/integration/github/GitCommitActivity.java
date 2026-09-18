/*
this file is the provider-independent representation of a single github commit, this is what GitHubAdapter hands back to callers like the Evidence Engine, keeps them
decoupled from the GitCommit jpa entity and its db column names

Author: Zamokuhle Zwane
Date: 03/09/2026
*/

package timesheets.integration.github;

import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GitCommitActivity {

  private UUID workspaceMemberId;
  private UUID projectId;
  private String commitHash;
  private String repositoryName;
  private String repositoryUrl;
  private String commitMessage;
  private LocalDateTime commitTime;
  private Integer linesAdded;
  private Integer linesRemoved;
  private String authorName;
  private String authorEmail;
  private String githubAuthorLogin;
  private Integer changedFiles;
}
