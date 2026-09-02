/*
This file handles the JPA entity for GitCommit, which represents a commit in a Git repository
It includes fields for the commit hash, author, date, and message, as well as relationships to other entities
such as the repository and associated timesheets. The class is annotated with JPA annotations to map it to the corresponding database table and column
changed files came from V8

Author: Zamokuhle Zwane
Date: 02/09/2026
*/

package timesheets.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "git_commits")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GitCommit {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(name = "workspace_member_id", nullable = false)
  private UUID workspaceMemberId;

  @Column(name = "project_id")
  private UUID projectId;

  @Column(name = "commit_hash", nullable = false, length = 100)
  private String commitHash;

  @Column(name = "repository_name")
  private String repositoryName;

  @Column(name = "repository_url")
  private String repositoryUrl;

  @Column(name = "commit_message")
  private String commitMessage;

  @Column(name = "commit_time")
  private LocalDateTime commitTime;

  @Column(name = "lines_added")
  private Integer linesAdded;

  @Column(name = "lines_removed")
  private Integer linesRemoved;

  @Column(name = "author_name")
  private String authorName;

  @Column(name = "author_email")
  private String authorEmail;

  @Column(name = "github_author_login")
  private String githubAuthorLogin;

  @Column(name = "changed_files")
  private Integer changedFiles;

  @Column(name = "created_at")
  private LocalDateTime createdAt;

  @Column(name = "updated_at")
  private LocalDateTime updatedAt;
}
