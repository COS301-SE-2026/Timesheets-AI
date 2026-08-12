package timesheets.service;

import exception.TimeEntryAccessDeniedException;
import exception.TimeEntryNotFoundException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import timesheets.domain.TimeEntry;
import timesheets.domain.Timesheet;
import timesheets.dto.request.TimeEntryRequest;
import timesheets.dto.response.TimeEntryResponse;
import timesheets.repository.TimeEntryRepository;
import timesheets.security.SecurityUtils;

// this is the file that has all my business logic, the control will call the service and the
// service will call repo

@Service
@RequiredArgsConstructor
public class TimeEntryService {

  private final TimeEntryRepository timeEntryRepository;
  private final TimesheetService timesheetService;
  private final SecurityUtils securityUtils;

  // this would be if they need to create a time entry manually
  @Transactional
  public TimeEntry createTimeEntry(TimeEntryRequest request) {

    UUID workspaceMemberId = securityUtils.getDefaultWorkspaceMemberId();

    LocalDate entryDate = request.getStartTime().toLocalDate();

    // calculate the weeks start and end
    LocalDate weekStart = entryDate.with(DayOfWeek.MONDAY);
    LocalDate weekEnd = entryDate.with(DayOfWeek.SUNDAY);

    // to get or create timesheet for this period
    Timesheet timesheet = timesheetService.getOrCreateTimesheet(weekStart, weekEnd);

    TimeEntry entry = new TimeEntry();

    entry.setWorkspaceMemberId(workspaceMemberId);
    entry.setTimesheetId(
        timesheet.getId()); // I want it to automatically be assigned to a Timesheet
    entry.setProjectId(request.getProjectId());
    entry.setTaskId(request.getTaskId());
    entry.setStartTime(request.getStartTime());
    entry.setEndTime(request.getEndTime());
    entry.setDurationSeconds(request.getDurationSeconds());
    entry.setEntryType(
        request
            .getEntryType()); // this will show if a time entry was manual or started with a timer
    entry.setDescription(request.getDescription());
    entry.setIsLocked(false);

    return timeEntryRepository.save(entry);
  }

  // to get all the time entries of a particular workspace member
  public List<TimeEntry> getMyTimeEntries() {

    UUID workspaceMemberId = securityUtils.getDefaultWorkspaceMemberId();

    return timeEntryRepository.findByWorkspaceMemberIdOrderByStartTimeDesc(workspaceMemberId);
  }

  // can get a time entry using the id
  public TimeEntry getTimeEntryById(UUID id) {
    return timeEntryRepository.findById(id).orElseThrow(() -> new TimeEntryNotFoundException(id));
  }

  @Transactional
  public void deleteTimeEntry(UUID id) {
    UUID workspaceMemberId = securityUtils.getDefaultWorkspaceMemberId();

    TimeEntry entry = getTimeEntryById(id);

    if (Boolean.TRUE.equals(entry.getIsLocked())) {
      throw new RuntimeException("Cannot delete a locked time entry");
    }

    if (!entry.getWorkspaceMemberId().equals(workspaceMemberId)) {
      throw new TimeEntryAccessDeniedException("You can only delete your own time entries");
    }

    // remember we are only doing soft deletes
    entry.setIsDeleted(true);
    entry.setDeletedAt(LocalDateTime.now());

    timeEntryRepository.save(entry);
  }

  /*
  - this will allow a user to edit time entries as long as if they are not locked
  - as per the client request both manual and timer, time entries are both editable
  */
  @Transactional
  public TimeEntry updateTimeEntry(UUID id, TimeEntryRequest request) {

    UUID workspaceMemberId = securityUtils.getDefaultWorkspaceMemberId();

    TimeEntry entry = getTimeEntryById(id);

    if (Boolean.TRUE.equals(entry.getIsLocked())) {
      throw new RuntimeException("Cannot edit a locked time entry");
    }

    if (!entry.getWorkspaceMemberId().equals(workspaceMemberId)) {
      throw new TimeEntryAccessDeniedException("You can only edit your own time entries");
    }

    entry.setProjectId(request.getProjectId());
    entry.setTaskId(request.getTaskId());
    entry.setStartTime(request.getStartTime());
    entry.setEndTime(request.getEndTime());
    entry.setDurationSeconds(request.getDurationSeconds());
    entry.setDescription(request.getDescription());

    return timeEntryRepository.save(entry);
  }

  // gets the time entries that all belong to a specific timesheet
  public List<TimeEntryResponse> getEntriesByTimesheet(UUID timesheetId) {
    List<TimeEntry> entries = timeEntryRepository.findByTimesheetId(timesheetId);

    return entries.stream().map(TimeEntryResponse::from).collect(Collectors.toList());
  }
}
