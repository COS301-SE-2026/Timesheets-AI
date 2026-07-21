package timesheets.service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import timesheets.domain.Project;
import timesheets.domain.Task;
import timesheets.domain.WorkspaceMember;
import timesheets.dto.response.TaskResponse;
import timesheets.repository.ProjectMemberRepository;
import timesheets.repository.ProjectRepository;
import timesheets.repository.TaskRepository;
import timesheets.repository.UserRepository;
import timesheets.repository.WorkspaceMemberRepository;
import timesheets.security.SecurityUtils;

@Service
@RequiredArgsConstructor
public class TaskService {

  private final SecurityUtils securityUtils;
  private final TaskRepository taskRepository;
  private final ProjectRepository projectRepository;
  private final ProjectMemberRepository projectMemberRepository;
  private final WorkspaceMemberRepository workspaceMemberRepository;
  private final UserRepository userRepository;

  // this gets all the active tasks of a project - only if the user has access to that project
  @Transactional(readOnly = true)
  public List<TaskResponse> getTasksForProject(UUID projectId, UUID workspaceMemberId) {

    // checks if user has access to that project
    if (!userHasAccessToProject(projectId, workspaceMemberId)) {
      throw new RuntimeException("You don't have access to  this project");
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
}
