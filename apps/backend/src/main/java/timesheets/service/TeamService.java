package timesheets.service;

import exception.AccessDeniedException;
import exception.ResourceNotFoundException;
import exception.StateConflictException;
import java.time.LocalDateTime;
import java.util.List;
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
    UUID workspaceId = request.getWorkspaceId();

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
    if (workspaceMemberRepository.existsByUserIdAndWorkspaceId(request.getUserId(), workspaceId)) {
      throw new StateConflictException("User is already a member of this workspace");
    }

    WorkspaceMember member = new WorkspaceMember();
    member.setWorkspaceId(workspaceId);
    member.setUserId(request.getUserId());
    member.setRole(request.getRole());
    member.setJoinedAt(LocalDateTime.now());
    member.setCreatedAt(LocalDateTime.now());
    member.setUpdatedAt(LocalDateTime.now());

    WorkspaceMember saved = workspaceMemberRepository.save(member);

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

    UUID workspaceId = member.getWorkspaceId();

    // I want to make sure that workspace admins do not go below 1 cause there should always be
    // someone who has access to them
    List<WorkspaceMember> admins =
        workspaceMemberRepository.findAllByWorkspaceIdAndRole(workspaceId, WorkspaceRole.ADMIN);

    if (admins.size() <= 1 && member.getRole() == WorkspaceRole.ADMIN) {
      throw new StateConflictException("Cannot remove the last Admin from the workspace");
    }

    workspaceMemberRepository.delete(member); // deleting them
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
        workspaceMemberRepository.findByWorkspaceId(workspaceId).stream()
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
