package timesheets.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import timesheets.domain.WorkspaceMember;
import timesheets.enums.WorkspaceRole;

@Repository
public interface WorkspaceMemberRepository extends JpaRepository<WorkspaceMember, UUID> {

  // finds all the workspaces for a given user
  List<WorkspaceMember> findByUserId(UUID userId);

  // finds all the workspace memberships for a workspace
  List<WorkspaceMember> findByWorkspaceId(UUID workspaceId);

  // finds a specific membership with workspace membership and userID
  Optional<WorkspaceMember> findByUserIdAndWorkspaceId(UUID userId, UUID workspaceId);

  // this will see if a user belongs to a specific workspace
  boolean existsByUserIdAndWorkspaceId(UUID userId, UUID workspaceId);

  // this finds the members of a specific workspace, regardless of the role
  List<WorkspaceMember> findAllByWorkspaceIdAndRole(UUID workspaceId, WorkspaceRole role);

  // finds all the members if a workspace
  List<WorkspaceMember> findAllByWorkspaceId(UUID workspaceId);

  //   // this will find all the workspace memberships for a given user, thinking it can be used for
  // like
  //   // a drop down and stuff
  //   List<WorkspaceMember> findAllByUserId(UUID userId);

  // this should find all the devs within a specific workspace
  default List<WorkspaceMember> findAllDevelopersByWorkspaceId(UUID workspaceId) {
    return findAllByWorkspaceIdAndRole(workspaceId, WorkspaceRole.DEVELOPER);
  }

  // this should find all the managers within a specific workspace
  default List<WorkspaceMember> findAllManagersByWorkspaceId(UUID workspaceId) {
    return findAllByWorkspaceIdAndRole(workspaceId, WorkspaceRole.MANAGER);
  }

  // this should find all the admins within a specific workspace
  default List<WorkspaceMember> findAllAdminsByWorkspaceId(UUID workspaceId) {
    return findAllByWorkspaceIdAndRole(workspaceId, WorkspaceRole.ADMIN);
  }

  //   // this will find if a specific user is locked from a workspace, is locked means they are
  //   // suspended from a workspace, or that they cannot access it
  //   @Query(
  //       "SELECT wm.isLocked FROM WorkspaceMember wm WHERE wm.userId = :userId AND wm.workspaceId
  // =:workspaceId")
  //   Optional<Boolean> isUserLockedInWorkspace(
  //       @Param("userId") UUID userId, @Param("workspaceId") UUID workspaceId);
}
