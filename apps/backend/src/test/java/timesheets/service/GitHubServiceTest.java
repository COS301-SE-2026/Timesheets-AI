// src/test/java/timesheets/service/GitHubServiceTest.java
package timesheets.integration.github;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import timesheets.repository.IntegrationTokenRepository;

// tests getCommits() maps GitCommit entities into GitCommitActivity correctly,
// this is the piece the evidence engine actually depends on, so worth proving
// it works in isolation before anyone wires a real collector against it
@ExtendWith(MockitoExtension.class)
class GitHubServiceTest {

  @Mock private IntegrationTokenRepository integrationTokenRepository;
  @Mock private GitCommitRepository gitCommitRepository;
  @Mock private GitHubOAuthService gitHubOAuthService;

  @InjectMocks private GitHubService gitHubService;

  @Test
  void getCommits_mapsGitCommitToGitCommitActivity() {
    UUID memberId = UUID.randomUUID();
    LocalDateTime start = LocalDateTime.now().minusDays(7);
    LocalDateTime end = LocalDateTime.now();

    GitCommit commit =
        GitCommit.builder()
            .workspaceMemberId(memberId)
            .commitHash("abc123")
            .repositoryName("momently/backend")
            .repositoryUrl("https://github.com/momently/backend")
            .commitMessage("fix: something")
            .commitTime(LocalDateTime.now().minusDays(1))
            .authorName("Zeze")
            .authorEmail("zeze@example.com")
            .githubAuthorLogin("zeze-dev")
            .build();

    when(gitCommitRepository.findByWorkspaceMemberIdAndCommitTimeBetween(memberId, start, end))
        .thenReturn(List.of(commit));

    List<GitCommitActivity> result = gitHubService.getCommits(memberId, start, end);

    assertThat(result).hasSize(1);
    assertThat(result.get(0).getCommitHash()).isEqualTo("abc123");
    assertThat(result.get(0).getRepositoryName()).isEqualTo("momently/backend");
    assertThat(result.get(0).getGithubAuthorLogin()).isEqualTo("zeze-dev");
  }

  @Test
  void getProvider_returnsGithub() {
    assertThat(gitHubService.getProvider()).isEqualTo("GITHUB");
  }
}
