/*
This file handles the sync last 7 days only, not the full history, keeping the first cut small on purpose

github commits endpoint docs: https://docs.github.com/en/rest/commits/commits
Author: Zamokuhle Zwane
Date: 02/09/2026
*/

package timesheets.service;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;
import timesheets.domain.GitCommit;
import timesheets.domain.IntegrationToken;
import timesheets.repository.GitCommitRepository;
import timesheets.repository.IntegrationTokenRepository;

@Service
@RequiredArgsConstructor
public class GitHubService {
  private final IntegrationTokenRepository integrationTokenRepository;
  private final GitCommitRepository gitCommitRepository;
  private final RestClient restClient = RestClient.create();

  @Transactional
  public int syncRecentCommits(UUID workspaceMemberId) {

    IntegrationToken token =
        integrationTokenRepository
            .findByWorkspaceMemberIdAndProvider(workspaceMemberId, "GITHUB")
            .orElseThrow(() -> new IllegalStateException("GitHub is not connected"));

    String accessToken = token.getAccessToken();

    // step 1: Asks who is this on github
    Map<String, Object> githubUser =
        restClient
            .get()
            .uri("https://api.github.com/user")
            .header("Authorization", "Bearer " + accessToken)
            .header("Accept", "application/vnd.github+json")
            .retrieve()
            .body(Map.class);

    String githubLogin = (String) githubUser.get("login");

    // step 2: checks their repos
    List<Map<String, Object>> repos =
        restClient
            .get()
            .uri("https://api.github.com/user/repos?per_page=100&sort=updated")
            .header("Authorization", "Bearer " + accessToken)
            .header("Accept", "application/vnd.github+json")
            .retrieve()
            .body(List.class);

    LocalDateTime since = LocalDateTime.now().minusDays(7);
    String sinceIso = since.atOffset(ZoneOffset.UTC).toString();

    int saved = 0;

    for (Map<String, Object> repo : repos) {
      String fullName = (String) repo.get("full_name");
      String htmlUrl = (String) repo.get("html_url");

      if (fullName == null) {
        continue;
      }

      List<Map<String, Object>> commits;
      try {
        commits =
            restClient
                .get()
                .uri(
                    "https://api.github.com/repos/"
                        + fullName
                        + "/commits?author="
                        + githubLogin
                        + "&since="
                        + sinceIso
                        + "&per_page=100")
                .header("Authorization", "Bearer " + accessToken)
                .header("Accept", "application/vnd.github+json")
                .retrieve()
                .body(List.class);
      } catch (Exception e) {
        /*
        repo might be empty or access denied, skip it and keep going
        FLAG: log this properly once we have a logging strategy, silent skip is intentional so one bad repo doesn't kill the whole sync
        */
        continue;
      }

      if (commits == null) {
        continue;
      }

      for (Map<String, Object> commit : commits) {
        String sha = (String) commit.get("sha");

        if (sha == null
            || gitCommitRepository.existsByWorkspaceMemberIdAndCommitHash(workspaceMemberId, sha)) {
          continue;
        }

        Map<String, Object> commitDetail = (Map<String, Object>) commit.get("commit");
        Map<String, Object> author = (Map<String, Object>) commitDetail.get("author");

        // github gives the commit date as an iso string like 2026-08-27T14:20:00Z (ref docs above)
        String dateStr = (String) author.get("date");
        LocalDateTime commitTime =
            dateStr != null ? OffsetDateTime.parse(dateStr).toLocalDateTime() : LocalDateTime.now();

        GitCommit savedCommit =
            GitCommit.builder()
                .workspaceMemberId(workspaceMemberId)
                .commitHash(sha)
                .repositoryName(fullName)
                .repositoryUrl(htmlUrl)
                .commitMessage((String) commitDetail.get("message"))
                .commitTime(commitTime)
                .authorName((String) author.get("name"))
                .authorEmail((String) author.get("email"))
                .githubAuthorLogin(githubLogin)
                // github's line-level stats need a separate, more expensive per-commit api call,
                // skipping for the first sync pass
                .linesAdded(0)
                .linesRemoved(0)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        gitCommitRepository.save(savedCommit);
        saved++;
      }
    }

    return saved;
  }
}
