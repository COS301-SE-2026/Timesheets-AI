package timesheets.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.DayOfWeek;
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
import timesheets.domain.TimeEntry;
import timesheets.domain.Timesheet;
import timesheets.dto.request.TimeEntryRequest;
import timesheets.dto.response.TimeEntryResponse;
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

  // ! helper so that I can just keep reusing it with default values for testing
  private TimeEntry createTimeEntry(UUID id, UUID memberId) {
    TimeEntry entry = new TimeEntry();
    entry.setId(id);
    entry.setWorkspaceMemberId(memberId);
    entry.setTimesheetId(UUID.randomUUID());
    entry.setProjectId(projectId);
    entry.setTaskId(taskId);
    entry.setStartTime(LocalDateTime.now().minusHours(2));
    entry.setEndTime(LocalDateTime.now());
    entry.setDurationSeconds(7200);
    entry.setEntryType("MANUAL");
    entry.setDescription("Test entry");
    entry.setIsLocked(false);
    entry.setIsDeleted(false);
    entry.setCreatedAt(LocalDateTime.now());
    entry.setUpdatedAt(LocalDateTime.now());
    return entry;
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

      // ARRANGE: this creates the entries using the helper method I defined
      UUID workspaceMemberId = UUID.randomUUID();
      TimeEntry entry1 = createTimeEntry(UUID.randomUUID(), workspaceMemberId);
      TimeEntry entry2 = createTimeEntry(UUID.randomUUID(), workspaceMemberId);
      List<TimeEntry> expectedEntries = List.of(entry1, entry2);

      when(securityUtils.getDefaultWorkspaceMemberId()).thenReturn(workspaceMemberId);
      when(timeEntryRepository.findByWorkspaceMemberIdOrderByStartTimeDesc(workspaceMemberId))
          .thenReturn(expectedEntries);

      // ACT
      List<TimeEntry> result = timeEntryService.getMyTimeEntries();

      // ASSERT: the entries should be what we expect, not null, exactly 2 entries, and they match
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
    @Test
    @DisplayName("returns time entry when it exists")
    void getTimeEntryById() {

      /*
      ARRANGE
      - when a time entry exists in the database
      - teh service should give it back
       */
      UUID timeEntryId = UUID.randomUUID();
      TimeEntry entry = createTimeEntry(timeEntryId, workspaceMemberId);

      when(timeEntryRepository.findById(timeEntryId)).thenReturn(Optional.of(entry));

      // ACT
      TimeEntry result = timeEntryService.getTimeEntryById(timeEntryId);

      // ASSERT: making sure that the entry is found and returned
      assertThat(result).isNotNull();
      assertThat(result.getId()).isEqualTo(timeEntryId);

      // making sure that the repo is called once and with the correct id
      verify(timeEntryRepository, times(1)).findById(timeEntryId);
    }
  }

  @Nested
  @DisplayName("Delete Time Entry Tests")
  class DeleteTimeEntryTests {

    @Test
    @DisplayName("this should soft delete a time entry")
    void deleteTimeEntry() {
      /*
      ARRANGE
      - this is for when user owns a time entry that is not locked
      - since this is a soft delete the isDeleted column is flagged
      - the entry is hidden from normal view
       */
      UUID timeEntryId = UUID.randomUUID();
      TimeEntry entry = createTimeEntry(timeEntryId, workspaceMemberId);
      entry.setIsLocked(false);

      when(securityUtils.getDefaultWorkspaceMemberId()).thenReturn(workspaceMemberId);
      when(timeEntryRepository.findById(timeEntryId)).thenReturn(Optional.of(entry));

      // just simulating a DB save
      when(timeEntryRepository.save(any(TimeEntry.class)))
          .thenAnswer(invocation -> invocation.getArgument(0));

      // ACT: calling the actual delete onn the time entry
      timeEntryService.deleteTimeEntry(timeEntryId);

      /*
      ASSERT
      - we need to make sure that the record is not removed from the DB
      - that the deletedAt field is not null
      - I need to also make sure that the entry is got based on the id
      - the entry gets saved when the deleted flag is set to
      */
      assertThat(entry.getIsDeleted()).isTrue();
      assertThat(entry.getDeletedAt()).isNotNull();

      verify(timeEntryRepository, times(1)).findById(timeEntryId);
      verify(timeEntryRepository, times(1)).save(entry);
    }
  }

  @Nested
  @DisplayName("Update Time Entry Tests")
  class UpdateTimeEntryTests {
    @Test
    @DisplayName("updates a time entry successfully")
    void updateTimeEntry() {
      /*
      ARRANGE
      - a user owns a time entry that is not locked
      - the user should be able to update all the fields in the time entry
       */
      UUID timeEntryId = UUID.randomUUID();
      TimeEntry existingEntry = createTimeEntry(timeEntryId, workspaceMemberId);
      existingEntry.setIsLocked(false);
      existingEntry.setDescription("Old description");

      // below will now be a new request with the updated values
      TimeEntryRequest updateRequest = new TimeEntryRequest();
      updateRequest.setProjectId(UUID.randomUUID());
      updateRequest.setTaskId(UUID.randomUUID());
      updateRequest.setStartTime(LocalDateTime.now().minusHours(2));
      updateRequest.setEndTime(LocalDateTime.now());
      updateRequest.setDurationSeconds(7200);
      updateRequest.setEntryType("MANUAL");
      updateRequest.setDescription("Updated description");

      when(securityUtils.getDefaultWorkspaceMemberId()).thenReturn(workspaceMemberId);
      when(timeEntryRepository.findById(timeEntryId)).thenReturn(Optional.of(existingEntry));

      // this should just simulate a DB save
      when(timeEntryRepository.save(any(TimeEntry.class)))
          .thenAnswer(invocation -> invocation.getArgument(0));

      // ACT
      TimeEntry result = timeEntryService.updateTimeEntry(timeEntryId, updateRequest);

      // ASSERT: making sure that all fields have been updated
      assertThat(result).isNotNull();
      assertThat(result.getProjectId()).isEqualTo(updateRequest.getProjectId());
      assertThat(result.getTaskId()).isEqualTo(updateRequest.getTaskId());
      assertThat(result.getStartTime()).isEqualTo(updateRequest.getStartTime());
      assertThat(result.getEndTime()).isEqualTo(updateRequest.getEndTime());
      assertThat(result.getDurationSeconds()).isEqualTo(updateRequest.getDurationSeconds());
      assertThat(result.getDescription()).isEqualTo("Updated description");

      // also making sure that the entry was fetched and the entry was updated
      verify(timeEntryRepository, times(1)).findById(timeEntryId);
      verify(timeEntryRepository, times(1)).save(any(TimeEntry.class));
    }
  }

  @Nested
  @DisplayName("Get Entries By Timesheet Tests")
  class GetEntriesByTimesheetTests {
    @Test
    @DisplayName("returns all time entries for a timesheet")
    void getEntriesByTimesheetGetList() {
      /*
      ARRANGE
      - a timesheet has time entries
      - gets all the entries for a specific timesheet
       */
      UUID timesheetId = UUID.randomUUID();

      TimeEntry entry1 = createTimeEntry(UUID.randomUUID(), workspaceMemberId);
      entry1.setTimesheetId(timesheetId);

      TimeEntry entry2 = createTimeEntry(UUID.randomUUID(), workspaceMemberId);
      entry2.setTimesheetId(timesheetId);

      List<TimeEntry> expectedEntries = List.of(entry1, entry2);

      when(timeEntryRepository.findByTimesheetId(timesheetId)).thenReturn(expectedEntries);

      /*
      ACT
      - the user will request all entries for a timesheet
      - the method should get all the entries from the repo
       */
      List<TimeEntryResponse> result = timeEntryService.getEntriesByTimesheet(timesheetId);

      // ASSERT
      assertThat(result).isNotNull();
      assertThat(result).hasSize(2); // expecting the 2 entries we want
      assertThat(result.get(0).getId()).isEqualTo(entry1.getId());
      assertThat(result.get(1).getId()).isEqualTo(entry2.getId());

      // checking that theres dtos and not just entities
      assertThat(result.get(0)).isInstanceOf(TimeEntryResponse.class);

      verify(timeEntryRepository, times(1)).findByTimesheetId(timesheetId);
    }
  }
}
