package timesheets.service;

import exception.ConflictException;
import exception.ResourceNotFoundException;
import exception.UnauthorizedException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import timesheets.domain.Project;
import timesheets.domain.Task;
import timesheets.domain.TimeEntry;
import timesheets.domain.TimerSession;
import timesheets.domain.Timesheet;
import timesheets.domain.WorkspaceMember;
import timesheets.dto.request.StartTimerRequest;
import timesheets.repository.ProjectMemberRepository;
import timesheets.repository.ProjectRepository;
import timesheets.repository.TaskRepository;
import timesheets.repository.TimeEntryRepository;
import timesheets.repository.TimerSessionRepository;
import timesheets.repository.WorkspaceMemberRepository;
import timesheets.security.SecurityUtils;

// this is the file that has all my timer business logic
// the controller will call the service and the service will call the repositories

@Service
@RequiredArgsConstructor
public class TimerService {

  private final TimerSessionRepository timerSessionRepository;
  private final TimeEntryRepository timeEntryRepository;
  private final WorkspaceMemberRepository workspaceMemberRepository;
  private final ProjectRepository projectRepository;
  private final TaskRepository taskRepository;
  private final ProjectMemberRepository projectMemberRepository;
  private final SecurityUtils securityUtils;
  private final TimesheetService timesheetService;

  // this will start a new timer, and in our system only one timer is allowed across the entire
  // workspace
  @Transactional
  public TimerSession startTimer(StartTimerRequest request) {

    // I want to check if a member exists
    UUID workspaceMemberId = securityUtils.getDefaultWorkspaceMemberId();

    UUID userId =
        workspaceMemberRepository
            .findById(workspaceMemberId)
            .orElseThrow(() -> new ResourceNotFoundException("Workspace member not found"))
            .getUserId(); // gets the ID of the user

    List<WorkspaceMember> userMemberships =
        workspaceMemberRepository.findByUserId(
            userId); // to find all the workspace memberships for this user

    // finds all the workspace member IDs
    List<UUID> workspaceMemberIds =
        userMemberships.stream().map(WorkspaceMember::getId).collect(Collectors.toList());

    // should see if they have an timers in any workspace
    Optional<TimerSession> existingActiveTimer =
        timerSessionRepository.findFirstByWorkspaceMemberIdInAndIsRunningTrue(workspaceMemberIds);

    if (existingActiveTimer.isPresent()) {
      TimerSession activeTimer = existingActiveTimer.get();

      // checks if the existing timer is paused
      if (Boolean.TRUE.equals(activeTimer.getIsPaused())) {
        throw new ConflictException(
            "Timer is paused",
            "You have a paused timer. Resume it or discard it before starting a new one.",
            activeTimer.getId());
      }

      throw new ConflictException(
          "Timer already active in another workspace",
          "You already have a running timer. Stop it before starting a new one.",
          activeTimer.getId());
    }

    // does the member have access??
    Project project =
        projectRepository
            .findById(request.getProjectId())
            .orElseThrow(() -> new ResourceNotFoundException("Project not found"));

    boolean isMember =
        projectMemberRepository.existsByProjectIdAndWorkspaceMemberId(
            project.getId(), workspaceMemberId);
    if (!isMember) {
      throw new UnauthorizedException(
          "You are not assigned to this project"); // cause I want to see if they actually have
      // access
    }

    Task task = null;

    if (request.getTaskId() != null) {
      task = taskRepository.findById(request.getTaskId()).orElse(null);

      if (task != null && !task.getProjectId().equals(project.getId())) {
        throw new IllegalArgumentException("Task does not belong to the specified project");
      }
    }
    // above I am getting the task and seeing if it actually belongs o the project

    TimerSession timerSession = new TimerSession();
    timerSession.setWorkspaceMemberId(workspaceMemberId);
    timerSession.setProjectId(project.getId());
    timerSession.setTaskId(task != null ? task.getId() : null);
    timerSession.setStartedAt(LocalDateTime.now());
    timerSession.setIsRunning(true);
    timerSession.setIsPaused(false);
    timerSession.setPausedDurationSeconds(0L);
    timerSession.setPausedAt(null);

    return timerSessionRepository.save(
        timerSession); // so I am creating and getting a timer session
  }

  /*
  -- this should pause the current running timer
  - I added the timer pause and stuff such that the timer can be resumed later
   */
  @Transactional
  public TimerSession pauseTimer() {
    UUID workspaceMemberId = securityUtils.getDefaultWorkspaceMemberId();
    return pauseTimerInternal(workspaceMemberId);
  }

  /*
  - this pauses a timer during logout, and should not require authentication
  - auth service should call this one
  - it silently fails when there is no active timer found since nothing should happen with that
   */
  @Transactional
  public boolean pauseTimerForLogout(UUID workspaceMemberId) {
    try {
      pauseTimerInternal(workspaceMemberId);
      return true;
    } catch (IllegalStateException e) {
      // when no active timer is founf so that is fine, there is nothing to pause
      return false;
    }
  }

  // helper function to help with pausing a timer
  private TimerSession pauseTimerInternal(UUID workspaceMemberId) {
    TimerSession activeTimer =
        timerSessionRepository
            .findByWorkspaceMemberIdAndIsRunningTrue(workspaceMemberId)
            .orElseThrow(() -> new IllegalStateException("No active timer found"));

    // check if the timer is already paused
    if (Boolean.TRUE.equals(activeTimer.getIsPaused())) {
      throw new IllegalStateException("Timer is already paused");
    }

    activeTimer.setIsPaused(true);
    activeTimer.setPausedAt(LocalDateTime.now());

    return timerSessionRepository.save(activeTimer);
  }

  // resumes a timer
  @Transactional
  public TimerSession resumeTimer() {
    UUID workspaceMemberId = securityUtils.getDefaultWorkspaceMemberId();

    TimerSession activeTimer =
        timerSessionRepository
            .findByWorkspaceMemberIdAndIsRunningTrue(workspaceMemberId)
            .orElseThrow(() -> new IllegalStateException("No active timer found"));

    // should see if there is an active timer for the user
    if (!Boolean.TRUE.equals(activeTimer.getIsPaused())) {
      throw new IllegalStateException("Timer is not paused");
    }

    // calculates how long it is paused
    if (activeTimer.getPausedAt() != null) {
      long pausedDurationSeconds =
          ChronoUnit.SECONDS.between(activeTimer.getPausedAt(), LocalDateTime.now());

      // add the total to paused duration
      Long currentPausedDuration = activeTimer.getPausedDurationSeconds();

      if (currentPausedDuration == null) {
        currentPausedDuration = 0L;
      }
      activeTimer.setPausedDurationSeconds(currentPausedDuration + pausedDurationSeconds);
    }

    // update timer states
    activeTimer.setIsPaused(false);
    activeTimer.setPausedAt(null);

    return timerSessionRepository.save(activeTimer);
  }

  // this should be if a timer is stopped and a draft timer entry is created
  @Transactional
  public TimeEntry stopTimer() {

    UUID workspaceMemberId = securityUtils.getDefaultWorkspaceMemberId();

    // to find an active timer
    TimerSession activeTimer =
        timerSessionRepository
            .findByWorkspaceMemberIdAndIsRunningTrue(workspaceMemberId)
            .orElseThrow(() -> new IllegalStateException("No active timer found"));

    LocalDateTime now = LocalDateTime.now();
    LocalDateTime startedAt = activeTimer.getStartedAt();
    long durationSeconds = ChronoUnit.SECONDS.between(startedAt, now); // I am calculating how long

    // sibtracts the total paused duration
    if (activeTimer.getPausedDurationSeconds() != null) {
      durationSeconds -= (activeTimer.getPausedDurationSeconds());
    }

    // if the timer is paused, only time that counts is when a timer is paused
    if (Boolean.TRUE.equals(activeTimer.getIsPaused()) && activeTimer.getPausedAt() != null) {
      long durationUntilPause = ChronoUnit.SECONDS.between(startedAt, activeTimer.getPausedAt());

      durationSeconds = durationUntilPause;

      if (activeTimer.getPausedDurationSeconds() != null) {
        durationSeconds -= activeTimer.getPausedDurationSeconds();
      }
    }

    // to make sure that duration is not negative
    durationSeconds = Math.max(0, durationSeconds);

    activeTimer.setEndedAt(now);
    activeTimer.setIsRunning(false); // stopping the timer

    timerSessionRepository.save(activeTimer);

    // creates or gets timesheet ofr the week
    LocalDate entryDate = now.toLocalDate();
    LocalDate weekStart = entryDate.with(java.time.DayOfWeek.MONDAY);
    LocalDate weekEnd = entryDate.with(java.time.DayOfWeek.SUNDAY);
    Timesheet timesheet = timesheetService.getOrCreateTimesheet(weekStart, weekEnd);

    // draft timer created, and it links to a timesheet
    TimeEntry timeEntry = new TimeEntry();
    timeEntry.setWorkspaceMemberId(workspaceMemberId);
    timeEntry.setTimesheetId(timesheet.getId());
    timeEntry.setProjectId(activeTimer.getProjectId());
    timeEntry.setTaskId(activeTimer.getTaskId());
    timeEntry.setStartTime(startedAt);
    timeEntry.setEndTime(now);
    timeEntry.setDurationSeconds((int) durationSeconds);
    timeEntry.setEntryType("TIMER");
    timeEntry.setIsLocked(false);

    return timeEntryRepository.save(timeEntry);
  }

  // ! helper function
  public TimerSession getActiveTimer() {

    UUID workspaceMemberId = securityUtils.getDefaultWorkspaceMemberId();

    return timerSessionRepository
        .findByWorkspaceMemberIdAndIsRunningTrue(workspaceMemberId)
        .orElse(null);
  }

  // okay so when the page is refreshed it should still have their timer running, so that is how I
  // am doing it

  // ! we want our users to be able to discard a timer without without it creating a time entry
  @Transactional
  public void discardTimer() {

    UUID workspaceMemberId = securityUtils.getDefaultWorkspaceMemberId();

    // should find an active timer
    TimerSession activeTimer =
        timerSessionRepository
            .findByWorkspaceMemberIdAndIsRunningTrue(workspaceMemberId)
            .orElseThrow(() -> new IllegalStateException("No active timer found to discard"));

    timerSessionRepository.delete(
        activeTimer); // will just delete the timer entry without creating a new one
  }
}
