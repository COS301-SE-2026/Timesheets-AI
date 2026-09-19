package timesheets.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import timesheets.domain.ProjectMember;

@Repository
public interface ProjectMemberRepository extends JpaRepository<ProjectMember, UUID> {

  // this will see if a certain member is already assigned to a specific project
  boolean existsByProjectIdAndWorkspaceMemberId(UUID projectId, UUID workspaceMemberId);

  // finds all the projects that a workspace member is assigned to
  // List<ProjectMember> findByWorkspaceMemberId(UUID workspaceMemberId);
  List<ProjectMember> findByWorkspaceMemberIdAndIsActiveTrue(UUID workspaceMemberId);

  // finds all the workspace members assigned to a specific project
  // List<ProjectMember> findByProjectId(UUID projectId);
  List<ProjectMember> findByProjectIdAndIsActiveTrue(UUID projectId);

  // finds a specifc record or a member on a project
  Optional<ProjectMember> findByProjectIdAndWorkspaceMemberId(
      UUID projectId, UUID workspaceMemberId);
}
