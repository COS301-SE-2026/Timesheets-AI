package timesheets.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
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
import timesheets.dto.request.TimesheetRequest;
import timesheets.repository.TimeEntryRepository;
import timesheets.repository.TimesheetRepository;
import timesheets.security.SecurityUtils;

@ExtendWith(MockitoExtension.class)
@DisplayName("TimesheetService Unit Tests")
class TimesheetServiceTest {

  @Mock private TimesheetRepository timesheetRepository;
  @Mock private TimeEntryRepository timeEntryRepository;
  @Mock private SecurityUtils securityUtils;

  @InjectMocks private TimesheetService timesheetService;

  private UUID workspaceMemberId;
  private UUID timesheetId;
  private LocalDate periodStart;
  private LocalDate periodEnd;
  private Timesheet timesheet;

  @BeforeEach
  void setUp() {
    workspaceMemberId = UUID.randomUUID();
    timesheetId = UUID.randomUUID();
    periodStart = LocalDate.of(2026, 7, 13); // Monday
    periodEnd = LocalDate.of(2026, 7, 19); // Sunday

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
          .isInstanceOf(RuntimeException.class)
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

      when(securityUtils.isAdmin()).thenReturn(true);
      when(timesheetRepository.findById(timesheetId)).thenReturn(Optional.of(timesheet));

      // ACT and ASSERT
      assertThatThrownBy(
              () -> timesheetService.rejectTimesheet(timesheetId, reviewerId, rejectionReason))
          .isInstanceOf(RuntimeException.class)
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
      when(timesheetRepository.findById(timesheetId)).thenReturn(Optional.of(timesheet));

      // ACT and ASSERT
      assertThatThrownBy(() -> timesheetService.approveTimesheet(timesheetId, reviewerId))
          .isInstanceOf(RuntimeException.class)
          .hasMessage("You cannot approve your own timesheet");

      // making sure that it did not get saved
      verify(timesheetRepository, times(1)).findById(timesheetId);
      verify(timesheetRepository, never()).save(any());
    }
  }
}
