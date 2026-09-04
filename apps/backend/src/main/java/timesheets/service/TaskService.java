package timesheets.service;

import exception.AccessDeniedException;
import exception.BadRequestException;
import exception.ResourceNotFoundException;
import exception.StateConflictException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import timesheets.domain.Project;
import timesheets.domain.ProjectMember;
import timesheets.domain.Task;
import timesheets.domain.WorkspaceMember;
import timesheets.dto.request.CreateTaskRequest;
import timesheets.dto.response.JiraIssueResponse;
import timesheets.dto.response.TaskResponse;
import timesheets.integration.issue.JiraAdapter;
import timesheets.repository.ProjectMemberRepository;
import timesheets.repository.ProjectRepository;
import timesheets.repository.TaskRepository;
import timesheets.repository.UserRepository;
import timesheets.repository.WorkspaceMemberRepository;
import timesheets.security.SecurityUtils;

@Service
@RequiredArgsConstructor
@Slf4j
public class TaskService {

  private final SecurityUtils securityUtils;
  private final TaskRepository taskRepository;
  private final ProjectRepository projectRepository;
  private final ProjectMemberRepository projectMemberRepository;
  private final WorkspaceMemberRepository workspaceMemberRepository;
  private final UserRepository userRepository;
  private final JiraAdapter jiraAdapter;

  // this gets all the active tasks of a project - only if the user has access to
  // that project
  @Transactional(readOnly = true)
  public List<TaskResponse> getTasksForProject(UUID projectId, UUID workspaceMemberId) {

    // checks if user has access to that project
    if (!userHasAccessToProject(projectId, workspaceMemberId)) {
      throw new AccessDeniedException("You don't have access to  this project");
    }

    List<Task> tasks = taskRepository.findByProjectIdAndIsDeletedFalse(projectId);

    // gets the project name that can be used for display
    String projectName =
        projectRepository.findById(projectId).map(Project::getName).orElse("Unknown Project");

    // each task converted to a response
    return tasks.stream()
        .map(
            task -> {
              String assignedToName = getAssignedToName(task.getAssignedWorkspaceMemberId());
              return TaskResponse.fromWithDetails(task, projectName, assignedToName);
            })
        .collect(Collectors.toList());
  }

  // gets a task by it's id with the full details for display
  @Transactional(readOnly = true)
  public TaskResponse getTaskResponseById(UUID taskId, UUID workspaceMemberId) {
    Task task = getTaskById(taskId);

    // checks if the task has been deleted
    if (Boolean.TRUE.equals(task.getIsDeleted())) {
      throw new ResourceNotFoundException("Task has been deleted");
    }

    if (!userHasAccessToProject(task.getProjectId(), workspaceMemberId)) {
      throw new AccessDeniedException("You do not have access to this task");
    }

    String projectName =
        projectRepository.findById(task.getProjectId()).map(Project::getName).orElse("Unknown Pro");

    String assignedToName = getAssignedToName(task.getAssignedWorkspaceMemberId());

    return TaskResponse.fromWithDetails(task, projectName, assignedToName);
  }

  // this gets the task by the id - internal entity
  @Transactional(readOnly = true)
  public Task getTaskById(UUID taskId) {
    return taskRepository
        .findById(taskId)
        .orElseThrow(() -> new ResourceNotFoundException("Task not found"));
  }

  @Transactional(readOnly = true)
  public List<TaskResponse> getMyTasks(UUID workspaceMemberId) {
    List<Task> tasks =
        taskRepository.findByAssignedWorkspaceMemberIdAndIsDeletedFalse(workspaceMemberId);

    return tasks.stream()
        .map(
            task -> {
              String projectName =
                  projectRepository
                      .findById(task.getProjectId())
                      .map(Project::getName)
                      .orElse("Unknown Project");
              String assignedToName = getAssignedToName(task.getAssignedWorkspaceMemberId());
              return TaskResponse.fromWithDetails(task, projectName, assignedToName);
            })
        .collect(Collectors.toList());
  }

  // this creates a new task
  @Transactional
  public TaskResponse createTask(CreateTaskRequest request, UUID workspaceMemberId) {
    UUID projectId = request.getProjectId();

    // to verify that the user has access to create tasks on the project
    if (!userHasAccessToProject(projectId, workspaceMemberId)) {
      throw new AccessDeniedException("You don't have permission to create tasks on this project");
    }

    // to verify that the project exists and is not archived
    Project project =
        projectRepository
            .findById(projectId)
            .orElseThrow(
                () -> new ResourceNotFoundException("Project not found with id: " + projectId));

    if ("ARCHIVED".equals(project.getStatus())) {
      throw new StateConflictException("Cannot create tasks on an archived project");
    }

    // checking if the user can assign tasks to others
    boolean isTaskAssigner =
        isProjectManager(projectId, workspaceMemberId)
            || securityUtils.isAdmin()
            || securityUtils.isManager();

    // developers should not be able to create tasks and assign them to others
    if (!isTaskAssigner
        && request.getAssignedWorkspaceMemberId() != null
        && !request.getAssignedWorkspaceMemberId().equals(workspaceMemberId)) {
      throw new RuntimeException("Developers can only create tasks assigned to themselves");
    }

    // this checks if there is a valid parent task and if that parent task is part of the same
    // project
    if (request.getParentTaskId() != null) {
      Task parentTask =
          taskRepository
              .findById(request.getParentTaskId())
              .orElseThrow(
                  () ->
                      new ResourceNotFoundException(
                          "Parent task not found with id: " + request.getParentTaskId()));

      if (!parentTask.getProjectId().equals(projectId)) {
        throw new BadRequestException("Parent task does not belong to this project");
      }
    }

    // builds the task
    Task task = new Task();
    task.setProjectId(projectId);
    task.setTitle(request.getTitle());
    task.setDescription(request.getDescription());
    task.setParentTaskId(request.getParentTaskId());
    task.setEstimatedHours(request.getEstimatedHours());

    if (request.getAssignedWorkspaceMemberId() != null) {
      task.setAssignedWorkspaceMemberId(request.getAssignedWorkspaceMemberId());
    } else {
      task.setAssignedWorkspaceMemberId(workspaceMemberId);
    }

    task.setDueDate(request.getDueDate());
    task.setPriority(request.getPriority() != null ? request.getPriority() : "MEDIUM");
    task.setStatus(request.getStatus() != null ? request.getStatus() : "TODO");
    task.setIsDeleted(false);

    // if the task gets marked as done right away, then the timestamp is updated
    if ("DONE".equals(task.getStatus())) {
      task.setCompletedAt(LocalDateTime.now());
    }

    // if the user wants to create a Jira issue then this is requested, want to make this optional
    // for the user
    if (request.isCreateJiraIssue() && request.getJiraDetails() != null) {
      try {
        // when there is no project key, the default is what will be use
        if (request.getJiraDetails().getProjectKey() == null
            || request.getJiraDetails().getProjectKey().isEmpty()) {

          String defaultProjectKey = getDefaultJiraProject(workspaceMemberId);

          if (defaultProjectKey != null) {
            request.getJiraDetails().setProjectKey(defaultProjectKey);
            log.info("Using default Jira project: {}", defaultProjectKey);
          } else {
            throw new RuntimeException(
                "No default Jira project found. Please specify a project key.");
          }
        }

        // going to be using the adapter to create the issue
        JiraIssueResponse jiraIssue =
            jiraAdapter.createIssue(workspaceMemberId, request.getJiraDetails());

        // the jira ticket will be stored here, so that it is stored in the system
        task.setJiraTicketKey(jiraIssue.getKey());

        log.info("Created Jira issue {} for task '{}'", jiraIssue.getKey(), request.getTitle());

      } catch (Exception e) {
        log.error(
            "Failed to create Jira issue for task '{}': {}", request.getTitle(), e.getMessage());

        throw new RuntimeException("Failed to create Jira issue: " + e.getMessage(), e);
      }
    }

    Task savedTask = taskRepository.save(task);

    String projectName = project.getName();
    String assignedToName = getAssignedToName(savedTask.getAssignedWorkspaceMemberId());

    return TaskResponse.fromWithDetails(savedTask, projectName, assignedToName);
  }

  // ! helper functions
  // checks if the user has access to the project
  private boolean userHasAccessToProject(UUID projectId, UUID workspaceMemeberId) {

    boolean isAdmin = securityUtils.isAdmin();
    boolean isManager = securityUtils.isManager();

    // admins and managers have access to all the projects
    if (isAdmin || isManager) {
      return true;
    }

    // the dev must be a member of the project in order to see it
    return projectMemberRepository.existsByProjectIdAndWorkspaceMemberId(
        projectId, workspaceMemeberId);
  }

  // gets the name of the user assigned to that task
  private String getAssignedToName(UUID workspaceMemberId) {
    if (workspaceMemberId == null) {
      return "Unassigned";
    }

    return workspaceMemberRepository
        .findById(workspaceMemberId)
        .map(WorkspaceMember::getUserId)
        .flatMap(userRepository::findById)
        .map(user -> user.getFirstName() + " " + user.getLastName())
        .orElse("Unknown user");
  }

  // checks if the user is a project manager for a particular project
  private boolean isProjectManager(UUID projectId, UUID workspaceMemberId) {
    return projectMemberRepository
        .findByProjectIdAndWorkspaceMemberId(projectId, workspaceMemberId)
        .map(ProjectMember::getIsProjectManager)
        .orElse(false);
  }

  // had to do research on how to extract in this way
  private String getDefaultJiraProject(UUID workspaceMemberId) {
    try {
      List<JiraIssueResponse> issues = jiraAdapter.getIssues(workspaceMemberId);

      if (issues != null && !issues.isEmpty()) {
        // the project key can be taken from the first issue
        String projectKey = issues.get(0).getProjectKey();

        if (projectKey != null && !projectKey.isEmpty()) {
          log.debug("Using default Jira project from first issue: {}", projectKey);
          return projectKey;
        }
      }

      log.debug("No existing Jira issues found to determine default project");
      return null;

    } catch (Exception e) {
      log.warn("Could not determine default Jira project: {}", e.getMessage());
      return null;
    }
  }
}
