package timesheets.controller;

import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import timesheets.dto.response.TaskResponse;
import timesheets.security.SecurityUtils;
import timesheets.service.TaskService;
import timesheets.domain.Task;

@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
public class TaskController {

  private final SecurityUtils securityUtils;
  private final TaskService taskService;

  //gets all the active tasks for a specific project
  @GetMapping("/project/{projectId}")
  public ResponseEntity<List<TaskResponse>> getTasksForProject(@PathVariable UUID projectId) {
    UUID workspaceMemberId = securityUtils.getDefaultWorkspaceMemberId();

    List<TaskResponse> tasks = taskService.getTasksForProject(projectId, workspaceMemberId);
    return ResponseEntity.ok(tasks);
  }

  //this will gets a single task using the id and return the full details
  @GetMapping("/{taskId}")
  public ResponseEntity<TaskResponse> getTaskById(@PathVariable UUID taskId) {
    UUID workspaceMemberId = securityUtils.getDefaultWorkspaceMemberId();

    TaskResponse task = taskService.getTaskResponseById(taskId, workspaceMemberId);
    return ResponseEntity.ok(task);
  }

  //the will get all the tasks assigned for a specific user
  @GetMapping("/my-tasks")
  public ResponseEntity<List<TaskResponse>> getMyTasks(){
    UUID workspaceMemberId = securityUtils.getDefaultWorkspaceMemberId();

    List<TaskResponse> tasks = taskService.getMyTasks(workspaceMemberId);
    return ResponseEntity.ok(tasks);
  }
}
