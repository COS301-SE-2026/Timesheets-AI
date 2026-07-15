package timesheets.controller;

import exception.ConflictException;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import timesheets.domain.Project;
import timesheets.domain.Task;
import timesheets.domain.TimeEntry;
import timesheets.domain.TimerSession;
import timesheets.domain.User;
import timesheets.domain.WorkspaceMember;
import timesheets.dto.request.StartTimerRequest;
import timesheets.dto.response.ActiveTimerResponse;
import timesheets.dto.response.CreatedTimeEntryResponse;
import timesheets.dto.response.ErrorResponse;
import timesheets.dto.response.StopTimerResponse;
import timesheets.repository.ProjectRepository;
import timesheets.repository.TaskRepository;
import timesheets.repository.UserRepository;
import timesheets.repository.WorkspaceMemberRepository;
import timesheets.service.TimerService;

// the rest controller should handle the HTTP requests
@RestController
@RequestMapping("/api/timers") // my base URL it should be /api/timers
@RequiredArgsConstructor
public class TimerController {

  private final TimerService timerService;
  private final ProjectRepository projectRepository;
  private final TaskRepository taskRepository;
  private final WorkspaceMemberRepository workspaceMemberRepository;
  private final UserRepository userRepository;

  private UUID getCurrentWorkspaceMemberId() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

    // gets the Spring Security User
    org.springframework.security.core.userdetails.User springUser =
        (org.springframework.security.core.userdetails.User) authentication.getPrincipal();

    // gets email from Spring Security User
    String email = springUser.getUsername();

    // finds your custom user from database
    User user =
        userRepository.findByEmail(email).orElseThrow(() -> new RuntimeException("User not found"));

    // gets workspace member
    return workspaceMemberRepository.findByUserId(user.getId()).stream()
        .findFirst()
        .map(WorkspaceMember::getId)
        .orElseThrow(() -> new RuntimeException("User is not a member of any workspace"));
  }

  @PostMapping("/start") // the endpoint will look like POST /api/timers/start
  public ResponseEntity<?> startTimer(@Valid @RequestBody StartTimerRequest request) {
    try {
      UUID workspaceMemberId = getCurrentWorkspaceMemberId();
      TimerSession timer = timerService.startTimer(workspaceMemberId, request);

      ActiveTimerResponse response =
          convertToResponse(
              timer); // I am converting the entry to DTO for frontend, going to create a helper
      return ResponseEntity.ok(response);

    } catch (ConflictException e) {
      ErrorResponse errorResponse =
          ErrorResponse.conflict(e.getUserMessage(), e.getActiveTimerId());
      return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
      // if a user tries to start another time while one exists already
    }
  }

  @PostMapping("/stop") // the endpoint will look like POST /api/timers/stop
  public ResponseEntity<StopTimerResponse> stopTimer() {

    UUID workspaceMemberId = getCurrentWorkspaceMemberId();

    TimerSession activeTimer = timerService.getActiveTimer(workspaceMemberId);

    TimeEntry timeEntry = timerService.stopTimer(workspaceMemberId); // draft entry will be created

    StopTimerResponse response = convertToStopResponse(timeEntry, activeTimer.getId());

    return ResponseEntity.ok(response); // this should sent a 200 response
  }

  // should get the currently running timer
  @GetMapping("/active") // endpoint will look like GET /api/timers/active
  public ResponseEntity<ActiveTimerResponse> getActiveTimer() {
    UUID workspaceMemberId = getCurrentWorkspaceMemberId();

    TimerSession timer = timerService.getActiveTimer(workspaceMemberId);

    if (timer != null) {
      return ResponseEntity.ok(convertToResponse(timer)); // the timer is running
    }
    // No active timer - return 204 No Content (frontend knows no timer running)
    return ResponseEntity.noContent().build();
  }

  @DeleteMapping("/discard") // the endpoint will look like DELETE /api/timers/discard
  public ResponseEntity<Void> discardTimer() {

    UUID workspaceMemberId = getCurrentWorkspaceMemberId();

    TimerSession activeTimer =
        timerService.getActiveTimer(workspaceMemberId); // should see if there is an active timer

    if (activeTimer == null) {
      return ResponseEntity.notFound().build(); // meaning no timer found
    }

    timerService.discardTimer(workspaceMemberId); // should just discard the timer

    return ResponseEntity.noContent().build(); // no success cause nothing created
  }

  // ! so the database has more fields than we want the frontend to know about, so we are not going
  // to send all those to frontend, so we need to only send what frontend needs
  // typically called private conversion methods

  private ActiveTimerResponse convertToResponse(TimerSession timer) {

    ActiveTimerResponse response = new ActiveTimerResponse();

    response.setId(timer.getId());
    response.setStartedAt(timer.getStartedAt());
    response.setActive(timer.getIsRunning());

    // this will find the time that passed since the timer started
    if (timer.getIsRunning() && timer.getStartedAt() != null) {

      long minutes =
          java.time.Duration.between(timer.getStartedAt(), java.time.LocalDateTime.now())
              .toMinutes();
      response.setElapsedMinutes((int) minutes);

    } else {
      response.setElapsedMinutes(0);
    }

    String projectName = "Unknown Project";

    if (timer.getProjectId() != null) {
      Project project =
          projectRepository
              .findById(timer.getProjectId())
              .orElse(null); // I can find the actual prject name!

      if (project != null) {
        projectName = project.getName();
      }
    }
    // previously I wanted to put something loading, but since I can have a stub, and because of JPA
    // I have the basic functions now I can do this yayyy!!

    ActiveTimerResponse.SimpleProject simpleProject = new ActiveTimerResponse.SimpleProject();

    simpleProject.setId(timer.getProjectId());
    simpleProject.setName(projectName);
    response.setProject(simpleProject);

    if (timer.getTaskId() != null) {
      String taskTitle = "Unknown Task";

      Task task = taskRepository.findById(timer.getTaskId()).orElse(null);
      if (task != null) {
        taskTitle = task.getTitle();
      }

      ActiveTimerResponse.SimpleTask simpleTask = new ActiveTimerResponse.SimpleTask();

      simpleTask.setId(timer.getTaskId());
      simpleTask.setTitle(taskTitle); // same thing where I can get the actual task name
      response.setTask(simpleTask);
    }

    return response;
  }

  // this will be what the shows when a timer is stopped
  private StopTimerResponse convertToStopResponse(TimeEntry timeEntry, UUID timerId) {

    StopTimerResponse response = new StopTimerResponse();

    response.setTimerId(timerId);
    response.setStoppedAt(timeEntry.getEndTime());
    response.setDurationMinutes(timeEntry.getDurationMinutes());

    // this creates the nested time entry
    CreatedTimeEntryResponse createdEntry = new CreatedTimeEntryResponse();

    createdEntry.setId(timeEntry.getId());
    createdEntry.setDate(
        timeEntry.getStartTime() != null
            ? timeEntry.getStartTime().toLocalDate()
            : LocalDate.now());

    createdEntry.setStartTime(
        timeEntry.getStartTime() != null
            ? timeEntry.getStartTime().toLocalTime()
            : LocalTime.now());
    createdEntry.setEndTime(
        timeEntry.getEndTime() != null ? timeEntry.getEndTime().toLocalTime() : LocalTime.now());

    createdEntry.setDurationMinutes(timeEntry.getDurationMinutes());
    createdEntry.setStatus("DRAFT");

    String projectName = "Unknown Project";

    if (timeEntry.getProjectId() != null) {
      Project project = projectRepository.findById(timeEntry.getProjectId()).orElse(null);

      if (project != null) {
        projectName = project.getName();
      }
    }

    CreatedTimeEntryResponse.SimpleProject simpleProject =
        new CreatedTimeEntryResponse.SimpleProject();

    simpleProject.setId(timeEntry.getProjectId());
    simpleProject.setName(projectName);
    createdEntry.setProject(simpleProject);

    if (timeEntry.getTaskId() != null) {
      String taskTitle = "Unknown Task";
      Task task = taskRepository.findById(timeEntry.getTaskId()).orElse(null);

      if (task != null) {
        taskTitle = task.getTitle();
      }

      CreatedTimeEntryResponse.SimpleTask simpleTask = new CreatedTimeEntryResponse.SimpleTask();

      simpleTask.setId(timeEntry.getTaskId());
      simpleTask.setTitle(taskTitle);
      createdEntry.setTask(simpleTask);
    }

    response.setCreatedTimeEntry(createdEntry);
    return response;
  }
}
