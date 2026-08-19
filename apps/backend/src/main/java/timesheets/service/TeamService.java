package timesheets.service;

import exception.AccessDeniedException;
import exception.ResourceNotFoundException;
import exception.StateConflictException;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import timesheets.domain.User;
import timesheets.domain.WorkspaceMember;
import timesheets.dto.request.AssignWorkspaceMemberRequest;
import timesheets.dto.response.WorkspaceMemberResponse;
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
}
