package timesheets.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import timesheets.domain.TimeEntry;
import timesheets.domain.Timesheet;
import timesheets.dto.request.TimesheetRequest;
import timesheets.repository.TimeEntryRepository;
import timesheets.repository.TimesheetRepository;
import timesheets.security.SecurityUtils;

@Service
@RequiredArgsConstructor
public class TimesheetService {

  private final TimesheetRepository timesheetRepository;
  private final TimeEntryRepository timeEntryRepository;
  private final SecurityUtils securityUtils;

  @Transactional
  public Timesheet createTimesheet(TimesheetRequest request) {

    UUID workspaceMemberId = securityUtils.getDefaultWorkspaceMemberId();

    Timesheet timesheet = new Timesheet();
    timesheet.setWorkspaceMemberId(workspaceMemberId);
    timesheet.setPeriodStart(request.getPeriodStart());
    timesheet.setPeriodEnd(request.getPeriodEnd());
    timesheet.setStatus("DRAFT");
    timesheet.setIsLocked(false);

    return timesheetRepository.save(timesheet);
  }

  // get existing timesheet or create a new one
  @Transactional
  public Timesheet getOrCreateTimesheet(LocalDate periodStart, LocalDate periodEnd) {

    UUID workspaceMemberId = securityUtils.getDefaultWorkspaceMemberId();

    return timesheetRepository
        .findByWorkspaceMemberIdAndPeriodStartAndPeriodEnd(
            workspaceMemberId, periodStart, periodEnd)
        .orElseGet(
            () -> {
              TimesheetRequest request = new TimesheetRequest();
              request.setPeriodStart(periodStart);
              request.setPeriodEnd(periodEnd);
              return createTimesheet(request);
            });
  }

  // get current week's timesheet
  @Transactional
  public Timesheet getOrCreateCurrentTimesheet() {

    UUID workspaceMemberId = securityUtils.getDefaultWorkspaceMemberId();

    LocalDate today = LocalDate.now();
    LocalDate periodStart = today.with(java.time.DayOfWeek.MONDAY);
    LocalDate periodEnd = today.with(java.time.DayOfWeek.SUNDAY);
    return getOrCreateTimesheet(periodStart, periodEnd);
  }

  @Transactional
  public Timesheet submitTimesheet(UUID timesheetId) {
    UUID currentMemberId = securityUtils.getDefaultWorkspaceMemberId();

    Timesheet timesheet =
        timesheetRepository
            .findById(timesheetId)
            .orElseThrow(() -> new RuntimeException("Timesheet not found"));

    if (!timesheet.getWorkspaceMemberId().equals(currentMemberId)) {
      throw new RuntimeException("You can only submit your own timesheets");
    }

    if (!"DRAFT".equals(timesheet.getStatus())) {
      throw new RuntimeException("Timesheet has already been submitted");
    }

    timesheet.setIsLocked(true);
    timesheet.setLockedAt(LocalDateTime.now());
    timesheet.setStatus("SUBMITTED");
    timesheet.setSubmittedAt(LocalDateTime.now());

    // all the time entries in the timesheet should be locked
    List<TimeEntry> entries = timeEntryRepository.findByTimesheetId(timesheetId);

    for (TimeEntry entry : entries) {
      entry.setIsLocked(true);
    }
    timeEntryRepository.saveAll(entries);

    return timesheetRepository.save(timesheet);
  }

  @Transactional
  public Timesheet approveTimesheet(UUID timesheetId, UUID reviewerId) {

    // the user should be an admin or a manager to approve
    if (!securityUtils.isAdmin() && !securityUtils.isManager()) {
      throw new RuntimeException("Only Admins and Managers can approve timesheets");
    }

    Timesheet timesheet =
        timesheetRepository
            .findById(timesheetId)
            .orElseThrow(() -> new RuntimeException("Timesheet not found"));

    // to prevent self-approval
    if (timesheet.getWorkspaceMemberId().equals(reviewerId)) {
      throw new RuntimeException("You cannot approve your own timesheet");
    }

    if (!"SUBMITTED".equals(timesheet.getStatus())) {
      throw new RuntimeException("Only submitted timesheets can be approved");
    }

    timesheet.setIsLocked(true);
    timesheet.setStatus("APPROVED");
    timesheet.setApprovedAt(LocalDateTime.now());
    timesheet.setApprovedByWorkspaceMemberId(reviewerId);
    timesheet.setLockedAt(LocalDateTime.now());

    lockEntries(timesheetId);

    return timesheetRepository.save(timesheet);
  }

  @Transactional
  public Timesheet rejectTimesheet(UUID timesheetId, UUID reviewerId, String reason) {

    // the user must be admin or manager to reject
    if (!securityUtils.isAdmin() && !securityUtils.isManager()) {
      throw new RuntimeException("Only Admins and Managers can reject timesheets");
    }

    Timesheet timesheet =
        timesheetRepository
            .findById(timesheetId)
            .orElseThrow(() -> new RuntimeException("Timesheet not found"));

    // to prevent self-rejection
    if (timesheet.getWorkspaceMemberId().equals(reviewerId)) {
      throw new RuntimeException("You cannot reject your own timesheet");
    }

    if (!"SUBMITTED".equals(timesheet.getStatus())) {
      throw new RuntimeException("Only submitted timesheets can be rejected");
    }

    timesheet.setIsLocked(false);
    timesheet.setLockedAt(null);
    timesheet.setStatus("REJECTED");
    timesheet.setRejectedAt(LocalDateTime.now());
    timesheet.setApprovedByWorkspaceMemberId(reviewerId);
    timesheet.setRejectionReason(reason);

    // since it is rejected all time entries should be unlocked
    unlockEntries(timesheetId);

    return timesheetRepository.save(timesheet);
  }

  /*
   - managers and admins can see all timesheets that are approved, rejected and submitted
   - gets all the timesheets in a workspace
  */
  @Transactional
  public List<Timesheet> getWorkspaceTimesheets() {
    if (!securityUtils.isAdmin() && !securityUtils.isManager()) {
      throw new RuntimeException("Only Admins and Managers can view other peoples timesheets");
    }

    UUID workspaceId = securityUtils.getCurrentWorkspaceId();
    return timesheetRepository.findByWorkspaceIdExcludingDraft(workspaceId);
  }

  public List<Timesheet> getTimesheetsByMember(UUID workspaceMemberId) {
    return timesheetRepository.findByWorkspaceMemberId(workspaceMemberId);
  }

  public List<Timesheet> getTimesheetsByMemberAndStatus(UUID workspaceMemberId, String status) {
    return timesheetRepository.findByWorkspaceMemberIdAndStatus(workspaceMemberId, status);
  }

  public Timesheet getTimesheetById(UUID id) {
    return timesheetRepository
        .findById(id)
        .orElseThrow(() -> new RuntimeException("Timesheet not found"));
  }

  // !helper functions
  private void lockEntries(UUID timesheetId) {
    List<TimeEntry> entries = timeEntryRepository.findByTimesheetId(timesheetId);
    for (TimeEntry entry : entries) {
      entry.setIsLocked(true);
    }
    timeEntryRepository.saveAll(entries);
  }

  private void unlockEntries(UUID timesheetId) {
    List<TimeEntry> entries = timeEntryRepository.findByTimesheetId(timesheetId);
    for (TimeEntry entry : entries) {
      entry.setIsLocked(false);
    }
    timeEntryRepository.saveAll(entries);
  }
}
