package timesheets.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import exception.ResourceNotFoundException;
import exception.StateConflictException;
import exception.UnauthorizedException;
import java.time.LocalDate;
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
import timesheets.domain.Timesheet;
import timesheets.domain.WorkspaceMember;
import timesheets.dto.request.TimesheetRequest;
import timesheets.repository.TimeEntryRepository;
import timesheets.repository.TimesheetRepository;
import timesheets.repository.WorkspaceMemberRepository;
import timesheets.security.SecurityUtils;

@ExtendWith(MockitoExtension.class)
@DisplayName("TimesheetService Unit Tests")
class TimesheetServiceTest {

  @Mock private TimesheetRepository timesheetRepository;
  @Mock private TimeEntryRepository timeEntryRepository;
  @Mock private SecurityUtils securityUtils;
  @Mock private WorkspaceMemberRepository workspaceMemberRepository;

  @InjectMocks private TimesheetService timesheetService;

  private UUID workspaceMemberId;
  private UUID timesheetId;
  private LocalDate periodStart;
  private LocalDate periodEnd;
  private Timesheet timesheet;
  private UUID workspaceId;
  private WorkspaceMember workspaceMember;
  private UUID userId;

  @BeforeEach
  void setUp() {
    workspaceMemberId = UUID.randomUUID();
    timesheetId = UUID.randomUUID();
    workspaceId = UUID.randomUUID();
    periodStart = LocalDate.of(2026, 7, 13); // Monday
    periodEnd = LocalDate.of(2026, 7, 19); // Sunday

    workspaceMember = new WorkspaceMember();
    workspaceMember.setId(workspaceMemberId);
    workspaceMember.setWorkspaceId(workspaceId);
    workspaceMember.setUserId(userId);

    timesheet = new Timesheet();
    timesheet.setId(timesheetId);
    timesheet.setWorkspaceMemberId(workspaceMemberId);
    timesheet.setPeriodStart(periodStart);
    timesheet.setPeriodEnd(periodEnd);
    timesheet.setStatus("DRAFT");
    timesheet.setIsLocked(false);
    timesheet.setCreatedAt(LocalDateTime.now());
  }

  @Nested
  @DisplayName("Get Or Create Timesheet Tests")
  class GetOrCreateTimesheetTests {

    @Test
    @DisplayName("returns an existing timesheet")
    void getOrCreateTimesheet() {

      // ARRANGE: a user already has a timesheet for a specific week
      // meant to simulate a logged in user making a request
      when(securityUtils.getDefaultWorkspaceMemberId()).thenReturn(workspaceMemberId);

      // simulating a timesheet already existing
      when(timesheetRepository.findByWorkspaceMemberIdAndPeriodStartAndPeriodEnd(
              workspaceMemberId, periodStart, periodEnd))
          .thenReturn(Optional.of(timesheet));

      // ACT: calls the actual function
      Timesheet result = timesheetService.getOrCreateTimesheet(periodStart, periodEnd);

      // ASSERT: checking the expected data
      assertThat(result).isNotNull(); // did we get something back?
      assertThat(result.getId()).isEqualTo(timesheetId); // exact timesheet back
      assertThat(result.getWorkspaceMemberId()).isEqualTo(workspaceMemberId);

      // verifying ranges
      assertThat(result.getPeriodStart()).isEqualTo(periodStart);
      assertThat(result.getPeriodEnd()).isEqualTo(periodEnd);

      // making sure the deafult values are being handled
      assertThat(result.getStatus()).isEqualTo("DRAFT");
      assertThat(result.getIsLocked()).isFalse();

      verify(timesheetRepository)
          .findByWorkspaceMemberIdAndPeriodStartAndPeriodEnd(
              workspaceMemberId, periodStart, periodEnd);

      // making sure that the save was not called, if it was, duplicate timesheets are created
      verify(timesheetRepository)
          .findByWorkspaceMemberIdAndPeriodStartAndPeriodEnd(
              workspaceMemberId, periodStart, periodEnd);
    }
  }

  @Nested
  @DisplayName("Get Or Create Current Timesheet Tests")
  class GetOrCreateCurrentTimesheetTests {

    @Test
    @DisplayName("returns existing current week's timesheet")
    void getOrCreateCurrentTimesheet() {

      // ARRANGE: a timesheet already exists, it should return that existing timesheet
      LocalDate today = LocalDate.now();
      LocalDate monday = today.with(java.time.DayOfWeek.MONDAY);
      LocalDate sunday = today.with(java.time.DayOfWeek.SUNDAY);

      when(securityUtils.getDefaultWorkspaceMemberId()).thenReturn(workspaceMemberId);

      // fins the existing timesheets and should return it
      when(timesheetRepository.findByWorkspaceMemberIdAndPeriodStartAndPeriodEnd(
              workspaceMemberId, monday, sunday))
          .thenReturn(Optional.of(timesheet));

      // ACT: calling the actual function
      Timesheet result = timesheetService.getOrCreateCurrentTimesheet();

      /*
      ASSERT
      - the timesheet should be returned, not null
      - the repo should find it
      - there should be not save call, since this is just getting */
      assertThat(result).isNotNull();
      assertThat(result.getId()).isEqualTo(timesheetId);

      verify(timesheetRepository, times(1))
          .findByWorkspaceMemberIdAndPeriodStartAndPeriodEnd(workspaceMemberId, monday, sunday);
      verify(timesheetRepository, never()).save(any());
    }

    @Test
    @DisplayName("creates timesheet for the current week when it does not exist")
    void getOrCreateCurrentTimesheetCreate() {

      /*
      ARRANGE
      - no timesheet exists for the current week
      - the system has to create a new draft timesheets
      - just indicating when a new timesheet should range from
       */
      LocalDate today = LocalDate.now();
      LocalDate monday = today.with(java.time.DayOfWeek.MONDAY);
      LocalDate sunday = today.with(java.time.DayOfWeek.SUNDAY);

      when(securityUtils.getDefaultWorkspaceMemberId()).thenReturn(workspaceMemberId);
      when(timesheetRepository.findByWorkspaceMemberIdAndPeriodStartAndPeriodEnd(
              workspaceMemberId, monday, sunday))
          .thenReturn(Optional.empty());
      // saved is mocked, since when a new timesheet is created it should be saved as well
      when(timesheetRepository.save(any(Timesheet.class)))
          .thenAnswer(invocation -> invocation.getArgument(0));

      // ACT
      Timesheet result = timesheetService.getOrCreateCurrentTimesheet();

      // ASSERT
      assertThat(result).isNotNull();
      assertThat(result.getWorkspaceMemberId()).isEqualTo(workspaceMemberId);
      assertThat(result.getPeriodStart()).isEqualTo(monday);
      assertThat(result.getPeriodEnd()).isEqualTo(sunday);
      assertThat(result.getStatus()).isEqualTo("DRAFT"); // the status should be in drafts
      assertThat(result.getIsLocked()).isFalse();

      verify(timesheetRepository, times(1))
          .findByWorkspaceMemberIdAndPeriodStartAndPeriodEnd(workspaceMemberId, monday, sunday);
      verify(timesheetRepository, times(1)).save(any(Timesheet.class));
    }
  }

  @Nested
  @DisplayName("Get Timesheet by ID Tests")
  class GetTimesheetByIdTests {
    @Test
    @DisplayName("returns timesheet when it exists")
    void getTimesheetByIdExists() {

      // mocking a dev who owns the timesheet
      when(securityUtils.getDefaultWorkspaceMemberId()).thenReturn(workspaceMemberId);
      when(securityUtils.getCurrentWorkspaceId()).thenReturn(workspaceId);
      when(securityUtils.isAdmin()).thenReturn(false);
      when(securityUtils.isManager()).thenReturn(false);

      // ARRANGE: a timesheet exists in the database, so looks for this timesheet in theDB
      when(timesheetRepository.findById(timesheetId)).thenReturn(Optional.of(timesheet));

      // ACT: call the actual method
      Timesheet result = timesheetService.getTimesheetById(timesheetId);

      // ASSERT: finds the exact timesheet, and calls repo only once
      assertThat(result).isNotNull();
      assertThat(result.getId()).isEqualTo(timesheetId);
      verify(timesheetRepository, times(1)).findById(timesheetId);
    }

    @Test
    @DisplayName("throws exception when timesheet does not exist")
    void getTimesheetByIdNotExist() {
      /*
      ARRANGE
      - similar to if a user tries to access a timesheet that does not exist
      - ideally UI should not allow this, but the edges cases should be prevented in backend
      */
      when(timesheetRepository.findById(timesheetId)).thenReturn(Optional.empty());

      // ACT & ASSERT: it throws an exception, and a clear message
      assertThatThrownBy(() -> timesheetService.getTimesheetById(timesheetId))
          .isInstanceOf(ResourceNotFoundException.class)
          .hasMessageContaining("Timesheet not found with id:");

      // checking that the repository got called, and that the DB did not skip it
      verify(timesheetRepository, times(1)).findById(timesheetId);
    }
  }

  @Nested
  @DisplayName("Get Timesheets by member Tests")
  class GetTimesheetsByMemberTests {

    @Test
    @DisplayName("returns list of timesheets for a member")
    void getTimesheetsByMemberTimesheetList() {

      // mocking a developer who owns the timesheet
      when(securityUtils.getDefaultWorkspaceMemberId()).thenReturn(workspaceMemberId);
      when(securityUtils.isAdmin()).thenReturn(false);
      when(securityUtils.isManager()).thenReturn(false);

      // this will simulate what a DB returns, a list of timesheets
      List<Timesheet> expectedTimesheets = List.of(timesheet);

      // mocks a repo to return the timesheet list
      when(timesheetRepository.findByWorkspaceMemberId(workspaceMemberId))
          .thenReturn(expectedTimesheets);

      // ACT
      List<Timesheet> result = timesheetService.getTimesheetsByMember(workspaceMemberId);

      /*
      ASSERT
      - the list should have the one timesheet I mocked
      - need to make sure that the timesheet is the one we expect
       */
      assertThat(result).isNotNull();
      assertThat(result).hasSize(1);
      assertThat(result.get(0).getId()).isEqualTo(timesheetId);

      // the repo should only be called once
      verify(timesheetRepository, times(1)).findByWorkspaceMemberId(workspaceMemberId);
    }

    @Test
    @DisplayName("empty list returned when the user has not timesheets")
    void getTimesheetsByMemberEmptyList() {

      // mocking a dev who owns the timesheet
      when(securityUtils.getDefaultWorkspaceMemberId()).thenReturn(workspaceMemberId);
      when(securityUtils.isAdmin()).thenReturn(false);
      when(securityUtils.isManager()).thenReturn(false);

      // because you know nulls give unexpected results, so we need empty lists not null things

      // ARRANGE: mocking the repo returning an empty list
      when(timesheetRepository.findByWorkspaceMemberId(workspaceMemberId)).thenReturn(List.of());

      // ACT: calls the actual actual method
      List<Timesheet> result = timesheetService.getTimesheetsByMember(workspaceMemberId);

      /*
      ASSERT
      - checking that it is not null, and empty list does not mean null
      - making sure the repo is only called once
      */
      assertThat(result).isNotNull();
      assertThat(result).isEmpty();

      verify(timesheetRepository, times(1)).findByWorkspaceMemberId(workspaceMemberId);
    }
  }

  @Nested
  @DisplayName("Create Timesheet Tests")
  class CreateTimesheetTests {

    @Test
    @DisplayName("creates a new timesheet successfully")
    void createTimesheet() {
      /*
      ARRANGE
      - simulating a user creating a timesheet for the week
      - the timesheet is created for a certain start and end date
       */
      TimesheetRequest request = new TimesheetRequest();
      request.setPeriodStart(periodStart);
      request.setPeriodEnd(periodEnd);

      when(securityUtils.getDefaultWorkspaceMemberId()).thenReturn(workspaceMemberId);
      when(timesheetRepository.save(any(Timesheet.class)))
          .thenAnswer(invocation -> invocation.getArgument(0));

      // ACT: calls the actual create timesheet function
      Timesheet result = timesheetService.createTimesheet(request);

      // ASSERT: making sure that all the details I expect match
      assertThat(result).isNotNull();
      assertThat(result.getWorkspaceMemberId()).isEqualTo(workspaceMemberId);
      assertThat(result.getPeriodStart()).isEqualTo(periodStart);
      assertThat(result.getPeriodEnd()).isEqualTo(periodEnd);
      assertThat(result.getStatus()).isEqualTo("DRAFT"); // should be a draft
      assertThat(result.getIsLocked()).isFalse();

      verify(timesheetRepository, times(1)).save(any(Timesheet.class));
    }
  }

  @Nested
  @DisplayName("Submit Timesheet Tests")
  class SubmitTimesheetTests {

    @Test
    @DisplayName("submits a draft timesheet successfully")
    void submitTimesheet() {

      when(securityUtils.getDefaultWorkspaceMemberId()).thenReturn(workspaceMemberId);
      when(securityUtils.getCurrentWorkspaceId()).thenReturn(workspaceId);

      when(workspaceMemberRepository.findById(workspaceMemberId))
          .thenReturn(Optional.of(workspaceMember));

      // ARRANGE
      // should return the test user
      when(securityUtils.getDefaultWorkspaceMemberId()).thenReturn(workspaceMemberId);
      when(timesheetRepository.findById(timesheetId)).thenReturn(Optional.of(timesheet));

      // to simulate saving we just return the same object
      when(timesheetRepository.save(any(Timesheet.class)))
          .thenAnswer(invocation -> invocation.getArgument(0));

      // ACT: calling the method, to simulate handling the action
      Timesheet result = timesheetService.submitTimesheet(timesheetId);

      /*
      - I am expecting that:
      - the timsheet exists
      - the status has changed from draft to submitted
      - the timesheet gets locked
      - that the locked at is recorded, so just making sure that it is not null
      - that submitted at is recorded */
      assertThat(result).isNotNull();
      assertThat(result.getStatus()).isEqualTo("SUBMITTED");
      assertThat(result.getIsLocked()).isTrue();
      assertThat(result.getLockedAt()).isNotNull();
      assertThat(result.getSubmittedAt()).isNotNull();

      // want to make sure that the repo got called, and timesheet was fetched
      verify(timesheetRepository, times(1)).findById(timesheetId);
      verify(timesheetRepository, times(1)).save(any(Timesheet.class));
    }

    @Test
    @DisplayName("throw exception when one tries to submit another user timesheet")
    void submitTimesheetOfAnotherUser() {
      /*
      ARRANGE
      - a user tries to submit someone elses timesheet
      - mocking to make it that the logged in user is not the owner of the timesheet
      - ideally on the UI side a user would not even see other peoples timesheets, but the security needs to be there
      */
      UUID differentUser = UUID.randomUUID();
      when(securityUtils.getDefaultWorkspaceMemberId()).thenReturn(differentUser);
      when(securityUtils.getCurrentWorkspaceId()).thenReturn(workspaceId);

      when(timesheetRepository.findById(timesheetId)).thenReturn(Optional.of(timesheet));

      // ACT and ASSERT
      assertThatThrownBy(() -> timesheetService.submitTimesheet(timesheetId))
          .isInstanceOf(UnauthorizedException.class)
          .hasMessage("You can only submit your own timesheets");

      verify(timesheetRepository, times(1)).findById(timesheetId);
      // making sure that nothing got saved, because it should not be saved
      verify(timesheetRepository, never()).save(any());

      verify(workspaceMemberRepository, never()).findById(any());
    }

    @Test
    @DisplayName("throws exception when trying to submit a timesheet that is no longer a draft")
    void submitTimesheetNotDraft() {

      // ARRANGE: mocking a timesheet that is already submitted
      timesheet.setStatus("SUBMITTED");
      when(securityUtils.getDefaultWorkspaceMemberId()).thenReturn(workspaceMemberId);

      when(securityUtils.getCurrentWorkspaceId()).thenReturn(workspaceId);
      when(workspaceMemberRepository.findById(workspaceMemberId))
          .thenReturn(Optional.of(workspaceMember));

      when(timesheetRepository.findById(timesheetId)).thenReturn(Optional.of(timesheet));

      /*
      ACT and ASSERT
      - an exception should be thrown
      - the repo should be called so the timesheet can be found
      - the timesheet did not get saved
       */
      assertThatThrownBy(() -> timesheetService.submitTimesheet(timesheetId))
          .isInstanceOf(StateConflictException.class)
          .hasMessage("Timesheet has already been submitted");

      verify(timesheetRepository, times(1)).findById(timesheetId);
      verify(timesheetRepository, never()).save(any());
    }

    @Test
    @DisplayName("throws exception when timesheet does not exist")
    void submitTimesheetNotFound() {

      // ARRANGE: when someone tries to submit a timesheet with an invalid ID
      when(securityUtils.getDefaultWorkspaceMemberId()).thenReturn(workspaceMemberId);
      when(timesheetRepository.findById(timesheetId)).thenReturn(Optional.empty());

      // ACT and ASSERT: an exception should be thrown
      assertThatThrownBy(() -> timesheetService.submitTimesheet(timesheetId))
          .isInstanceOf(ResourceNotFoundException.class)
          .hasMessageContaining("Timesheet not found with id: ");

      verify(timesheetRepository, times(1)).findById(timesheetId);
      verify(timesheetRepository, never())
          .save(any()); // should not be saved since the operation is not valid
    }
  }

  @Nested
  @DisplayName("Reject Timesheet Tests")
  class RejectTimesheetTests {

    @Test
    @DisplayName("rejects a timesheet that is submitted") // should reject successfully
    void rejectTimesheet() {

      // ARRANGE: having a random reviewer, and a reason
      UUID reviewerId = UUID.randomUUID();
      String rejectionReason = "Docs missing";

      // simulating a submitted timesheet, remember that the timesheet gets locked
      timesheet.setStatus("SUBMITTED");
      timesheet.setIsLocked(true);

      when(securityUtils.isManager()).thenReturn(true);
      when(securityUtils.getCurrentWorkspaceId()).thenReturn(workspaceId);
      when(workspaceMemberRepository.findById(workspaceMemberId))
          .thenReturn(Optional.of(workspaceMember));

      // basically returning a mock timesheet, as if the actual repository was called
      when(timesheetRepository.findById(timesheetId)).thenReturn(Optional.of(timesheet));

      when(timesheetRepository.save(any(Timesheet.class)))
          .thenAnswer(invocation -> invocation.getArgument(0));

      // ACT: calling actual reject timesheet function
      Timesheet result = timesheetService.rejectTimesheet(timesheetId, reviewerId, rejectionReason);

      /*
      ASSERT:
      - need to check that the timesheet is returned
      - that the status changes to rejected
      - that it gets unlocked
      - that the locked_at is no longer there- previously changed this to remove ambiguity
      - it has a rejection reason
      */
      assertThat(result).isNotNull();
      assertThat(result.getStatus()).isEqualTo("REJECTED");
      assertThat(result.getIsLocked()).isFalse();
      assertThat(result.getLockedAt()).isNull();
      assertThat(result.getRejectedAt()).isNotNull();
      assertThat(result.getApprovedByWorkspaceMemberId()).isEqualTo(reviewerId);
      assertThat(result.getRejectionReason()).isEqualTo(rejectionReason);

      // those checks about making sure that the repository was called once
      verify(timesheetRepository, times(1)).findById(timesheetId);
      verify(timesheetRepository, times(1)).save(any(Timesheet.class));
    }

    @Test
    @DisplayName("throw exception when a dev tries to reject")
    void rejectTimesheetByDev() {

      // ARRANGE
      UUID reviewerId = UUID.randomUUID();
      String rejectionReason = "Missing docs";

      // the person trying to reject the timesheets is neither a manager or an admin
      when(securityUtils.isAdmin()).thenReturn(false);
      when(securityUtils.isManager()).thenReturn(false);

      // ACT and ASSERT
      assertThatThrownBy(
              () -> timesheetService.rejectTimesheet(timesheetId, reviewerId, rejectionReason))
          .isInstanceOf(UnauthorizedException.class)
          .hasMessage("Only Admins and Managers can reject timesheets");

      verify(timesheetRepository, never()).findById(any());
      verify(timesheetRepository, never()).save(any());
    }

    @Test
    @DisplayName("throws exception when user rejects their own timesheet")
    void rejectTimesheetOwnTimesheet() {

      /*
      ARRANGE
      - remember that a user is not allowed to reject their own timesheet
      - setting the reviewer id to be the same as the workspace member id
      - even if they are an admin they cannot rejet their own timesheet
       */
      UUID reviewerId = workspaceMemberId;
      String rejectionReason = "Missing docs";

      timesheet.setStatus("SUBMITTED");

      when(securityUtils.getCurrentWorkspaceId()).thenReturn(workspaceId);
      when(securityUtils.isAdmin()).thenReturn(true);

      when(workspaceMemberRepository.findById(workspaceMemberId))
          .thenReturn(Optional.of(workspaceMember));

      when(timesheetRepository.findById(timesheetId)).thenReturn(Optional.of(timesheet));

      // ACT and ASSERT
      assertThatThrownBy(
              () -> timesheetService.rejectTimesheet(timesheetId, reviewerId, rejectionReason))
          .isInstanceOf(StateConflictException.class)
          .hasMessage("You cannot reject your own timesheet");

      // need to make sure that the repo is only called once and that that is not saved, because it
      // is not allowed
      verify(timesheetRepository, times(1)).findById(timesheetId);
      verify(timesheetRepository, never()).save(any());
    }
  }

  @Nested
  @DisplayName("Approve Timesheet Tests")
  class ApproveTimesheetTests {

    @Test
    @DisplayName("approves a timesheet that is submitted") // should approve successfully
    void approveTimesheet() {

      // ARRANGE: having a random reviewer
      UUID reviewerId = UUID.randomUUID();

      // simulating a submitted timesheet, remember that the timesheet gets locked
      timesheet.setStatus("SUBMITTED");
      timesheet.setIsLocked(true);

      when(securityUtils.isManager()).thenReturn(true);
      when(securityUtils.getCurrentWorkspaceId()).thenReturn(workspaceId);

      when(workspaceMemberRepository.findById(workspaceMemberId))
          .thenReturn(Optional.of(workspaceMember));

      // basically returning a mock timesheet, as if the actual repository was called
      when(timesheetRepository.findById(timesheetId)).thenReturn(Optional.of(timesheet));
      when(timesheetRepository.save(any(Timesheet.class)))
          .thenAnswer(invocation -> invocation.getArgument(0));

      // ACT: calling actual reject timesheet function
      Timesheet result = timesheetService.approveTimesheet(timesheetId, reviewerId);

      /*
      ASSERT:
      - need to check that the timesheet is returned
      - that the status changes to accepted
      - that it stays as locked
      */
      assertThat(result).isNotNull();
      assertThat(result.getStatus()).isEqualTo("APPROVED");
      assertThat(result.getIsLocked()).isTrue();
      assertThat(result.getLockedAt()).isNotNull();
      assertThat(result.getApprovedAt()).isNotNull();
      assertThat(result.getRejectedAt()).isNull();
      assertThat(result.getApprovedByWorkspaceMemberId()).isEqualTo(reviewerId);

      // those checks about making sure that the repository was called once
      verify(timesheetRepository, times(1)).findById(timesheetId);
      verify(timesheetRepository, times(1)).save(any(Timesheet.class));
    }

    @Test
    @DisplayName("throws exception when a user approves their own timesheet")
    void approveTimesheetOwnTimesheet() {

      // ARRANGE: making sure that the reviewer is the same as the timesheet owner
      UUID reviewerId = workspaceMemberId;
      timesheet.setStatus("SUBMITTED");

      when(securityUtils.isAdmin()).thenReturn(true);
      when(securityUtils.getCurrentWorkspaceId()).thenReturn(workspaceId);

      when(workspaceMemberRepository.findById(workspaceMemberId))
          .thenReturn(Optional.of(workspaceMember));

      when(timesheetRepository.findById(timesheetId)).thenReturn(Optional.of(timesheet));

      // ACT and ASSERT
      assertThatThrownBy(() -> timesheetService.approveTimesheet(timesheetId, reviewerId))
          .isInstanceOf(StateConflictException.class)
          .hasMessage("You cannot approve your own timesheet");

      // making sure that it did not get saved
      verify(timesheetRepository, times(1)).findById(timesheetId);
      verify(timesheetRepository, never()).save(any());
    }
  }

  @Nested
  @DisplayName("Helper Functions Tests")
  class HelperFunctionsTests {
    // tests here as well
  }
}
