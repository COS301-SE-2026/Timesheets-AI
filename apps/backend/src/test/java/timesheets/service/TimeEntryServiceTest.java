package timesheets.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import timesheets.domain.TimeEntry;
import timesheets.domain.Timesheet;
import timesheets.dto.request.TimeEntryRequest;
import timesheets.repository.TimeEntryRepository;
import timesheets.security.SecurityUtils;

@ExtendWith(MockitoExtension.class)
@DisplayName("TimeEntryService Unit Tests")
class TimeEntryServiceTest {

  @Mock private TimeEntryRepository timeEntryRepository;
  @Mock private TimesheetService timesheetService;
  @Mock private SecurityUtils securityUtils;

  @InjectMocks private TimeEntryService timeEntryService;

  private UUID workspaceMemberId;
  private UUID timesheetId;
  private UUID projectId;
  private UUID taskId;
  private LocalDateTime startTime;
  private LocalDateTime endTime;
  private TimeEntryRequest request;
  private Timesheet timesheet;

  @BeforeEach
  void setUp() {
    workspaceMemberId = UUID.randomUUID();
    timesheetId = UUID.randomUUID();
    projectId = UUID.randomUUID();
    taskId = UUID.randomUUID();
    startTime = LocalDateTime.of(2026, 7, 15, 9, 0); // 15 July at 09:00
    endTime = LocalDateTime.of(2026, 7, 15, 17, 0);

    request = new TimeEntryRequest();
    request.setProjectId(projectId);
    request.setTaskId(taskId);
    request.setStartTime(startTime);
    request.setEndTime(endTime);
    request.setDurationSeconds(28800); // 8 hours for a day
    request.setEntryType("MANUAL");
    request.setDescription("Working on test project");

    timesheet = new Timesheet();
    timesheet.setId(timesheetId);
  }

  @Test
  @DisplayName("create time entry successfully")
  void createTimeEntry() {

    // ARRANGE: setting up mocks
    when(securityUtils.getDefaultWorkspaceMemberId()).thenReturn(workspaceMemberId);

    // mocking the week starting from Monday to Sunday
    LocalDate entryDate = startTime.toLocalDate();
    LocalDate weekStart = entryDate.with(DayOfWeek.MONDAY);
    LocalDate weekEnd = entryDate.with(DayOfWeek.SUNDAY);

    when(timesheetService.getOrCreateTimesheet(weekStart, weekEnd)).thenReturn(timesheet);

    // mocking the save operation
    when(timeEntryRepository.save(any(TimeEntry.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    /*
    - ACT:calling the method to create a time entry
    - remember that the createTimeEntry:
    - gets or creates a timesheet
    - creates a new time entry with the data from the request
    - saves the time entry to the DB
    */

    TimeEntry result = timeEntryService.createTimeEntry(request);

    // ASSERT: check that the time entry was created with the right data
    assertThat(result).isNotNull();
    assertThat(result.getWorkspaceMemberId()).isEqualTo(workspaceMemberId);
    assertThat(result.getTimesheetId()).isEqualTo(timesheetId);
    assertThat(result.getProjectId()).isEqualTo(projectId);
    assertThat(result.getTaskId()).isEqualTo(taskId);
    assertThat(result.getStartTime()).isEqualTo(startTime);
    assertThat(result.getEndTime()).isEqualTo(endTime);
    assertThat(result.getDurationSeconds()).isEqualTo(28800);
    assertThat(result.getEntryType()).isEqualTo("MANUAL");
    assertThat(result.getDescription()).isEqualTo("Working on test project");
    assertThat(result.getIsLocked()).isFalse();

    // make sure that the save function was called
    verify(timeEntryRepository).save(any(TimeEntry.class));
  }

  @Nested
  @DisplayName("Get My Time Entries Tests")
  class GetMyTimeEntriesTests {

    @Test
    @DisplayName("returns all time entries for user that is signed in")
    void getMyTimeEntriesList() {
        /*
        ARRANGE
        - 
         */
        UUID workspaceMemberId = UUID.randomUUID();
        TimeEntry entry1 = createTimeEntry(UUID.randomUUID(), workspaceMemberId);
        TimeEntry entry2 = createTimeEntry(UUID.randomUUID(), workspaceMemberId);
        List<TimeEntry> expectedEntries = List.of(entry1, entry2);

        when(securityUtils.getDefaultWorkspaceMemberId()).thenReturn(workspaceMemberId);
        when(timeEntryRepository.findByWorkspaceMemberIdOrderByStartTimeDesc(workspaceMemberId))
            .thenReturn(expectedEntries);

        // ACT
        List<TimeEntry> result = timeEntryService.getMyTimeEntries();

        // ASSERT
        assertThat(result).isNotNull();
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getId()).isEqualTo(entry1.getId());
        assertThat(result.get(1).getId()).isEqualTo(entry2.getId());

        verify(timeEntryRepository, times(1))
            .findByWorkspaceMemberIdOrderByStartTimeDesc(workspaceMemberId);
    }
  }

  @Nested
  @DisplayName("Get Time Entry by ID Tests")
  class GetTimeEntryByIdTests {
    //my tests her
  }

  @Nested
  @DisplayName("Delete Time Entry Tests")
  class DeleteTimeEntryTests {
    //my tests here
  }

  @Nested
  @DisplayName("Update Time Entry Tests")
  class UpdateTimeEntryTests {
    //my tests here
  }

  @Nested
  @DisplayName("Get Entries By Timesheet Tests")
  class GetEntriesByTimesheetTests {
    //my tests here
  }
}
