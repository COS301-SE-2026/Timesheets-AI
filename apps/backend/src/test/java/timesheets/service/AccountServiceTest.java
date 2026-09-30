package timesheets.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import exception.ResourceNotFoundException;
import exception.StateConflictException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
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
import timesheets.dto.request.AccountDeletionRequest;
import timesheets.enums.WorkspaceRole;
import timesheets.repository.UserRepository;
import timesheets.repository.WorkspaceMemberRepository;
import timesheets.security.SecurityUtils;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("AccountService Unit Tests")
class AccountServiceTest {

  @Mock private SecurityUtils securityUtils;
  @Mock private UserRepository userRepository;
  @Mock private WorkspaceMemberRepository workspaceMemberRepository;

  @InjectMocks private AccountService accountService;

  private final UUID userId = UUID.randomUUID();
  private User user;
  private AccountDeletionRequest.Request request;

  private WorkspaceMember member(UUID memberUserId, WorkspaceRole role, UUID workspaceId) {
    WorkspaceMember m = mock(WorkspaceMember.class);
    when(m.getUserId()).thenReturn(memberUserId);
    when(m.getRole()).thenReturn(role);
    when(m.getWorkspaceId()).thenReturn(workspaceId);
    return m;
  }

  @BeforeEach
  void setUp() {
    user = mock(User.class);
    when(user.getEmail()).thenReturn("john@momentum.co.za");
    request = mock(AccountDeletionRequest.Request.class);
    when(request.getReason()).thenReturn("leaving");
    when(securityUtils.getCurrentUserId()).thenReturn(userId);
    when(userRepository.findById(userId)).thenReturn(Optional.of(user));
    when(workspaceMemberRepository.findAll()).thenReturn(List.of());
    when(workspaceMemberRepository.findByUserId(userId)).thenReturn(List.of());
  }

  @Nested
  @DisplayName("requestDeletion")
  class RequestDeletionTests {

    @Test
    @DisplayName("should mark the deletion request")
    void marksRequest() {
      accountService.requestDeletion(request);

      verify(userRepository).requestDeletion(eq(userId), any(LocalDateTime.class), eq("leaving"));
    }

    @Test
    @DisplayName("should throw when the user does not exist")
    void userMissing() {
      when(userRepository.findById(userId)).thenReturn(Optional.empty());

      assertThatThrownBy(() -> accountService.requestDeletion(request))
          .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("should throw when deletion was already requested")
    void alreadyRequested() {
      when(user.getDeletionRequestedAt()).thenReturn(LocalDateTime.now());

      assertThatThrownBy(() -> accountService.requestDeletion(request))
          .isInstanceOf(StateConflictException.class)
          .hasMessageContaining("already requested");
      verify(userRepository, never()).requestDeletion(any(), any(), any());
    }

    @Test
    @DisplayName("should throw when deletion was already processed")
    void alreadyProcessed() {
      when(user.getDeletionProcessedAt()).thenReturn(LocalDateTime.now());

      assertThatThrownBy(() -> accountService.requestDeletion(request))
          .isInstanceOf(StateConflictException.class)
          .hasMessageContaining("already been processed");
    }

    @Test
    @DisplayName("should block the only system admin")
    void onlyAdmin() {
      WorkspaceMember admin = member(userId, WorkspaceRole.ADMIN, UUID.randomUUID());
      when(workspaceMemberRepository.findAll()).thenReturn(List.of(admin));

      assertThatThrownBy(() -> accountService.requestDeletion(request))
          .isInstanceOf(StateConflictException.class)
          .hasMessageContaining("only system administrator");
    }

    @Test
    @DisplayName("should allow an admin when another admin exists")
    void adminWithAnotherAdmin() {
      WorkspaceMember me = member(userId, WorkspaceRole.ADMIN, UUID.randomUUID());
      WorkspaceMember other = member(UUID.randomUUID(), WorkspaceRole.ADMIN, UUID.randomUUID());
      when(workspaceMemberRepository.findAll()).thenReturn(List.of(me, other));

      accountService.requestDeletion(request);

      verify(userRepository).requestDeletion(eq(userId), any(), any());
    }

    @Test
    @DisplayName("should block the only manager of a workspace")
    void onlyManager() {
      UUID workspaceId = UUID.randomUUID();
      WorkspaceMember me = member(userId, WorkspaceRole.MANAGER, workspaceId);
      when(workspaceMemberRepository.findByUserId(userId)).thenReturn(List.of(me));
      when(workspaceMemberRepository.findAllByWorkspaceIdAndRole(
              workspaceId, WorkspaceRole.MANAGER))
          .thenReturn(List.of(me));

      assertThatThrownBy(() -> accountService.requestDeletion(request))
          .isInstanceOf(StateConflictException.class)
          .hasMessageContaining("only manager");
    }

    @Test
    @DisplayName("should allow a manager when another manager exists")
    void managerWithAnotherManager() {
      UUID workspaceId = UUID.randomUUID();
      WorkspaceMember me = member(userId, WorkspaceRole.MANAGER, workspaceId);
      WorkspaceMember other = member(UUID.randomUUID(), WorkspaceRole.MANAGER, workspaceId);
      when(workspaceMemberRepository.findByUserId(userId)).thenReturn(List.of(me));
      when(workspaceMemberRepository.findAllByWorkspaceIdAndRole(
              workspaceId, WorkspaceRole.MANAGER))
          .thenReturn(List.of(me, other));

      accountService.requestDeletion(request);

      verify(userRepository).requestDeletion(eq(userId), any(), any());
    }

    @Test
    @DisplayName("should ignore workspaces where the user is not a manager")
    void ignoresNonManagerRoles() {
      WorkspaceMember dev = member(userId, WorkspaceRole.DEVELOPER, UUID.randomUUID());
      when(workspaceMemberRepository.findByUserId(userId)).thenReturn(List.of(dev));

      accountService.requestDeletion(request);

      verify(workspaceMemberRepository, never()).findAllByWorkspaceIdAndRole(any(), any());
    }
  }
}
