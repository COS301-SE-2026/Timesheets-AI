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
import timesheets.domain.WorkspaceMember;
import timesheets.dto.request.TimesheetRequest;
import timesheets.repository.TimeEntryRepository;
import timesheets.repository.TimesheetRepository;
import timesheets.repository.WorkspaceMemberRepository;
import timesheets.security.SecurityUtils;

@Service
@RequiredArgsConstructor
public class TimesheetService {

  private final TimesheetRepository timesheetRepository;
  private final TimeEntryRepository timeEntryRepository;
  private final SecurityUtils securityUtils;
  private final WorkspaceMemberRepository workspaceMemberRepository;

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
   - admins can see all timesheets that are approved, rejected and submitted
   - managers only see other peoples timesheets that are approved, rejected and submitted
   - gets all the timesheets in a workspace
  */
  @Transactional
  public List<Timesheet> getWorkspaceTimesheets() {

    if (!securityUtils.isAdmin() && !securityUtils.isManager()) {
      throw new RuntimeException("Only Admins and Managers can view other peoples timesheets");
    }

    UUID workspaceId = securityUtils.getCurrentWorkspaceId();
    UUID currentMemberId = securityUtils.getDefaultWorkspaceMemberId();

    // we want admins to be able to view all the timesheets, including their own
    // but I also have protections in place to prevent self-approval and rejection
    if (securityUtils.isAdmin()) {
      return timesheetRepository.findByWorkspaceIdExcludingDraft(workspaceId);
    }

    // managers will see other peoples timesheets but not their own
    return timesheetRepository.findByWorkspaceIdExcludingDraftAndMember(
        workspaceId, currentMemberId);
  }

  // this will get the users timesheets by the status
  @Transactional
  public List<Timesheet> getWorkspaceTimesheetsByStatus(String status) {

    if (!securityUtils.isAdmin() && !securityUtils.isManager()) {
      throw new RuntimeException("Only managers and admins can view workspace timesheets");
    }

    UUID workspaceId = securityUtils.getCurrentWorkspaceId();
    UUID currentMemberId = securityUtils.getDefaultWorkspaceMemberId();

    // admins will see all timesheets for all users in that workspace
    if (securityUtils.isAdmin()) {
      return timesheetRepository.findByWorkspaceIdAndStatus(workspaceId, status);
    }

    // managers will see only other peoples timesheets but they will not see their own
    return timesheetRepository.findByWorkspaceIdAndStatusExcludingMember(
        workspaceId, status, currentMemberId);
  }

  // will get all the timesheets that have been submitted and waiting approval
  @Transactional
  public List<Timesheet> getPendingWorkspaceTimesheets() {

    if (!securityUtils.isAdmin() && !securityUtils.isManager()) {
      throw new RuntimeException("Only Admins and Managers can view pending timesheets");
    }

    UUID workspaceId = securityUtils.getCurrentWorkspaceId();
    UUID currentMemberId = securityUtils.getDefaultWorkspaceMemberId();

    // admins will see all that have been submitted
    if (securityUtils.isAdmin()) {
      return timesheetRepository.findPendingByWorkspaceId(workspaceId);
    }

    // managers will see all that the other submitted, excluding theirs
    return timesheetRepository.findPendingByWorkspaceIdExcludingMember(
        workspaceId, currentMemberId);
  }

  public List<Timesheet> getTimesheetsByMember(UUID workspaceMemberId) {
    return timesheetRepository.findByWorkspaceMemberId(workspaceMemberId);
  }

  public List<Timesheet> getTimesheetsByMemberAndStatus(UUID workspaceMemberId, String status) {
    return timesheetRepository.findByWorkspaceMemberIdAndStatus(workspaceMemberId, status);
  }

  @Transactional(readOnly = true)
  public Timesheet getTimesheetById(UUID id) {

    Timesheet timesheet =
        timesheetRepository
            .findById(id)
            .orElseThrow(() -> new RuntimeException("Timesheet not found"));

    UUID currentMemberId = securityUtils.getDefaultWorkspaceMemberId();
    UUID workspaceId = securityUtils.getCurrentWorkspaceId();
    UUID timesheetOwnerId = timesheet.getWorkspaceMemberId();

    // since our admins can see any timesheet
    if (securityUtils.isAdmin()) {
      return timesheet;
    }

    // a regular user should only be able to see their own timesheets
    if (!securityUtils.isManager()) {
      if (!timesheetOwnerId.equals(currentMemberId)) {
        throw new RuntimeException("You can only view your own timesheets");
      }
      return timesheet;
    }

    // managers can see everyone elses timesheets besides their own
    if (timesheetOwnerId.equals(currentMemberId)) {
      throw new RuntimeException("Managers cannot view their own timesheets in workspace view");
    }

    // to check that the timesheet is in the managers workspace
    WorkspaceMember owner =
        workspaceMemberRepository
            .findById(timesheetOwnerId)
            .orElseThrow(() -> new RuntimeException("Workspace member not found"));

    if (!owner.getWorkspaceId().equals(workspaceId)) {
      throw new RuntimeException("Timesheet not found in your workspace");
    }

    return timesheet;
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
