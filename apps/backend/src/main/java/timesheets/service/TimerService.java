package timesheets.service;

import exception.ConflictException;
import exception.ResourceNotFoundException;
import exception.UnauthorizedException;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import timesheets.domain.*;
import timesheets.dto.request.StartTimerRequest;
import timesheets.enums.TimeEntryStatus;
import timesheets.repository.*;

// this is the file that has all my timer business logic
// the controller will call the service and the service will call the repositories

@Service
public class TimerService {

  private final TimerSessionRepository timerSessionRepository;
  private final TimeEntryRepository timeEntryRepository;
  private final WorkspaceMemberRepository workspaceMemberRepository;
  private final ProjectRepository projectRepository;
  private final TaskRepository taskRepository;
  private final ProjectMemberRepository projectMemberRepository;

  public TimerService(
      TimerSessionRepository timerSessionRepository,
      TimeEntryRepository timeEntryRepository,
      WorkspaceMemberRepository workspaceMemberRepository,
      ProjectRepository projectRepository,
      TaskRepository taskRepository,
      ProjectMemberRepository projectMemberRepository) {
    this.timerSessionRepository = timerSessionRepository;
    this.timeEntryRepository = timeEntryRepository;
    this.workspaceMemberRepository = workspaceMemberRepository;
    this.projectRepository = projectRepository;
    this.taskRepository = taskRepository;
    this.projectMemberRepository = projectMemberRepository;
  }

  // this will start a new timer, and in our system only one timer is allowed across the entire
  // workspace
  @Transactional
  public TimerSession startTimer(UUID workspaceMemberId, StartTimerRequest request) {

    // I want to check if a memeber exists
    WorkspaceMember member =
        workspaceMemberRepository
            .findById(workspaceMemberId)
            .orElseThrow(() -> new ResourceNotFoundException("Workspace member not found"));

    UUID userId = member.getUserId(); // gets the ID of the user

    List<WorkspaceMember> userMemberships =
        workspaceMemberRepository.findAllByUserId(
            userId); // to find all the workspace memberships for this user

    // finds all the workspace member IDs
    List<UUID> workspaceMemberIds =
        userMemberships.stream().map(WorkspaceMember::getId).collect(Collectors.toList());

    // should see if they have an timers in any workspace
    Optional<TimerSession> existingActiveTimer =
        timerSessionRepository.findFirstByWorkspaceMemberIdInAndIsRunningTrue(workspaceMemberIds);

    if (existingActiveTimer.isPresent()) {
      TimerSession activeTimer = existingActiveTimer.get();
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

    timerSession.setPausedDurationSeconds(0L);
    timerSession.setSource("timer");
    timerSession.setNotes(request.getNotes());

    return timerSessionRepository.save(
        timerSession); // so I am creating and getting a timer session
  }

  // this should be if a timer is stopped and a draft timer entry is created
  @Transactional
  public TimeEntry stopTimer(UUID workspaceMemberId) {

    WorkspaceMember member =
        workspaceMemberRepository
            .findById(workspaceMemberId)
            .orElseThrow(() -> new ResourceNotFoundException("Workspace member not found"));

    // to find an active timer
    TimerSession activeTimer =
        timerSessionRepository
            .findByWorkspaceMemberIdAndIsRunningTrue(workspaceMemberId)
            .orElseThrow(() -> new IllegalStateException("No active timer found"));

    LocalDateTime now = LocalDateTime.now();
    LocalDateTime startedAt = activeTimer.getStartedAt();

    long durationMinutes = ChronoUnit.MINUTES.between(startedAt, now); // I am calculating how long

    if (activeTimer.getPausedDurationSeconds() != null) {
      durationMinutes -= (activeTimer.getPausedDurationSeconds() / 60);
    } // so this should subtract the paused duration to see the actual time- I made the mistake of
    // not cosidering this properlly

    activeTimer.setEndedAt(now);
    activeTimer.setIsRunning(false); // stopping the timer

    timerSessionRepository.save(activeTimer);

    // draft timer created
    TimeEntry timeEntry = new TimeEntry();
    timeEntry.setWorkspaceMemberId(workspaceMemberId);
    timeEntry.setProjectId(activeTimer.getProjectId());
    timeEntry.setTaskId(activeTimer.getTaskId());

    timeEntry.setStartTime(startedAt);
    timeEntry.setEndTime(now);
    timeEntry.setDurationMinutes((int) durationMinutes);

    timeEntry.setEntryType("timer");
    timeEntry.setDescription(activeTimer.getNotes());
    timeEntry.setStatus(TimeEntryStatus.DRAFT);

    timeEntry.setIsLocked(false);

    return timeEntryRepository.save(timeEntry);
  }

  public TimerSession getActiveTimer(UUID workspaceMemberId) {
    return timerSessionRepository
        .findByWorkspaceMemberIdAndIsRunningTrue(workspaceMemberId)
        .orElse(null);
  }

  // okay so when the page is refreshed it should still have their timer running, so that is how I
  // am doing it

  // ! we want our users to be able to discard a timer without without it creating a time entry
  @Transactional
  public void discardTimer(UUID workspaceMemberId) {

    // should find an active timer
    TimerSession activeTimer =
        timerSessionRepository
            .findByWorkspaceMemberIdAndIsRunningTrue(workspaceMemberId)
            .orElseThrow(() -> new IllegalStateException("No active timer found to discard"));

    timerSessionRepository.delete(
        activeTimer); // will just delete the timer entry without creating a new one
  }
}
