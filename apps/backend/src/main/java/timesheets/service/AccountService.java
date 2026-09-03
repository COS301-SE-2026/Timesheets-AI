package timesheets.service;

import exception.ResourceNotFoundException;
import exception.StateConflictException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import timesheets.domain.User;
import timesheets.domain.WorkspaceMember;
import timesheets.dto.request.AccountDeletionRequest;
import timesheets.enums.WorkspaceRole;
import timesheets.repository.UserRepository;
import timesheets.repository.WorkspaceMemberRepository;
import timesheets.security.SecurityUtils;

@Service
@RequiredArgsConstructor
@Slf4j
public class AccountService {
  private final SecurityUtils securityUtils;
  private final UserRepository userRepository;
  private final WorkspaceMemberRepository workspaceMemberRepository;

  // when the user requests account deletion
  @Transactional
  public void requestDeletion(AccountDeletionRequest.Request request) {

    UUID userId = securityUtils.getCurrentUserId();

    User user =
        userRepository
            .findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));

    // if the user has already requested deletion they should not be able to
    if (user.getDeletionRequestedAt() != null) {
      throw new StateConflictException(
          "You have already requested account deletion. Please wait for admin approval.");
    }

    // if the deletion was processed then they should not be able to delete again
    if (user.getDeletionProcessedAt() != null) {
      throw new StateConflictException("Your deletion request has already been processed.");
    }

    /*
    - if the user is the last admin system-wide then this gets rejected
    - if the user is the last manager in the system then they cannot delete their account
     */
    validateUserCanBeDeleted(userId);

    // the deletion request should be marked
    userRepository.requestDeletion(userId, LocalDateTime.now(), request.getReason());

    log.info(
        "User {} requested account deletion. Reason: {}", user.getEmail(), request.getReason());

    // TODO: Notify all admins about the deletion request: with our Observer pattern
  }

  // !helper functions

  private void validateUserCanBeDeleted(UUID userId) {
    // the last admin across the entire system cannot be deleted
    validateUserNotLastAdminInAnyWorkspace(userId);

    // the last manager in that specific workspace cannot be deleted
    validateNotLastManagerInWorkspace(userId);
  }

  private void validateUserNotLastAdminInAnyWorkspace(UUID userId) {

    // this finds the workspace roles of admins across the entire workspace
    List<WorkspaceMember> allAdminMemberships =
        workspaceMemberRepository.findAll().stream()
            .filter(member -> member.getRole() == WorkspaceRole.ADMIN)
            .toList();

    /*
    - this should get the unique user id's of all the admins
    - an admin can be an admin in multiple workspaces, or different roles in different workspaces
    - so the check makes sure that there is atleast on admin on all the workspaces that are seen */
    List<UUID> adminUserIds =
        allAdminMemberships.stream().map(WorkspaceMember::getUserId).distinct().toList();

    // if this user is an admin and they are the only admin across the entire system
    boolean isUserAdmin = adminUserIds.contains(userId);
    if (isUserAdmin && adminUserIds.size() == 1) {
      throw new StateConflictException(
          "Cannot delete the only system administrator. Please assign another user as an admin in any workspace first.");
    }
  }

  private void validateNotLastManagerInWorkspace(UUID userId) {

    List<WorkspaceMember> userWorkspaces = workspaceMemberRepository.findByUserId(userId);

    for (WorkspaceMember membership : userWorkspaces) {

      // should only check the workspaces where the user is a manager
      if (membership.getRole() != WorkspaceRole.MANAGER) continue;

      UUID workspaceId = membership.getWorkspaceId();

      List<WorkspaceMember> managers =
          workspaceMemberRepository.findAllByWorkspaceIdAndRole(workspaceId, WorkspaceRole.MANAGER);

      // if this is the only manager in the workspace then they should not be able to be deleted
      if (managers.size() == 1 && managers.get(0).getUserId().equals(userId)) {
        throw new StateConflictException(
            "Cannot delete the only manager of workspace. Please assign another manager to the workspace first.");
      }
    }
  }
}
