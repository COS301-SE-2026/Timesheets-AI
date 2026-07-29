package timesheets.service;

import static org.assertj.core.api.Assertions.assertThat;
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
}
