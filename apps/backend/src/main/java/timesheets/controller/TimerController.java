package timesheets.controller;

import exception.ConflictException;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import timesheets.domain.Project;
import timesheets.domain.Task;
import timesheets.domain.TimeEntry;
import timesheets.domain.TimerSession;
import timesheets.dto.request.StartTimerRequest;
import timesheets.dto.response.ActiveTimerResponse;
import timesheets.dto.response.CreatedTimeEntryResponse;
import timesheets.dto.response.ErrorResponse;
import timesheets.dto.response.StopTimerResponse;
import timesheets.repository.ProjectRepository;
import timesheets.repository.TaskRepository;
import timesheets.security.SecurityUtils;
import timesheets.service.TimerService;

// the rest controller should handle the HTTP requests
@RestController
@RequestMapping("/api/timers") // my base URL it should be /api/timers
@RequiredArgsConstructor
public class TimerController {

  private final TimerService timerService;
  private final ProjectRepository projectRepository;
  private final TaskRepository taskRepository;
  private final SecurityUtils securityUtils;

  @PostMapping("/start") // the endpoint will look like POST /api/timers/start
  public ResponseEntity<?> startTimer(@Valid @RequestBody StartTimerRequest request) {
    try {
      TimerSession timer = timerService.startTimer(request);

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

  //pause the currently running timer
  @PostMapping("/pause")
  public ResponseEntity<ActiveTimerResponse> pauseTimer() {
    TimerSession timer = timerService.pauseTimer();

    ActiveTimerResponse response = convertToResponse(timer);

    return ResponseEntity.ok(response);
  }

  //resumes a timer
  @PostMapping("/resume")
  public ResponseEntity<ActiveTimerResponse> resumeTimer(){
    TimerSession timer = timerService.resumeTimer();

    ActiveTimerResponse response = convertToResponse(timer);

    return ResponseEntity.ok(response);
  }

  @PostMapping("/stop") // the endpoint will look like POST /api/timers/stop
  public ResponseEntity<StopTimerResponse> stopTimer() {

    TimerSession activeTimer = timerService.getActiveTimer();

    TimeEntry timeEntry = timerService.stopTimer(); // draft entry will be created

    StopTimerResponse response = convertToStopResponse(timeEntry, activeTimer.getId());

    return ResponseEntity.ok(response); // this should sent a 200 response
  }

  // should get the currently running timer
  @GetMapping("/active") // endpoint will look like GET /api/timers/active
  public ResponseEntity<ActiveTimerResponse> getActiveTimer() {

    TimerSession timer = timerService.getActiveTimer();

    if (timer != null) {
      return ResponseEntity.ok(convertToResponse(timer)); // the timer is running
    }
    // No active timer - return 204 No Content (frontend knows no timer running)
    return ResponseEntity.noContent().build();
  }

  @DeleteMapping("/discard") // the endpoint will look like DELETE /api/timers/discard
  public ResponseEntity<Void> discardTimer() {

    TimerSession activeTimer =
        timerService.getActiveTimer(); // should see if there is an active timer

    if (activeTimer == null) {
      return ResponseEntity.notFound().build(); // meaning no timer found
    }

    timerService.discardTimer(); // should just discard the timer

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
    response.setIsPaused(Boolean.TRUE.equals(timer.getIsPaused()));
    response.setPausedAt(timer.getPausedAt());

    // this will find the time that passed since the timer started
    if (timer.getIsRunning() && timer.getStartedAt() != null) {
      LocalDateTime now = LocalDateTime.now();

      // the total seconds from the start till now
      long totalSeconds = java.time.Duration.between(timer.getStartedAt(), now).toSeconds();
      if (timer.getPausedDurationSeconds() != null) {
        totalSeconds -= timer.getPausedDurationSeconds();
      }

      // if paused get the time since last paused
      if (Boolean.TRUE.equals(timer.getIsPaused()) && timer.getPausedAt() != null) {
        long secondsSincePause = java.time.Duration.between(timer.getPausedAt(), now).toSeconds();
        totalSeconds -= secondsSincePause;
      }

      totalSeconds = Math.max(0, totalSeconds);

      response.setElapsedSeconds((int) totalSeconds);
      response.setElapsedMinutes((int) (totalSeconds / 60));
    } else {
      response.setElapsedMinutes(0);
      response.setElapsedSeconds(0);
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
    response.setDurationMinutes(timeEntry.getDurationSeconds());

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

    createdEntry.setDurationMinutes(timeEntry.getDurationSeconds());
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
