package timesheets.service;

import exception.AccessDeniedException;
import exception.BadRequestException;
import exception.ResourceNotFoundException;
import exception.StateConflictException;
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

  // creates a new draft timesheet for the current user
  @Transactional
  public Timesheet createTimesheet(TimesheetRequest request) {

    UUID workspaceMemberId = securityUtils.getDefaultWorkspaceMemberId();

    if (request.getPeriodStart() == null || request.getPeriodEnd() == null) {
      throw new BadRequestException("Period start and end dates are required");
    }

    if (request.getPeriodStart().isAfter(request.getPeriodEnd())) {
      throw new BadRequestException("Period start must be before period end");
    }

    Timesheet timesheet = new Timesheet();
    timesheet.setWorkspaceMemberId(workspaceMemberId);
    timesheet.setPeriodStart(request.getPeriodStart());
    timesheet.setPeriodEnd(request.getPeriodEnd());
    timesheet.setStatus("DRAFT");
    timesheet.setIsLocked(false);

    return timesheetRepository.save(timesheet);
  }

  // get existing timesheet or create a new draft one
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

  /*
  - gets a single timesheet by it's id
  - this can be used when a dev, manager or admin wants to view their timesheet details
  - when an admin or manager want to view the details of someone elses timesheet in their workspace
  */
  @Transactional(readOnly = true)
  public Timesheet getTimesheetById(UUID id) {

    Timesheet timesheet =
        timesheetRepository
            .findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Timesheet not found with id: " + id));

    UUID currentMemberId = securityUtils.getDefaultWorkspaceMemberId();
    UUID workspaceId = securityUtils.getCurrentWorkspaceId();
    UUID timesheetOwnerId = timesheet.getWorkspaceMemberId();

    // a regular user should only be able to see their own timesheets
    if (!securityUtils.isManager() && !securityUtils.isAdmin()) {
      if (!timesheetOwnerId.equals(currentMemberId)) {
        throw new AccessDeniedException("You can only view your own timesheets");
      }
      return timesheet;
    }

    // to check that the timesheet is in the managers or admins workspace
    WorkspaceMember owner =
        workspaceMemberRepository
            .findById(timesheetOwnerId)
            .orElseThrow(() -> new ResourceNotFoundException("Workspace member not found"));

    if (!owner.getWorkspaceId().equals(workspaceId)) {
      throw new AccessDeniedException("Timesheet not found in your workspace");
    }

    // admins and managers can view any timesheet in their workspace
    return timesheet;
  }

  /*
  - this will get all the timesheets for a specific member
  - used for dev, manager or admin to view their own timesheets
  - OR also, manager and admin can see timesheets of any member in workspace
  */
  @Transactional
  public List<Timesheet> getTimesheetsByMember(UUID workspaceMemberId) {

    UUID currentMemberId = securityUtils.getDefaultWorkspaceMemberId();
    UUID workspaceId = securityUtils.getCurrentWorkspaceId();

    // devs should only be able to see their own timesheets
    if (!securityUtils.isAdmin() && !securityUtils.isManager()) {
      if (!workspaceMemberId.equals(currentMemberId)) {
        throw new AccessDeniedException("You can only view your own timesheets");
      }
      return timesheetRepository.findByWorkspaceMemberId(workspaceMemberId);
    }

    // checking that the requested member exists in the workspace
    WorkspaceMember member =
        workspaceMemberRepository
            .findById(workspaceMemberId)
            .orElseThrow(() -> new ResourceNotFoundException("Workspace member not found"));

    if (!member.getWorkspaceId().equals(workspaceId)) {
      throw new AccessDeniedException("Cannot view timesheets from another workspace");
    }

    // managers are allowed to view any members timesheets from the workspace
    return timesheetRepository.findByWorkspaceMemberId(workspaceMemberId);
  }

  /*
  - gets all the timesheets for a specific member, filtered by the status
  - used for dev, manager and admin to view their own timesheets
  - OR also, manager and admin can see the timesheets of any member in their workspace
  */
  @Transactional
  public List<Timesheet> getTimesheetsByMemberAndStatus(UUID workspaceMemberId, String status) {

    UUID workspaceId = securityUtils.getCurrentWorkspaceId();
    UUID currentMemberId = securityUtils.getDefaultWorkspaceMemberId();

    // a regular user should only be able to see their own timesheets
    if (!securityUtils.isManager() && !securityUtils.isAdmin()) {
      if (!workspaceMemberId.equals(currentMemberId)) {
        throw new AccessDeniedException("You can only view your own timesheets");
      }
      return timesheetRepository.findByWorkspaceMemberIdAndStatus(workspaceMemberId, status);
    }

    // to check that the timesheet is in the managers or admins workspace
    WorkspaceMember owner =
        workspaceMemberRepository
            .findById(workspaceMemberId)
            .orElseThrow(() -> new ResourceNotFoundException("Workspace member not found"));

    // the timesheet should only exist in the current workspace
    if (!owner.getWorkspaceId().equals(workspaceId)) {
      throw new AccessDeniedException("Timesheet not found in your workspace");
    }

    return timesheetRepository.findByWorkspaceMemberIdAndStatus(workspaceMemberId, status);
  }

  // submits a timesheet so it can be approved
  @Transactional
  public Timesheet submitTimesheet(UUID timesheetId) {
    UUID currentMemberId = securityUtils.getDefaultWorkspaceMemberId();
    UUID workspaceId = securityUtils.getCurrentWorkspaceId();

    Timesheet timesheet =
        timesheetRepository
            .findById(timesheetId)
            .orElseThrow(
                () -> new ResourceNotFoundException("Timesheet not found with id: " + timesheetId));

    // only the owner should be able to submit their timesheet
    if (!timesheet.getWorkspaceMemberId().equals(currentMemberId)) {
      throw new AccessDeniedException("You can only submit your own timesheets");
    }

    // making sure that the timesheet belongs to this current workspace
    // it could be the case where they have the timesheet id and try to access it
    WorkspaceMember owner =
        workspaceMemberRepository
            .findById(currentMemberId)
            .orElseThrow(() -> new ResourceNotFoundException("Workspace member not found"));

    if (!owner.getWorkspaceId().equals(workspaceId)) {
      throw new AccessDeniedException("Timesheet is not in your workspace");
    }

    if (!"DRAFT".equals(timesheet.getStatus()) && !"REJECTED".equals(timesheet.getStatus())) {
      throw new StateConflictException("Timesheet has already been submitted");
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

  // approves a submitted timesheet
  @Transactional
  public Timesheet approveTimesheet(UUID timesheetId, UUID reviewerId) {

    UUID workspaceId = securityUtils.getCurrentWorkspaceId();

    // the user should be an admin or a manager to approve
    if (!securityUtils.isAdmin() && !securityUtils.isManager()) {
      throw new AccessDeniedException("Only Admins and Managers can approve timesheets");
    }

    Timesheet timesheet =
        timesheetRepository
            .findById(timesheetId)
            .orElseThrow(
                () -> new ResourceNotFoundException("Timesheet not found with id: " + timesheetId));

    // making sure that the timesheet belongs to this current workspace
    // it could be the case where they have the timesheet id and try to access it
    WorkspaceMember owner =
        workspaceMemberRepository
            .findById(timesheet.getWorkspaceMemberId())
            .orElseThrow(() -> new ResourceNotFoundException("Workspace member not found"));

    if (!owner.getWorkspaceId().equals(workspaceId)) {
      throw new AccessDeniedException("Timesheet is not in your workspace");
    }

    // to prevent self-approval
    if (timesheet.getWorkspaceMemberId().equals(reviewerId)) {
      throw new StateConflictException("You cannot approve your own timesheet");
    }

    if (!"SUBMITTED".equals(timesheet.getStatus())) {
      throw new StateConflictException("Only submitted timesheets can be approved");
    }

    timesheet.setIsLocked(true);
    timesheet.setStatus("APPROVED");
    timesheet.setApprovedAt(LocalDateTime.now());
    timesheet.setApprovedByWorkspaceMemberId(reviewerId);
    timesheet.setLockedAt(LocalDateTime.now());

    lockEntries(timesheetId);

    return timesheetRepository.save(timesheet);
  }

  // rejects a submitted timesheet
  @Transactional
  public Timesheet rejectTimesheet(UUID timesheetId, UUID reviewerId, String reason) {

    UUID workspaceId = securityUtils.getCurrentWorkspaceId();

    // the user must be admin or manager to reject
    if (!securityUtils.isAdmin() && !securityUtils.isManager()) {
      throw new AccessDeniedException("Only Admins and Managers can reject timesheets");
    }

    Timesheet timesheet =
        timesheetRepository
            .findById(timesheetId)
            .orElseThrow(
                () -> new ResourceNotFoundException("Timesheet not found with id: " + timesheetId));

    // making sure that the timesheet belongs to this current workspace
    // it could be the case where they have the timesheet id and try to access it
    WorkspaceMember owner =
        workspaceMemberRepository
            .findById(timesheet.getWorkspaceMemberId())
            .orElseThrow(() -> new ResourceNotFoundException("Workspace member not found"));

    if (!owner.getWorkspaceId().equals(workspaceId)) {
      throw new AccessDeniedException("Timesheet is not in your workspace");
    }

    // to prevent self-rejection
    if (timesheet.getWorkspaceMemberId().equals(reviewerId)) {
      throw new StateConflictException("You cannot reject your own timesheet");
    }

    if (!"SUBMITTED".equals(timesheet.getStatus())) {
      throw new StateConflictException("Only submitted timesheets can be rejected");
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
  @Transactional(readOnly = true)
  public List<Timesheet> getWorkspaceTimesheets() {

    if (!securityUtils.isAdmin() && !securityUtils.isManager()) {
      throw new AccessDeniedException("Only Admins and Managers can view other peoples timesheets");
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
  // admins see all, managers see all besides their own
  @Transactional(readOnly = true)
  public List<Timesheet> getWorkspaceTimesheetsByStatus(String status) {

    if (!securityUtils.isAdmin() && !securityUtils.isManager()) {
      throw new AccessDeniedException("Only managers and admins can view workspace timesheets");
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
  @Transactional(readOnly = true)
  public List<Timesheet> getPendingWorkspaceTimesheets() {

    UUID workspaceId = securityUtils.getCurrentWorkspaceId();
    UUID currentMemberId = securityUtils.getDefaultWorkspaceMemberId();

    if (!securityUtils.isAdmin() && !securityUtils.isManager()) {
      throw new AccessDeniedException("Only Admins and Managers can view pending timesheets");
    }

    // admins will see all that have been submitted
    if (securityUtils.isAdmin()) {
      return timesheetRepository.findPendingByWorkspaceId(workspaceId);
    }

    // managers will see all that the other submitted, excluding theirs
    return timesheetRepository.findPendingByWorkspaceIdExcludingMember(
        workspaceId, currentMemberId);
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
