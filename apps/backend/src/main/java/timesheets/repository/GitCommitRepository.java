/*
This repository interface for git_commits, used by Github service to avoid resaving commits we've already synced

Author: Zamokuhle Zwane
Date: 02/09/2026
*/

package timesheets.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import timesheets.domain.GitCommit;

@Repository
public interface GitCommitRepository extends JpaRepository<GitCommit, UUID> {

  boolean existsByWorkspaceMemberIdAndCommitHash(UUID workspaceMemberId, String commitHash);

  List<GitCommit> findByWorkspaceMemberIdAndCommitTimeBetween(
      UUID workspaceMemberId, LocalDateTime from, LocalDateTime to);
}
