package timesheets.evidence;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import timesheets.domain.GitCommit;
import timesheets.repository.GitCommitRepository;

@Component
@RequiredArgsConstructor
public class GitHubEvidenceCollector implements EvidenceCollector {
  private final GitCommitRepository gitCommitRepository;

  @Override
  public List<EvidenceEvent> collect(
      UUID workspaceMemberId, LocalDateTime startTime, LocalDateTime endTime) {

    List<GitCommit> commits =
        gitCommitRepository.findByWorkspaceMemberIdAndCommitTimeBetween(
            workspaceMemberId, startTime, endTime);

    List<EvidenceEvent> evidenceEvents = new ArrayList<>();

    for (GitCommit commit : commits) {
      EvidenceEvent evidenceEvent = new EvidenceEvent();

      evidenceEvent.setId(UUID.randomUUID());
      evidenceEvent.setSource("GITHUB");
      evidenceEvent.setTimestamp(commit.getCommitTime());
      evidenceEvent.setWorkspaceMemberId(workspaceMemberId);
      evidenceEvent.setProjectId(commit.getProjectId());
      evidenceEvent.setActivityType("COMMIT");
      evidenceEvent.setDescription(commit.getCommitMessage());

      Map<String, Object> metadata = new HashMap<>();

      metadata.put("commitHash", commit.getCommitHash());
      metadata.put("repositoryName", commit.getRepositoryName());
      metadata.put("repositoryUrl", commit.getRepositoryUrl());
      metadata.put("authorName", commit.getAuthorName());
      metadata.put("githubAuthorLogin", commit.getGithubAuthorLogin());
      metadata.put("changedFiles", commit.getChangedFiles());
      metadata.put("linesAdded", commit.getLinesAdded());
      metadata.put("linesRemoved", commit.getLinesRemoved());

      evidenceEvent.setMetadata(metadata);
      evidenceEvents.add(evidenceEvent);
    }

    return evidenceEvents;
  }
}
