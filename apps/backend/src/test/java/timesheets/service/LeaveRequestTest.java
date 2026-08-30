package timesheets.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import exception.ResourceNotFoundException;
import exception.StateConflictException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
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
}
