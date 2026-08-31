package timesheets.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import exception.AccessDeniedException;
import exception.ResourceNotFoundException;
import exception.StateConflictException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
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

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("TeamService Unit Tests")
public class TeamServiceTest {

  @Mock private SecurityUtils securityUtils;
  @Mock private WorkspaceMemberRepository workspaceMemberRepository;
  @Mock private UserRepository userRepository;
  @Mock private ProjectMemberRepository projectMemberRepository;
  @Mock private ProjectRepository projectRepository;
  @Mock private WorkspaceRepository workspaceRepository;

  @InjectMocks private TeamService teamService;

  private final UUID testUserId = UUID.randomUUID();
  private final UUID testWorkspaceId = UUID.randomUUID();
  private final UUID testWorkspaceMemberId = UUID.randomUUID();
  private final String testFirstName = "Endo";
  private final String testLastName = "Khuhle";
  private final String testEmail = "endokhuhle@momentum.co.za";

  private User createTestUser() {
    User user = new User();

    user.setId(testUserId);
    user.setFirstName(testFirstName);
    user.setLastName(testLastName);
    user.setEmail(testEmail);
    user.setCreatedAt(LocalDateTime.now());
    user.setUpdatedAt(LocalDateTime.now());

    return user;
  }

  private WorkspaceMember createTestWorkspaceMember() {

    WorkspaceMember member = new WorkspaceMember();

    member.setId(testWorkspaceMemberId);
    member.setUserId(testUserId);
    member.setWorkspaceId(testWorkspaceId);
    member.setRole(WorkspaceRole.DEVELOPER);
    member.setJoinedAt(LocalDateTime.now());
    member.setCreatedAt(LocalDateTime.now());
    member.setUpdatedAt(LocalDateTime.now());

    return member;
  }

  private WorkspaceMember createTestAdminWorkspaceMember() {
    WorkspaceMember member = new WorkspaceMember();

    member.setId(UUID.randomUUID());
    member.setUserId(UUID.randomUUID());
    member.setWorkspaceId(testWorkspaceId);
    member.setRole(WorkspaceRole.ADMIN);
    member.setJoinedAt(LocalDateTime.now());
    member.setCreatedAt(LocalDateTime.now());
    member.setUpdatedAt(LocalDateTime.now());

    return member;
  }

  private AssignWorkspaceMemberRequest createValidAssignRequest() {
    AssignWorkspaceMemberRequest request = new AssignWorkspaceMemberRequest();

    request.setUserId(testUserId);
    request.setRole(WorkspaceRole.DEVELOPER);

    return request;
  }

  @Nested
  @DisplayName("Assign User To Workspace Tests")
  class AssignUserToWorkspaceTests {

    @Test
    @DisplayName("assign user to workspace successfully")
    void assignUserToWorkspace() {

      // ARRANGE: setting up the request and the user
      AssignWorkspaceMemberRequest request = createValidAssignRequest();
      User user = createTestUser();
      WorkspaceMember savedMember = createTestWorkspaceMember();

      // specifying that the user is an admin
      when(securityUtils.isAdmin()).thenReturn(true);
      when(securityUtils.getCurrentWorkspaceId()).thenReturn(testWorkspaceId);
      when(workspaceRepository.existsById(testWorkspaceId)).thenReturn(true);

      when(userRepository.findById(testUserId)).thenReturn(Optional.of(user));
      when(workspaceMemberRepository.existsByUserIdAndWorkspaceId(testUserId, testWorkspaceId))
          .thenReturn(false);
      when(workspaceMemberRepository.save(any(WorkspaceMember.class))).thenReturn(savedMember);

      // ACT: assigning the user to the workspace
      WorkspaceMemberResponse response = teamService.assignUserToWorkspace(request);

      // ASSERT: Verify the user was assigned
      assertThat(response).isNotNull();
      assertThat(response.getUserId()).isEqualTo(testUserId);
      assertThat(response.getFirstName()).isEqualTo(testFirstName);
      assertThat(response.getLastName()).isEqualTo(testLastName);

      assertThat(response.getEmail()).isEqualTo(testEmail);
      assertThat(response.getRole()).isEqualTo(WorkspaceRole.DEVELOPER);

      verify(workspaceMemberRepository).save(any(WorkspaceMember.class));
    }

    @Test
    @DisplayName("throw exception when non-admin tries to assign user")
    void throwExceptionWhenNonAdminAssignsUser() {

      AssignWorkspaceMemberRequest request = createValidAssignRequest();

      when(securityUtils.isAdmin()).thenReturn(false);

      assertThatThrownBy(() -> teamService.assignUserToWorkspace(request))
          .isInstanceOf(AccessDeniedException.class)
          .hasMessage("Only Admins can assign users to workspaces");

      verify(workspaceMemberRepository, never()).save(any(WorkspaceMember.class));
    }

    @Test
    @DisplayName("throw exception when workspace does not exist")
    void throwExceptionWhenWorkspaceNotFound() {
      AssignWorkspaceMemberRequest request = createValidAssignRequest();

      when(securityUtils.isAdmin()).thenReturn(true);
      when(securityUtils.getCurrentWorkspaceId()).thenReturn(testWorkspaceId);
      when(workspaceRepository.existsById(testWorkspaceId)).thenReturn(false);

      assertThatThrownBy(() -> teamService.assignUserToWorkspace(request))
          .isInstanceOf(ResourceNotFoundException.class)
          .hasMessage("Workspace not found");

      verify(workspaceMemberRepository, never()).save(any(WorkspaceMember.class));
    }

    @Test
    @DisplayName("throw exception when user does not exist")
    void throwExceptionWhenUserNotFound() {
      AssignWorkspaceMemberRequest request = createValidAssignRequest();

      when(securityUtils.isAdmin()).thenReturn(true);
      when(securityUtils.getCurrentWorkspaceId()).thenReturn(testWorkspaceId);

      when(workspaceRepository.existsById(testWorkspaceId)).thenReturn(true);
      when(userRepository.findById(testUserId)).thenReturn(Optional.empty());

      assertThatThrownBy(() -> teamService.assignUserToWorkspace(request))
          .isInstanceOf(ResourceNotFoundException.class)
          .hasMessage("User not found");

      verify(workspaceMemberRepository, never()).save(any(WorkspaceMember.class));
    }

    @Test
    @DisplayName("throw exception when user is already a member of workspace")
    void throwExceptionWhenUserAlreadyInWorkspace() {
      AssignWorkspaceMemberRequest request = createValidAssignRequest();

      when(securityUtils.isAdmin()).thenReturn(true);
      when(securityUtils.getCurrentWorkspaceId()).thenReturn(testWorkspaceId);
      when(workspaceRepository.existsById(testWorkspaceId)).thenReturn(true);

      when(userRepository.findById(testUserId)).thenReturn(Optional.of(createTestUser()));
      when(workspaceMemberRepository.existsByUserIdAndWorkspaceId(testUserId, testWorkspaceId))
          .thenReturn(true);

      assertThatThrownBy(() -> teamService.assignUserToWorkspace(request))
          .isInstanceOf(StateConflictException.class)
          .hasMessage("User is already a member of this workspace");

      verify(workspaceMemberRepository, never()).save(any(WorkspaceMember.class));
    }
  }

  @Nested
  @DisplayName("Remove User From Workspace Tests")
  class RemoveUserFromWorkspaceTests {

    @Test
    @DisplayName("remove user from workspace successfully")
    void removeUserFromWorkspace() {

      // ARRANGE: setting up the workspace member to be removed
      WorkspaceMember member = createTestWorkspaceMember();
      List<WorkspaceMember> admins = List.of(createTestAdminWorkspaceMember());

      when(securityUtils.isAdmin()).thenReturn(true);
      when(securityUtils.getCurrentWorkspaceId()).thenReturn(testWorkspaceId);
      when(workspaceMemberRepository.findById(testWorkspaceMemberId))
          .thenReturn(Optional.of(member));
      when(workspaceMemberRepository.findAllByWorkspaceIdAndRole(
              testWorkspaceId, WorkspaceRole.ADMIN))
          .thenReturn(admins);

      // ACT: removing the user from the workspace
      teamService.removeUserFromWorkspace(testWorkspaceMemberId);

      // ASSERT: Verify the user was removed
      verify(workspaceMemberRepository).delete(member);
    }

    @Test
    @DisplayName("throw exception when non-admin tries to remove user")
    void throwExceptionWhenNonAdminRemovesUser() {
      when(securityUtils.isAdmin()).thenReturn(false);

      assertThatThrownBy(() -> teamService.removeUserFromWorkspace(testWorkspaceMemberId))
          .isInstanceOf(AccessDeniedException.class)
          .hasMessage("Only Admins can remove users from workspaces");

      verify(workspaceMemberRepository, never()).delete(any(WorkspaceMember.class));
    }

    @Test
    @DisplayName("throw exception when workspace member not found")
    void throwExceptionWhenWorkspaceMemberNotFound() {

      when(securityUtils.isAdmin()).thenReturn(true);
      when(securityUtils.getCurrentWorkspaceId()).thenReturn(testWorkspaceId);

      when(workspaceMemberRepository.findById(testWorkspaceMemberId)).thenReturn(Optional.empty());

      assertThatThrownBy(() -> teamService.removeUserFromWorkspace(testWorkspaceMemberId))
          .isInstanceOf(ResourceNotFoundException.class)
          .hasMessage("Workspace member not found");

      verify(workspaceMemberRepository, never()).delete(any(WorkspaceMember.class));
    }

    @Test
    @DisplayName("throw exception when member does not belong to workspace")
    void throwExceptionWhenMemberNotInWorkspace() {

      WorkspaceMember member = createTestWorkspaceMember();
      member.setWorkspaceId(UUID.randomUUID());

      when(securityUtils.isAdmin()).thenReturn(true);
      when(securityUtils.getCurrentWorkspaceId()).thenReturn(testWorkspaceId);

      when(workspaceMemberRepository.findById(testWorkspaceMemberId))
          .thenReturn(Optional.of(member));

      assertThatThrownBy(() -> teamService.removeUserFromWorkspace(testWorkspaceMemberId))
          .isInstanceOf(AccessDeniedException.class)
          .hasMessage("Member does not belong to your workspace");

      verify(workspaceMemberRepository, never()).delete(any(WorkspaceMember.class));
    }

    @Test
    @DisplayName("throw exception when trying to remove the last admin")
    void throwExceptionWhenRemovingLastAdmin() {
      WorkspaceMember adminMember = createTestAdminWorkspaceMember();
      adminMember.setId(testWorkspaceMemberId);

      // Only one admin exists (the one we're trying to remove)
      List<WorkspaceMember> admins = List.of(adminMember);

      when(securityUtils.isAdmin()).thenReturn(true);
      when(securityUtils.getCurrentWorkspaceId()).thenReturn(testWorkspaceId);
      when(workspaceMemberRepository.findById(testWorkspaceMemberId))
          .thenReturn(Optional.of(adminMember));
      when(workspaceMemberRepository.findAllByWorkspaceIdAndRole(
              testWorkspaceId, WorkspaceRole.ADMIN))
          .thenReturn(admins);

      assertThatThrownBy(() -> teamService.removeUserFromWorkspace(testWorkspaceMemberId))
          .isInstanceOf(StateConflictException.class)
          .hasMessage("Cannot remove the last Admin from the workspace");

      verify(workspaceMemberRepository, never()).delete(any(WorkspaceMember.class));
    }

    @Test
    @DisplayName("allow removal of non-admin even if there is only one admin")
    void allowRemovalOfNonAdminWhenOnlyOneAdmin() {

      WorkspaceMember member = createTestWorkspaceMember();
      WorkspaceMember adminMember = createTestAdminWorkspaceMember();
      List<WorkspaceMember> admins = List.of(adminMember);

      when(securityUtils.isAdmin()).thenReturn(true);
      when(securityUtils.getCurrentWorkspaceId()).thenReturn(testWorkspaceId);

      when(workspaceMemberRepository.findById(testWorkspaceMemberId))
          .thenReturn(Optional.of(member));
      when(workspaceMemberRepository.findAllByWorkspaceIdAndRole(
              testWorkspaceId, WorkspaceRole.ADMIN))
          .thenReturn(admins);

      // ACT: removing the non-admin user
      teamService.removeUserFromWorkspace(testWorkspaceMemberId);

      // ASSERT: Verify the user was removed
      verify(workspaceMemberRepository).delete(member);
    }
  }

  @Nested
  @DisplayName("Get Available Users Tests")
  class GetAvailableUsersTests {

    @Test
    @DisplayName("return only workspace users for manager")
    void returnOnlyWorkspaceUsersForManager() {

      // ARRANGE: setting up users in the workspace
      User user1 = createTestUser();
      User user2 = createTestUser();
      user2.setId(UUID.randomUUID());
      user2.setEmail("jane@momentum.co.za");

      WorkspaceMember member1 = createTestWorkspaceMember();
      WorkspaceMember member2 = createTestWorkspaceMember();

      member2.setId(UUID.randomUUID());
      member2.setUserId(user2.getId());

      List<WorkspaceMember> workspaceMembers = List.of(member1, member2);
      List<User> usersInWorkspace = List.of(user1, user2);
      List<UUID> userIdsInWorkspace = List.of(user1.getId(), user2.getId());

      when(workspaceRepository.existsById(testWorkspaceId)).thenReturn(true);
      when(workspaceMemberRepository.findByWorkspaceId(testWorkspaceId))
          .thenReturn(workspaceMembers);

      when(userRepository.findAllById(userIdsInWorkspace)).thenReturn(usersInWorkspace);
      when(securityUtils.isAdmin()).thenReturn(false);
      when(securityUtils.isManager()).thenReturn(true);

      // ACT: getting the available users
      List<AvailableUserResponse> responses = teamService.getAvailableUsers(testWorkspaceId);

      // ASSERT: Verify only workspace users are returned with flag true
      assertThat(responses).isNotNull();
      assertThat(responses).hasSize(2);

      for (AvailableUserResponse response : responses) {
        assertThat(response.getIsInWorkspace()).isTrue();
      }

      verify(userRepository, never()).findAll();
    }

    @Test
    @DisplayName("throw exception when workspace does not exist")
    void throwExceptionWhenWorkspaceNotFoundForGetUsers() {

      when(workspaceRepository.existsById(testWorkspaceId)).thenReturn(false);

      assertThatThrownBy(() -> teamService.getAvailableUsers(testWorkspaceId))
          .isInstanceOf(ResourceNotFoundException.class)
          .hasMessage("Workspace not found");
    }

    @Test
    @DisplayName("throw exception when developer tries to get workspace members")
    void throwExceptionWhenDeveloperGetsWorkspaceMembers() {

      // ARRANGE: a developer should not be able to see workspace members
      when(workspaceRepository.existsById(testWorkspaceId)).thenReturn(true);
      when(securityUtils.isAdmin()).thenReturn(false);
      when(securityUtils.isManager()).thenReturn(false);

      assertThatThrownBy(() -> teamService.getAvailableUsers(testWorkspaceId))
          .isInstanceOf(AccessDeniedException.class)
          .hasMessage("Only Admins and Managers can view workspace members");
    }

    @Test
    @DisplayName("return empty list when workspace has no members for manager")
    void returnEmptyListWhenWorkspaceHasNoMembersForManager() {

      // ARRANGE: workspace exists but has no members
      List<WorkspaceMember> emptyMembersList = List.of();

      when(workspaceRepository.existsById(testWorkspaceId)).thenReturn(true);
      when(workspaceMemberRepository.findByWorkspaceId(testWorkspaceId))
          .thenReturn(emptyMembersList);

      when(securityUtils.isAdmin()).thenReturn(false);
      when(securityUtils.isManager()).thenReturn(true);

      // ACT: getting the available users
      List<AvailableUserResponse> responses = teamService.getAvailableUsers(testWorkspaceId);

      // ASSERT: Verify empty list is returned
      assertThat(responses).isNotNull();
      assertThat(responses).isEmpty();

      verify(userRepository, never()).findAll();
    }

    @Test
    @DisplayName("return empty list when workspace has no members for admin")
    void returnAllUsersWhenWorkspaceHasNoMembersForAdmin() {

      // ARRANGE: workspace exists but has no members
      List<WorkspaceMember> emptyMembersList = List.of();
      User user1 = createTestUser();
      User user2 = createTestUser();

      user2.setId(UUID.randomUUID());
      List<User> allUsers = List.of(user1, user2);

      when(workspaceRepository.existsById(testWorkspaceId)).thenReturn(true);

      when(workspaceMemberRepository.findByWorkspaceId(testWorkspaceId))
          .thenReturn(emptyMembersList);
      when(securityUtils.isAdmin()).thenReturn(true);
      when(userRepository.findAll()).thenReturn(allUsers);

      // ACT: getting the available users
      List<AvailableUserResponse> responses = teamService.getAvailableUsers(testWorkspaceId);

      // ASSERT: Verify all users are returned with flag false
      assertThat(responses).isNotNull();
      assertThat(responses).hasSize(2);

      for (AvailableUserResponse response : responses) {
        assertThat(response.getIsInWorkspace()).isFalse();
      }

      verify(userRepository).findAll();
    }
  }
}
