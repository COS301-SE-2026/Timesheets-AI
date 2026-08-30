package timesheets.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import exception.AccessDeniedException;
import exception.BadRequestException;
import exception.ResourceNotFoundException;
import exception.StateConflictException;
import java.math.BigDecimal;
import java.time.LocalDate;
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
import timesheets.domain.LeaveRequest;
import timesheets.domain.WorkspaceMember;
import timesheets.dto.request.LeaveRequestRequest;
import timesheets.dto.response.LeaveRequestResponse;
import timesheets.enums.WorkspaceRole;
import timesheets.repository.LeaveRequestRepository;
import timesheets.repository.UserRepository;
import timesheets.repository.WorkspaceMemberRepository;
import timesheets.security.SecurityUtils;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("LeaveRequestService Unit Tests")
public class LeaveRequestTest {

  @Mock private SecurityUtils securityUtils;
  @Mock private WorkspaceMemberRepository workspaceMemberRepository;
  @Mock private LeaveRequestRepository leaveRequestRepository;
  @Mock private UserRepository userRepository;

  @InjectMocks private LeaveRequestService leaveRequestService;

  private final UUID testUserId = UUID.randomUUID();
  private final UUID testWorkspaceMemberId = UUID.randomUUID();
  private final UUID testRequestId = UUID.randomUUID();
  private final UUID testWorkspaceId = UUID.randomUUID();
  private final LocalDate testStartDate = LocalDate.now().plusDays(1);
  private final LocalDate testEndDate = LocalDate.now().plusDays(3);
  private final BigDecimal testTotalDays = BigDecimal.valueOf(3);
  private final String testLeaveType = "ANNUAL";
  private final String testReason = "Family vacation";

  private WorkspaceMember createTestWorkspaceMember() {

    WorkspaceMember member = new WorkspaceMember();

    member.setId(testWorkspaceMemberId);
    member.setUserId(testUserId);
    member.setWorkspaceId(testWorkspaceId);
    member.setRole(WorkspaceRole.DEVELOPER);
    member.setCreatedAt(LocalDateTime.now());

    return member;
  }

  private LeaveRequest createTestLeaveRequest() {

    LeaveRequest leaveRequest = new LeaveRequest();
    leaveRequest.setId(testRequestId);
    leaveRequest.setWorkspaceMemberId(testWorkspaceMemberId);
    leaveRequest.setLeaveType(testLeaveType);
    leaveRequest.setStartDate(testStartDate);
    leaveRequest.setEndDate(testEndDate);
    leaveRequest.setTotalDays(testTotalDays);
    leaveRequest.setReason(testReason);
    leaveRequest.setAttachments("[\"attachment1.pdf\"]");
    leaveRequest.setStatus("PENDING");
    leaveRequest.setCreatedAt(LocalDateTime.now());
    leaveRequest.setUpdatedAt(LocalDateTime.now());

    return leaveRequest;
  }

  private LeaveRequest createApprovedLeaveRequest() {

    LeaveRequest leaveRequest = createTestLeaveRequest();
    leaveRequest.setStatus("APPROVED");
    leaveRequest.setApprovedByWorkspaceMemberId(UUID.randomUUID());
    leaveRequest.setApprovedAt(LocalDateTime.now());

    return leaveRequest;
  }

  private LeaveRequestRequest.Create createValidCreateRequest() {
    LeaveRequestRequest.Create request = new LeaveRequestRequest.Create();

    request.setLeaveType(testLeaveType);
    request.setStartDate(testStartDate);
    request.setEndDate(testEndDate);
    request.setTotalDays(testTotalDays);
    request.setReason(testReason);
    request.setAttachments("[\"attachment1.pdf\"]");
    return request;
  }

  private LeaveRequestRequest.Update createValidUpdateRequest() {
    LeaveRequestRequest.Update request = new LeaveRequestRequest.Update();

    request.setLeaveType("SICK");
    request.setStartDate(testStartDate.plusDays(5));
    request.setEndDate(testEndDate.plusDays(5));
    request.setTotalDays(BigDecimal.valueOf(2));
    request.setReason("Doctor's appointment");
    request.setAttachments("[\"attachment1.pdf\"]");

    return request;
  }

  private WorkspaceMember createTestAdminWorkspaceMember() {
    WorkspaceMember member = createTestWorkspaceMember();
    member.setRole(WorkspaceRole.ADMIN);

    return member;
  }

  @Nested
  @DisplayName("Create Leave Request Tests")
  class CreateLeaveRequestTests {

    @Test
    @DisplayName("create leave request successfully")
    void createLeaveRequestSuccessfully() {

      // ARRANGE: setup leave request creation
      LeaveRequestRequest.Create request = createValidCreateRequest();
      LeaveRequest savedLeaveRequest = createTestLeaveRequest();

      when(securityUtils.getDefaultWorkspaceMemberId()).thenReturn(testWorkspaceMemberId);
      when(workspaceMemberRepository.findById(testWorkspaceMemberId))
          .thenReturn(Optional.of(createTestWorkspaceMember()));

      when(leaveRequestRepository.hasOverlappingApprovedLeave(
              testWorkspaceMemberId, testEndDate, testStartDate))
          .thenReturn(false);
      when(leaveRequestRepository.save(any(LeaveRequest.class))).thenReturn(savedLeaveRequest);

      // ACT: create the leave request
      LeaveRequestResponse response = leaveRequestService.createLeaveRequest(request);

      // ASSERT: verify leave request was created
      assertThat(response).isNotNull();
      assertThat(response.getLeaveType()).isEqualTo(testLeaveType);
      assertThat(response.getStartDate()).isEqualTo(testStartDate);
      assertThat(response.getEndDate()).isEqualTo(testEndDate);
      assertThat(response.getTotalDays()).isEqualTo(testTotalDays);
      assertThat(response.getReason()).isEqualTo(testReason);
      assertThat(response.getStatus()).isEqualTo("PENDING");

      verify(leaveRequestRepository).save(any(LeaveRequest.class));
    }

    @Test
    @DisplayName("throw exception when workspace member not found")
    void throwExceptionWhenWorkspaceMemberNotFound() {

      LeaveRequestRequest.Create request = createValidCreateRequest();

      when(securityUtils.getDefaultWorkspaceMemberId()).thenReturn(testWorkspaceMemberId);
      when(workspaceMemberRepository.findById(testWorkspaceMemberId)).thenReturn(Optional.empty());

      assertThatThrownBy(() -> leaveRequestService.createLeaveRequest(request))
          .isInstanceOf(ResourceNotFoundException.class)
          .hasMessage("Workspace member not found");

      verify(leaveRequestRepository, never()).save(any(LeaveRequest.class));
    }

    @Test
    @DisplayName("throw exception when overlapping approved leave exists")
    void throwExceptionWhenOverlappingLeaveExists() {

      LeaveRequestRequest.Create request = createValidCreateRequest();

      when(securityUtils.getDefaultWorkspaceMemberId()).thenReturn(testWorkspaceMemberId);
      when(workspaceMemberRepository.findById(testWorkspaceMemberId))
          .thenReturn(Optional.of(createTestWorkspaceMember()));

      when(leaveRequestRepository.hasOverlappingApprovedLeave(
              testWorkspaceMemberId, testEndDate, testStartDate))
          .thenReturn(true);

      assertThatThrownBy(() -> leaveRequestService.createLeaveRequest(request))
          .isInstanceOf(StateConflictException.class)
          .hasMessage("You already have an approved leave that overlaps with these dates");

      verify(leaveRequestRepository, never()).save(any(LeaveRequest.class));
    }
  }

  @Nested
  @DisplayName("Update Leave Request Tests")
  class UpdateLeaveRequestTests {

    @Test
    @DisplayName("update leave request successfully")
    void updateLeaveRequestSuccessfully() {

      // ARRANGE: setup the leave request update
      LeaveRequest existingLeaveRequest = createTestLeaveRequest();
      LeaveRequestRequest.Update request = createValidUpdateRequest();

      when(leaveRequestRepository.findById(testRequestId))
          .thenReturn(Optional.of(existingLeaveRequest));
      when(securityUtils.getDefaultWorkspaceMemberId()).thenReturn(testWorkspaceMemberId);
      when(leaveRequestRepository.save(any(LeaveRequest.class))).thenReturn(existingLeaveRequest);

      // ACT: update the leave request
      LeaveRequestResponse response =
          leaveRequestService.updateLeaveRequest(testRequestId, request);

      // ASSERT: verify the fields were updated
      assertThat(response).isNotNull();
      assertThat(response.getLeaveType()).isEqualTo("SICK");
      assertThat(response.getStartDate()).isEqualTo(testStartDate.plusDays(5));
      assertThat(response.getEndDate()).isEqualTo(testEndDate.plusDays(5));
      assertThat(response.getTotalDays()).isEqualTo(BigDecimal.valueOf(2));
      assertThat(response.getReason()).isEqualTo("Doctor's appointment");

      verify(leaveRequestRepository).save(any(LeaveRequest.class));
    }

    @Test
    @DisplayName("throw exception when leave request not found")
    void throwExceptionWhenLeaveRequestNotFound() {

      LeaveRequestRequest.Update request = createValidUpdateRequest();

      when(leaveRequestRepository.findById(testRequestId)).thenReturn(Optional.empty());

      assertThatThrownBy(() -> leaveRequestService.updateLeaveRequest(testRequestId, request))
          .isInstanceOf(ResourceNotFoundException.class)
          .hasMessage("Leave request not found");

      verify(leaveRequestRepository, never()).save(any(LeaveRequest.class));
    }

    @Test
    @DisplayName("throw exception when updating non-pending leave request")
    void throwExceptionWhenUpdatingNonPendingLeaveRequest() {

      LeaveRequest approvedLeaveRequest = createApprovedLeaveRequest();
      LeaveRequestRequest.Update request = createValidUpdateRequest();

      when(leaveRequestRepository.findById(testRequestId))
          .thenReturn(Optional.of(approvedLeaveRequest));

      assertThatThrownBy(() -> leaveRequestService.updateLeaveRequest(testRequestId, request))
          .isInstanceOf(StateConflictException.class)
          .hasMessage("Only pending leave requests can be updated");

      verify(leaveRequestRepository, never()).save(any(LeaveRequest.class));
    }

    @Test
    @DisplayName("throw exception when user tries to update someone else's request")
    void throwExceptionWhenUpdatingOthersRequest() {

      LeaveRequest existingLeaveRequest = createTestLeaveRequest();
      LeaveRequestRequest.Update request = createValidUpdateRequest();
      UUID otherMemberId = UUID.randomUUID();

      when(leaveRequestRepository.findById(testRequestId))
          .thenReturn(Optional.of(existingLeaveRequest));
      when(securityUtils.getDefaultWorkspaceMemberId()).thenReturn(otherMemberId);

      assertThatThrownBy(() -> leaveRequestService.updateLeaveRequest(testRequestId, request))
          .isInstanceOf(AccessDeniedException.class)
          .hasMessage("You can only update your own leave requests");

      verify(leaveRequestRepository, never()).save(any(LeaveRequest.class));
    }

    @Test
    @DisplayName("throw exception when end date is before start date")
    void throwExceptionWhenEndDateBeforeStartDate() {

      LeaveRequest existingLeaveRequest = createTestLeaveRequest();
      LeaveRequestRequest.Update request = createValidUpdateRequest();

      request.setStartDate(testEndDate.plusDays(5));
      request.setEndDate(testStartDate.plusDays(1));

      when(leaveRequestRepository.findById(testRequestId))
          .thenReturn(Optional.of(existingLeaveRequest));
      when(securityUtils.getDefaultWorkspaceMemberId()).thenReturn(testWorkspaceMemberId);

      assertThatThrownBy(() -> leaveRequestService.updateLeaveRequest(testRequestId, request))
          .isInstanceOf(BadRequestException.class)
          .hasMessage("End date cannot be before start date");

      verify(leaveRequestRepository, never()).save(any(LeaveRequest.class));
    }

    @Test
    @DisplayName("update only provided fields")
    void updateOnlyProvidedFields() {

      // ARRANGE: setup with only some fields updated
      LeaveRequest existingLeaveRequest = createTestLeaveRequest();
      LeaveRequestRequest.Update request = new LeaveRequestRequest.Update();

      request.setLeaveType("SICK");
      // all the other fields are null

      when(leaveRequestRepository.findById(testRequestId))
          .thenReturn(Optional.of(existingLeaveRequest));
      when(securityUtils.getDefaultWorkspaceMemberId()).thenReturn(testWorkspaceMemberId);
      when(leaveRequestRepository.save(any(LeaveRequest.class))).thenReturn(existingLeaveRequest);

      // ACT: update the leave request
      LeaveRequestResponse response =
          leaveRequestService.updateLeaveRequest(testRequestId, request);

      // ASSERT: only the leave type should change
      assertThat(response.getLeaveType()).isEqualTo("SICK");
      assertThat(response.getStartDate()).isEqualTo(testStartDate);
      assertThat(response.getEndDate()).isEqualTo(testEndDate);
      assertThat(response.getTotalDays()).isEqualTo(testTotalDays);
      assertThat(response.getReason()).isEqualTo(testReason);

      verify(leaveRequestRepository).save(any(LeaveRequest.class));
    }
  }

  @Nested
  @DisplayName("Approve Leave Request Tests")
  class ApproveLeaveRequestTests {

    @Test
    @DisplayName("approve leave request successfully")
    void approveLeaveRequestSuccessfully() {

      // ARRANGE: setup the approval
      LeaveRequest pendingRequest = createTestLeaveRequest();
      WorkspaceMember adminMember = createTestAdminWorkspaceMember();

      UUID adminMemberId = UUID.randomUUID();

      when(securityUtils.getDefaultWorkspaceMemberId()).thenReturn(adminMemberId);
      when(securityUtils.isAdmin()).thenReturn(true);

      when(leaveRequestRepository.findById(testRequestId)).thenReturn(Optional.of(pendingRequest));
      when(leaveRequestRepository.save(any(LeaveRequest.class))).thenReturn(pendingRequest);

      // ACT: approve the leave request
      LeaveRequestResponse response = leaveRequestService.approveLeaveRequest(testRequestId);

      // ASSERT: verify the approval
      assertThat(response).isNotNull();
      assertThat(response.getStatus()).isEqualTo("APPROVED");
      assertThat(response.getApprovedByName()).isNotNull();

      verify(leaveRequestRepository).save(any(LeaveRequest.class));
    }

    @Test
    @DisplayName("throw exception when developer tries to approve")
    void throwExceptionWhenDeveloperApproves() {

      when(securityUtils.isAdmin()).thenReturn(false);
      when(securityUtils.isManager()).thenReturn(false);

      assertThatThrownBy(() -> leaveRequestService.approveLeaveRequest(testRequestId))
          .isInstanceOf(AccessDeniedException.class)
          .hasMessage("Only Admins and Managers can approve leave requests");

      verify(leaveRequestRepository, never()).save(any(LeaveRequest.class));
    }

    @Test
    @DisplayName("throw exception when approving non-pending request")
    void throwExceptionWhenApprovingNonPendingRequest() {

      LeaveRequest approvedRequest = createApprovedLeaveRequest();

      when(securityUtils.isAdmin()).thenReturn(true);
      when(leaveRequestRepository.findById(testRequestId)).thenReturn(Optional.of(approvedRequest));

      assertThatThrownBy(() -> leaveRequestService.approveLeaveRequest(testRequestId))
          .isInstanceOf(StateConflictException.class)
          .hasMessage("Leave request is not pending approval");

      verify(leaveRequestRepository, never()).save(any(LeaveRequest.class));
    }

    @Test
    @DisplayName("throw exception when approving own request")
    void throwExceptionWhenApprovingOwnRequest() {

      LeaveRequest ownRequest = createTestLeaveRequest();

      when(securityUtils.getDefaultWorkspaceMemberId()).thenReturn(testWorkspaceMemberId);
      when(securityUtils.isAdmin()).thenReturn(true);
      when(leaveRequestRepository.findById(testRequestId)).thenReturn(Optional.of(ownRequest));

      assertThatThrownBy(() -> leaveRequestService.approveLeaveRequest(testRequestId))
          .isInstanceOf(StateConflictException.class)
          .hasMessage("You cannot approve your own leave request");

      verify(leaveRequestRepository, never()).save(any(LeaveRequest.class));
    }

    @Test
    @DisplayName("manager can only approve requests from their workspace")
    void managerCanOnlyApproveRequestsFromWorkspace() {

      // ARRANGE: setup as manager with different workspace
      LeaveRequest pendingRequest = createTestLeaveRequest();

      UUID differentWorkspaceId = UUID.randomUUID();
      UUID managerMemberId = UUID.randomUUID();

      when(securityUtils.getDefaultWorkspaceMemberId()).thenReturn(managerMemberId);
      when(securityUtils.isManager()).thenReturn(true);
      when(securityUtils.isAdmin()).thenReturn(false);

      when(securityUtils.getCurrentWorkspaceId()).thenReturn(differentWorkspaceId);
      when(leaveRequestRepository.findById(testRequestId)).thenReturn(Optional.of(pendingRequest));
      when(leaveRequestRepository.findByWorkspaceId(differentWorkspaceId))
          .thenReturn(List.of()); // no requests in this workspace

      assertThatThrownBy(() -> leaveRequestService.approveLeaveRequest(testRequestId))
          .isInstanceOf(AccessDeniedException.class)
          .hasMessage("You can only approve requests from your workspace");

      verify(leaveRequestRepository, never()).save(any(LeaveRequest.class));
    }

    @Test
    @DisplayName("throw exception when leave request not found for approval")
    void throwExceptionWhenLeaveRequestNotFoundForApproval() {

      when(securityUtils.isAdmin()).thenReturn(true);
      when(leaveRequestRepository.findById(testRequestId)).thenReturn(Optional.empty());

      assertThatThrownBy(() -> leaveRequestService.approveLeaveRequest(testRequestId))
          .isInstanceOf(ResourceNotFoundException.class)
          .hasMessage("Leave request not found");
    }
  }

  @Nested
  @DisplayName("Reject Leave Request Tests")
  class RejectLeaveRequestTests {

    @Test
    @DisplayName("reject leave request successfully")
    void rejectLeaveRequestSuccessfully() {

      // ARRANGE: setup the rejection
      LeaveRequest pendingRequest = createTestLeaveRequest();
      String rejectionReason = "Insufficient leave balance";

      UUID adminMemberId = UUID.randomUUID();

      when(securityUtils.getDefaultWorkspaceMemberId()).thenReturn(adminMemberId);
      when(securityUtils.isAdmin()).thenReturn(true);

      when(leaveRequestRepository.findById(testRequestId)).thenReturn(Optional.of(pendingRequest));
      when(leaveRequestRepository.save(any(LeaveRequest.class))).thenReturn(pendingRequest);

      // ACT: reject the leave request
      LeaveRequestResponse response =
          leaveRequestService.rejectLeaveRequest(testRequestId, rejectionReason);

      // ASSERT: verify the rejection
      assertThat(response).isNotNull();
      assertThat(response.getStatus()).isEqualTo("REJECTED");
      assertThat(response.getRejectionReason()).isEqualTo(rejectionReason);

      verify(leaveRequestRepository).save(any(LeaveRequest.class));
    }

    @Test
    @DisplayName("throw exception when developer tries to reject")
    void throwExceptionWhenDeveloperRejects() {

      when(securityUtils.isAdmin()).thenReturn(false);
      when(securityUtils.isManager()).thenReturn(false);

      assertThatThrownBy(() -> leaveRequestService.rejectLeaveRequest(testRequestId, "Reason"))
          .isInstanceOf(AccessDeniedException.class)
          .hasMessage("Only Admins and Managers can reject leave requests");

      verify(leaveRequestRepository, never()).save(any(LeaveRequest.class));
    }

    @Test
    @DisplayName("throw exception when rejecting non-pending request")
    void throwExceptionWhenRejectingNonPendingRequest() {

      LeaveRequest approvedRequest = createApprovedLeaveRequest();

      when(securityUtils.isAdmin()).thenReturn(true);
      when(leaveRequestRepository.findById(testRequestId)).thenReturn(Optional.of(approvedRequest));

      assertThatThrownBy(() -> leaveRequestService.rejectLeaveRequest(testRequestId, "Reason"))
          .isInstanceOf(StateConflictException.class)
          .hasMessage("Leave request is not pending approval");

      verify(leaveRequestRepository, never()).save(any(LeaveRequest.class));
    }

    @Test
    @DisplayName("throw exception when rejecting own request")
    void throwExceptionWhenRejectingOwnRequest() {

      LeaveRequest ownRequest = createTestLeaveRequest();

      when(securityUtils.getDefaultWorkspaceMemberId()).thenReturn(testWorkspaceMemberId);
      when(securityUtils.isAdmin()).thenReturn(true);
      when(leaveRequestRepository.findById(testRequestId)).thenReturn(Optional.of(ownRequest));

      assertThatThrownBy(() -> leaveRequestService.rejectLeaveRequest(testRequestId, "Reason"))
          .isInstanceOf(StateConflictException.class)
          .hasMessage("You cannot reject your own leave request");

      verify(leaveRequestRepository, never()).save(any(LeaveRequest.class));
    }

    @Test
    @DisplayName("manager can only reject requests from their workspace")
    void managerCanOnlyRejectRequestsFromWorkspace() {

      // ARRANGE: setup as manager with different workspace
      LeaveRequest pendingRequest = createTestLeaveRequest();

      UUID managerMemberId = UUID.randomUUID();
      UUID differentWorkspaceId = UUID.randomUUID();

      when(securityUtils.getDefaultWorkspaceMemberId()).thenReturn(managerMemberId);
      when(securityUtils.isManager()).thenReturn(true);
      when(securityUtils.isAdmin()).thenReturn(false);
      when(securityUtils.getCurrentWorkspaceId()).thenReturn(differentWorkspaceId);

      when(leaveRequestRepository.findById(testRequestId)).thenReturn(Optional.of(pendingRequest));
      when(leaveRequestRepository.findByWorkspaceId(differentWorkspaceId)).thenReturn(List.of());

      assertThatThrownBy(() -> leaveRequestService.rejectLeaveRequest(testRequestId, "Reason"))
          .isInstanceOf(AccessDeniedException.class)
          .hasMessage("You can only reject requests from your workspace");

      verify(leaveRequestRepository, never()).save(any(LeaveRequest.class));
    }

    @Test
    @DisplayName("throw exception when leave request not found for rejection")
    void throwExceptionWhenLeaveRequestNotFoundForRejection() {

      when(securityUtils.isAdmin()).thenReturn(true);
      when(leaveRequestRepository.findById(testRequestId)).thenReturn(Optional.empty());

      assertThatThrownBy(() -> leaveRequestService.rejectLeaveRequest(testRequestId, "Reason"))
          .isInstanceOf(ResourceNotFoundException.class)
          .hasMessage("Leave request not found");
    }
  }

  @Nested
  @DisplayName("Cancel Leave Request Tests")
  class CancelLeaveRequestTests {

    @Test
    @DisplayName("cancel leave request successfully")
    void cancelLeaveRequestSuccessfully() {

      // ARRANGE: setup the cancellation
      LeaveRequest pendingRequest = createTestLeaveRequest();

      when(leaveRequestRepository.findById(testRequestId)).thenReturn(Optional.of(pendingRequest));

      when(securityUtils.getDefaultWorkspaceMemberId()).thenReturn(testWorkspaceMemberId);
      when(leaveRequestRepository.save(any(LeaveRequest.class))).thenReturn(pendingRequest);

      // ACT: cancel the leave request
      LeaveRequestResponse response =
          leaveRequestService.cancelLeaveRequest(testRequestId, "Changed plans");

      // ASSERT: verify the cancellation
      assertThat(response).isNotNull();
      assertThat(response.getStatus()).isEqualTo("CANCELLED");

      verify(leaveRequestRepository).save(any(LeaveRequest.class));
    }

    @Test
    @DisplayName("throw exception when user tries to cancel someone else's request")
    void throwExceptionWhenCancellingOthersRequest() {

      LeaveRequest pendingRequest = createTestLeaveRequest();
      UUID otherMemberId = UUID.randomUUID();

      when(leaveRequestRepository.findById(testRequestId)).thenReturn(Optional.of(pendingRequest));
      when(securityUtils.getDefaultWorkspaceMemberId()).thenReturn(otherMemberId);

      assertThatThrownBy(() -> leaveRequestService.cancelLeaveRequest(testRequestId, "Reason"))
          .isInstanceOf(AccessDeniedException.class)
          .hasMessage("Only the requester can cancel their own leave request");

      verify(leaveRequestRepository, never()).save(any(LeaveRequest.class));
    }

    @Test
    @DisplayName("throw exception when cancelling non-pending request")
    void throwExceptionWhenCancellingNonPendingRequest() {

      LeaveRequest approvedRequest = createApprovedLeaveRequest();

      when(leaveRequestRepository.findById(testRequestId)).thenReturn(Optional.of(approvedRequest));
      when(securityUtils.getDefaultWorkspaceMemberId()).thenReturn(testWorkspaceMemberId);

      assertThatThrownBy(() -> leaveRequestService.cancelLeaveRequest(testRequestId, "Reason"))
          .isInstanceOf(StateConflictException.class)
          .hasMessage("Only pending leave requests can be cancelled");

      verify(leaveRequestRepository, never()).save(any(LeaveRequest.class));
    }

    @Test
    @DisplayName("throw exception when leave request not found for cancellation")
    void throwExceptionWhenLeaveRequestNotFoundForCancellation() {

      when(leaveRequestRepository.findById(testRequestId)).thenReturn(Optional.empty());

      assertThatThrownBy(() -> leaveRequestService.cancelLeaveRequest(testRequestId, "Reason"))
          .isInstanceOf(ResourceNotFoundException.class)
          .hasMessage("Leave request not found");

      verify(leaveRequestRepository, never()).save(any(LeaveRequest.class));
    }
  }
}
