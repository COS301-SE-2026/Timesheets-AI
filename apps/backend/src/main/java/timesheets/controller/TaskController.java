package timesheets.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import timesheets.dto.request.CreateTaskRequest;
import timesheets.dto.request.UpdateTaskRequest;
import timesheets.dto.response.TaskResponse;
import timesheets.security.SecurityUtils;
import timesheets.service.TaskService;

@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
public class TaskController {

  private final SecurityUtils securityUtils;
  private final TaskService taskService;

  // gets all the active tasks for a specific project
  @Operation(
      summary = "Get project tasks",
      description = "Gets all active tasks for a specific project.")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Tasks retrieved successfully"),
    @ApiResponse(responseCode = "403", description = "User does not have access to the project")
  })
  @GetMapping("/project/{projectId}")
  public ResponseEntity<List<TaskResponse>> getTasksForProject(@PathVariable UUID projectId) {
    UUID workspaceMemberId = securityUtils.getDefaultWorkspaceMemberId();

    List<TaskResponse> tasks = taskService.getTasksForProject(projectId, workspaceMemberId);
    return ResponseEntity.ok(tasks);
  }

  // this will gets a single task using the id and return the full details
  @Operation(summary = "Get task", description = "Gets the details of a specific task.")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Task retrieved successfully"),
    @ApiResponse(responseCode = "404", description = "Task not found"),
    @ApiResponse(responseCode = "403", description = "User does not have access to the task")
  })
  @GetMapping("/{taskId}")
  public ResponseEntity<TaskResponse> getTaskById(@PathVariable UUID taskId) {
    UUID workspaceMemberId = securityUtils.getDefaultWorkspaceMemberId();

    TaskResponse task = taskService.getTaskResponseById(taskId, workspaceMemberId);
    return ResponseEntity.ok(task);
  }

  // the will get all the tasks assigned for a specific user
  @Operation(
      summary = "Get my tasks",
      description = "Gets all active tasks assigned to the current user.")
  @ApiResponses({@ApiResponse(responseCode = "200", description = "Tasks retrieved successfully")})
  @GetMapping("/my-tasks")
  public ResponseEntity<List<TaskResponse>> getMyTasks() {
    UUID workspaceMemberId = securityUtils.getDefaultWorkspaceMemberId();

    List<TaskResponse> tasks = taskService.getMyTasks(workspaceMemberId);
    return ResponseEntity.ok(tasks);
  }

  // this will create a new task
  @Operation(summary = "Create task", description = "Creates a new task for a project.")
  @ApiResponses({
    @ApiResponse(responseCode = "201", description = "Task created successfully"),
    @ApiResponse(responseCode = "400", description = "Invalid task request"),
    @ApiResponse(
        responseCode = "403",
        description = "User does not have permission to create the task"),
    @ApiResponse(responseCode = "404", description = "Project not found"),
    @ApiResponse(responseCode = "409", description = "Task cannot be created for the project")
  })
  @PostMapping
  public ResponseEntity<TaskResponse> createTask(@Valid @RequestBody CreateTaskRequest request) {
    UUID workspaceMemberId = securityUtils.getDefaultWorkspaceMemberId();

    TaskResponse response = taskService.createTask(request, workspaceMemberId);

    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }

  // gets all the tasks for the team
  @Operation(
      summary = "Get team tasks",
      description = "Gets all active tasks for the current team.")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Team tasks retrieved successfully"),
    @ApiResponse(
        responseCode = "403",
        description = "User does not have permission to view team tasks")
  })
  @GetMapping("/team-tasks")
  public ResponseEntity<List<TaskResponse>> getTeamTasks() {
    return ResponseEntity.ok(taskService.getTeamTasks());
  }

  // to update the editbale fields in the task
  @Operation(
      summary = "Update task",
      description = "Updates the editable details of an existing task.")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Task updated successfully"),
    @ApiResponse(responseCode = "400", description = "Invalid task update request"),
    @ApiResponse(
        responseCode = "403",
        description = "User does not have permission to update the task"),
    @ApiResponse(responseCode = "404", description = "Task not found")
  })
  @PatchMapping("/{taskId}")
  public ResponseEntity<TaskResponse> updateTask(
      @PathVariable UUID taskId, @Valid @RequestBody UpdateTaskRequest.UpdateTask request) {

    UUID workspaceMemberId = securityUtils.getDefaultWorkspaceMemberId();
    TaskResponse response = taskService.updateTask(taskId, request, workspaceMemberId);

    return ResponseEntity.ok(response);
  }
}
