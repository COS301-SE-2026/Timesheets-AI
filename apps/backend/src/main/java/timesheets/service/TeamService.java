package timesheets.service;

import exception.AccessDeniedException;
import exception.ResourceNotFoundException;
import exception.StateConflictException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import timesheets.domain.User;
import timesheets.domain.WorkspaceMember;
import timesheets.dto.request.AssignWorkspaceMemberRequest;
import timesheets.dto.response.AvailableUserResponse;
import timesheets.dto.response.WorkspaceMemberResponse;
import timesheets.enums.WorkspaceRole;
import timesheets.repository.ProjectMemberRepository;
import timesheets.repository.ProjectRepository;
import timesheets.repository.TaskRepository;
import timesheets.repository.UserRepository;
import timesheets.repository.WorkspaceMemberRepository;
import timesheets.repository.WorkspaceRepository;
import timesheets.security.SecurityUtils;

@Service
@RequiredArgsConstructor
public class TeamService {

  private final WorkspaceMemberRepository workspaceMemberRepository;
  private final UserRepository userRepository;
  private final ProjectMemberRepository projectMemberRepository;
  private final ProjectRepository projectRepository;
  private final WorkspaceRepository workspaceRepository;
  private final SecurityUtils securityUtils;
  private final TaskRepository taskRepository;
  private final TimerService timerService;

  /*
  - this is to assign members to a workspace
  - for admins only */
  @Transactional
  public WorkspaceMemberResponse assignUserToWorkspace(AssignWorkspaceMemberRequest request) {

    // only admins should be able to control who has access to a workspace
    if (!securityUtils.isAdmin()) {
      throw new AccessDeniedException("Only Admins can assign users to workspaces");
    }

    // since a user can belong to multiple workspaces, this is to ensure correct workspace
    UUID workspaceId = securityUtils.getCurrentWorkspaceId();

    // if the workspace does not exist then cannot assign a user there
    if (!workspaceRepository.existsById(workspaceId)) {
      throw new ResourceNotFoundException("Workspace not found");
    }

    // does the user we are assignin actually exist?
    User user =
        userRepository
            .findById(request.getUserId())
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));

    // only one user should belong to a workspace to prevent duplicate info
    // check all memberships including the historic ones as well because a user may have previously left this workspace
    Optional<WorkspaceMember> existingMembership = workspaceMemberRepository.findByUserIdAndWorkspaceId(request.getUserId(), workspaceId);

    WorkspaceMember saved;

    if (existingMembership.isPresent()) {

      WorkspaceMember member = existingMembership.get();

      if (Boolean.TRUE.equals(member.getIsActive())) {
        throw new StateConflictException("User is already a member of this workspace");
      }

      /*
      - this will reactivate the soft deleted so a user keeps the same workspace member identity
      - this is so that I can keep all the workspace activity linked to the same membership
      */
      member.setIsActive(true);
      member.setRemovedAt(null);
      member.setRole(request.getRole());
      member.setJoinedAt(LocalDateTime.now());

      saved = workspaceMemberRepository.save(member);

    } 
    else {

      //a new membership gets created only if the user is a part of the workspace for the first time
      WorkspaceMember member = new WorkspaceMember();
      member.setWorkspaceId(workspaceId);
      member.setUserId(request.getUserId());
      member.setRole(request.getRole());
      member.setJoinedAt(LocalDateTime.now());

      saved = workspaceMemberRepository.save(member);
    }

    return WorkspaceMemberResponse.builder()
        .workspaceMemberId(saved.getId())
        .userId(saved.getUserId())
        .firstName(user.getFirstName())
        .lastName(user.getLastName())
        .email(user.getEmail())
        .role(saved.getRole())
        .joinedAt(saved.getJoinedAt())
        .build();
  }

  // this will be about removing a user from the workspace
  @Transactional
  public void removeUserFromWorkspace(UUID workspaceMemberId) {

    // only admins should have the rights to remove users from the workspace
    if (!securityUtils.isAdmin()) {
      throw new AccessDeniedException("Only Admins can remove users from workspaces");
    }

    WorkspaceMember member =
        workspaceMemberRepository
            .findById(workspaceMemberId)
            .orElseThrow(() -> new ResourceNotFoundException("Workspace member not found"));

    // use the member's workspace because admins can remove members from any workspace
    UUID workspaceId = member.getWorkspaceId();

    if (!Boolean.TRUE.equals(member.getIsActive())) {
      throw new StateConflictException("Workspace member has already been removed");
    }

    // I want to make sure that workspace admins do not go below 1 cause there should always be
    // someone who has access to them
    List<WorkspaceMember> admins =
        workspaceMemberRepository.findAllByWorkspaceIdAndRoleAndIsActiveTrue(
            workspaceId, WorkspaceRole.ADMIN);

    if (admins.size() <= 1 && member.getRole() == WorkspaceRole.ADMIN) {
      throw new StateConflictException("Cannot remove the last Admin from the workspace");
    }

    LocalDateTime removedAt = LocalDateTime.now();

    // a removed member should not leave an active timer running in the workspace, because then who will remove it?
    timerService.discardTimerForWorkspaceRemoval(workspaceMemberId);

    // deactivate the workspace membership while preserving the records
    member.setIsActive(false);
    member.setRemovedAt(removedAt);
    workspaceMemberRepository.save(member);

    // removing someone from the workspace also removes their current project access
    projectMemberRepository.deactivateAllByWorkspaceMemberId(workspaceMemberId, removedAt);

    // tasks remain in the system but should no longer be assigned to the removed member
    taskRepository.unassignActiveTasksFromWorkspaceMember(workspaceMemberId, removedAt);
  }

  /*
  - this returns all users with a flag, frontend can then use the flag to filter
  - this should allow, the members not a part of the workspace to be retrieved
  - since we want both to list users in the workspace and then also users that are to be added to that workspace
  */
  @Transactional(readOnly = true)
  public List<AvailableUserResponse> getAvailableUsers(UUID workspaceId) {

    // checking if the workspace actually exists
    if (!workspaceRepository.existsById(workspaceId)) {
      throw new ResourceNotFoundException("Workspace not found");
    }

    List<UUID> userIdsInWorkspace =
        workspaceMemberRepository.findByWorkspaceIdAndIsActiveTrue(workspaceId).stream()
            .map(WorkspaceMember::getUserId)
            .collect(Collectors.toList());

    List<User> usersInWorkspace = userRepository.findAllById(userIdsInWorkspace);

    /*
    ADMIN: all the users will be returned but they will have a flag, that indicates if they are a part of the workspace or not
    - frontend can then use this flag to filter the users
    */
    if (securityUtils.isAdmin()) {
      List<User> allUsers = userRepository.findAll();

      return allUsers.stream()
          .map(
              user ->
                  AvailableUserResponse.builder()
                      .userId(user.getId())
                      .firstName(user.getFirstName())
                      .lastName(user.getLastName())
                      .email(user.getEmail())
                      .isInWorkspace(userIdsInWorkspace.contains(user.getId()))
                      .build())
          .collect(Collectors.toList());
    }

    /*
    MANAGER:
    - they should only see the list of the users in that workspace
    - then with that list they can assign people and stuff
    - the flag will always be true for managers
     */
    if (securityUtils.isManager()) {
      return usersInWorkspace.stream()
          .map(
              user ->
                  AvailableUserResponse.builder()
                      .userId(user.getId())
                      .firstName(user.getFirstName())
                      .lastName(user.getLastName())
                      .email(user.getEmail())
                      .isInWorkspace(true)
                      .build())
          .collect(Collectors.toList());
    }

    /*
    - devs should not be able to see workspace members like that
    - devs should only see people they are in projects with
    */
    throw new AccessDeniedException("Only Admins and Managers can view workspace members");
  }
}
