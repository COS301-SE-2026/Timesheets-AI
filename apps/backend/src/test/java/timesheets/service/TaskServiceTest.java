package timesheets.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import timesheets.domain.Project;
import timesheets.domain.Task;
import timesheets.domain.User;
import timesheets.domain.WorkspaceMember;
import timesheets.dto.response.TaskResponse;
import timesheets.repository.ProjectMemberRepository;
import timesheets.repository.ProjectRepository;
import timesheets.repository.TaskRepository;
import timesheets.repository.UserRepository;
import timesheets.repository.WorkspaceMemberRepository;
import timesheets.security.SecurityUtils;

@ExtendWith(MockitoExtension.class)
@DisplayName("TaskService Unit Tests")
class TaskServiceTest {

  @Mock private SecurityUtils securityUtils;
  @Mock private TaskRepository taskRepository;
  @Mock private ProjectRepository projectRepository;
  @Mock private ProjectMemberRepository projectMemberRepository;
  @Mock private WorkspaceMemberRepository workspaceMemberRepository;
  @Mock private UserRepository userRepository;

  @InjectMocks private TaskService taskService;

  private UUID workspaceMemberId;
  private UUID projectId;
  private UUID taskId;
  private UUID assignedWorkspaceMemberId;
  private UUID userId;
  private Project project;
  private Task task;
  private WorkspaceMember workspaceMember;
  private User user;

  @BeforeEach
  void setUp() {
    workspaceMemberId = UUID.randomUUID();
    projectId = UUID.randomUUID();
    taskId = UUID.randomUUID();
    assignedWorkspaceMemberId = UUID.randomUUID();
    userId = UUID.randomUUID();

    project = new Project();
    project.setId(projectId);
    project.setName("Test Project");
    project.setStatus("ACTIVE");

    task = new Task();
    task.setId(taskId);
    task.setProjectId(projectId);
    task.setTitle("Test Task");
    task.setDescription("Test Description");
    task.setPriority("MEDIUM");
    task.setStatus("TODO");
    task.setIsDeleted(false);
    task.setAssignedWorkspaceMemberId(assignedWorkspaceMemberId);

    workspaceMember = new WorkspaceMember();
    workspaceMember.setId(assignedWorkspaceMemberId);
    workspaceMember.setUserId(userId);

    user = new User();
    user.setId(userId);
    user.setFirstName("Enzokuhle");
    user.setLastName("Khumalo");
  }

  @Nested
  @DisplayName("Get Tasks For Project Tests")
  class GetTasksForProjectTests {

    @Test
    @DisplayName("return all tasks if user has access")
    void getTasksForProject() {

      // ARRANGE
      // need to verify that the user has access to these projects
      when(projectMemberRepository.existsByProjectIdAndWorkspaceMemberId(
              projectId, workspaceMemberId))
          .thenReturn(true);
      when(taskRepository.findByProjectIdAndIsDeletedFalse(projectId)).thenReturn(List.of(task));

      // I need the project name to be returned with each task as well
      when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));

      // this is is to help with getting the name of the person assigned to the task.
      when(workspaceMemberRepository.findById(assignedWorkspaceMemberId))
          .thenReturn(Optional.of(workspaceMember));
      when(userRepository.findById(userId)).thenReturn(Optional.of(user));

      // ACT: this will fetch all the tasks from the project
      List<TaskResponse> result = taskService.getTasksForProject(projectId, workspaceMemberId);

      // ASSERT: lets verify that we are getting all that we expect
      assertThat(result).isNotNull();
      assertThat(result).hasSize(1); // only expecting the mock task setup

      TaskResponse response = result.get(0);
      assertThat(response.getId()).isEqualTo(taskId);
      assertThat(response.getTitle()).isEqualTo("Test Task");
      assertThat(response.getDescription()).isEqualTo("Test Description");
      assertThat(response.getPriority()).isEqualTo("MEDIUM");
      assertThat(response.getStatus()).isEqualTo("TODO");

      // I want to make sure that the project name actually appears
      // it is a small thing but frontend really wants it
      assertThat(response.getProjectName()).isEqualTo("Test Project");

      assertThat(response.getAssignedToName()).isEqualTo("Enzokuhle Khumalo");

      // I want to confirm it checked access, fetched the tasks, and got the project details.
      verify(projectMemberRepository)
          .existsByProjectIdAndWorkspaceMemberId(projectId, workspaceMemberId);
      verify(taskRepository).findByProjectIdAndIsDeletedFalse(projectId);
      verify(projectRepository).findById(projectId);
      verify(workspaceMemberRepository).findById(assignedWorkspaceMemberId);
      verify(userRepository).findById(userId);
    }
  }

  @Nested
  @DisplayName("Get Task By ID Tests")
  class GetTaskByIdTests {

    @Test
    @DisplayName("returns a task when a user has access, successfully")
    void getTaskById() {

      /*ARRANGE
      - making sure that a user is a developer
      - get the task and that it is not deleted
      - the user has access to the project that the task is in
      */

      when(securityUtils.isAdmin()).thenReturn(false);
      when(securityUtils.isManager()).thenReturn(false);

      when(taskRepository.findById(taskId)).thenReturn(Optional.of(task));

      // the user has access to the project
      when(projectMemberRepository.existsByProjectIdAndWorkspaceMemberId(
              projectId, workspaceMemberId))
          .thenReturn(true);

      when(projectRepository.findById(projectId)).thenReturn(Optional.of(project)); // project name

      when(workspaceMemberRepository.findById(assignedWorkspaceMemberId))
          .thenReturn(Optional.of(workspaceMember));
      when(userRepository.findById(userId)).thenReturn(Optional.of(user));

      // ACT: to call the actual method
      TaskResponse result = taskService.getTaskResponseById(taskId, workspaceMemberId);

      // ASSERT: the values should match what I actually want
      assertThat(result).isNotNull();
      assertThat(result.getId()).isEqualTo(taskId);
      assertThat(result.getTitle()).isEqualTo("Test Task");
      assertThat(result.getDescription()).isEqualTo("Test Description");
      assertThat(result.getProjectName()).isEqualTo("Test Project");
      assertThat(result.getAssignedToName()).isEqualTo("Enzokuhle Khumalo");
      assertThat(result.getStatus()).isEqualTo("TODO");
      assertThat(result.getPriority()).isEqualTo("MEDIUM");

      // Verify all the repository calls happened
      verify(taskRepository, times(1)).findById(taskId); // tasks were fetched

      // checked the users access to the project, the number of calls should be 1
      verify(projectMemberRepository, times(1))
          .existsByProjectIdAndWorkspaceMemberId(projectId, workspaceMemberId);
      verify(projectRepository, times(1)).findById(projectId); // project name retrieved
      verify(workspaceMemberRepository, times(1)).findById(assignedWorkspaceMemberId);
      verify(userRepository, times(1)).findById(userId);
    }
  }

  @Nested
  @DisplayName("Get My Tasks Tests")
  class GetMyTasksTests {

    @Test
    @DisplayName("returns all tasks for the user")
    void getMyTasks() {
      /*
      ARRANGE
      - finds all the tasks assigned to the user
      - fetches the prject name
      - also get the name for display
      */
      when(taskRepository.findByAssignedWorkspaceMemberIdAndIsDeletedFalse(workspaceMemberId))
          .thenReturn(List.of(task));

      // the task also needs the project name and assigness full name
      when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
      when(workspaceMemberRepository.findById(assignedWorkspaceMemberId))
          .thenReturn(Optional.of(workspaceMember));
      when(userRepository.findById(userId)).thenReturn(Optional.of(user));

      // ACT: call the actual function
      List<TaskResponse> result = taskService.getMyTasks(workspaceMemberId);

      assertThat(result).isNotNull();
      assertThat(result).hasSize(1);
      assertThat(result.get(0).getId()).isEqualTo(taskId);
      assertThat(result.get(0).getProjectName())
          .isEqualTo("Test Project"); // project name is attatched
      assertThat(result.get(0).getAssignedToName())
          .isEqualTo("Enzokuhle Khumalo"); // making sure the name is also ther

      // ensuring that the repo was called correctly
      verify(taskRepository, times(1))
          .findByAssignedWorkspaceMemberIdAndIsDeletedFalse(workspaceMemberId);
    }
  }

  @Nested
  @DisplayName("Create Task Tests")
  class CreateTaskTests {
    // my tests
  }
}
