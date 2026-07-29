package timesheets.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
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
import timesheets.domain.TimerSession;
import timesheets.domain.WorkspaceMember;
import timesheets.dto.request.StartTimerRequest;
import timesheets.repository.ProjectMemberRepository;
import timesheets.repository.ProjectRepository;
import timesheets.repository.TaskRepository;
import timesheets.repository.TimeEntryRepository;
import timesheets.repository.TimerSessionRepository;
import timesheets.repository.WorkspaceMemberRepository;
import timesheets.security.SecurityUtils;

/*
-following the principle from the coding handbook of Arrange, Act, Assert
- unit tests for the TimerService class
*/

@ExtendWith(MockitoExtension.class)
@DisplayName("TimerService Unit Tests")
class TimerServiceTest {

  @Mock private TimerSessionRepository timerSessionRepository;
  @Mock private TimeEntryRepository timeEntryRepository;
  @Mock private WorkspaceMemberRepository workspaceMemberRepository;
  @Mock private ProjectRepository projectRepository;
  @Mock private TaskRepository taskRepository;
  @Mock private ProjectMemberRepository projectMemberRepository;
  @Mock private SecurityUtils securityUtils;
  @Mock private TimesheetService timesheetService;

  @InjectMocks private TimerService timerService;

  private UUID workspaceMemberId;
  private UUID userId;
  private UUID projectId;
  private UUID taskId;
  private WorkspaceMember workspaceMember;
  private Project project;
  private Task task;
  private StartTimerRequest request;

  @BeforeEach
  void setUp() {
    workspaceMemberId = UUID.randomUUID();
    userId = UUID.randomUUID();
    projectId = UUID.randomUUID();
    taskId = UUID.randomUUID();

    workspaceMember = new WorkspaceMember();
    workspaceMember.setId(workspaceMemberId);
    workspaceMember.setUserId(userId);

    project = new Project();
    project.setId(projectId);
    project.setName("Test Project");

    task = new Task();
    task.setId(taskId);
    task.setProjectId(projectId);
    task.setTitle("Test Task");

    request = new StartTimerRequest();
    request.setProjectId(projectId);
    request.setTaskId(taskId);
  }

  @Nested
  @DisplayName("Start Timer Tests")
  class StartTimerTests {

    @Test
    @DisplayName("should start timer successfully")
    void startTimerSuccessfully() {

      // ARRANGE: setting up the valid details, project and tasks
      // the user who is starting the timer
      when(securityUtils.getDefaultWorkspaceMemberId()).thenReturn(workspaceMemberId);

      // checking that the user is a valid workspace member
      when(workspaceMemberRepository.findById(workspaceMemberId))
          .thenReturn(Optional.of(workspaceMember));

      when(workspaceMemberRepository.findByUserId(userId)).thenReturn(List.of(workspaceMember));

      // making sure that no active timer exists
      when(timerSessionRepository.findFirstByWorkspaceMemberIdInAndIsRunningTrue(anyList()))
          .thenReturn(Optional.empty());

      when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));

      // user gets assigned to the project
      when(projectMemberRepository.existsByProjectIdAndWorkspaceMemberId(
              projectId, workspaceMemberId))
          .thenReturn(true);

      when(taskRepository.findById(taskId)).thenReturn(Optional.of(task));
      when(timerSessionRepository.save(any(TimerSession.class)))
          .thenAnswer(invocation -> invocation.getArgument(0));

      /*
      - ACT: starts the timer
      - this should be getting the workspace memeber ID
      - validate the workspace member exists
      - checks if there are active timers
      - validates if the project exists and is assigned
      - creates a new session
      */

      TimerSession result = timerService.startTimer(request);

      // ASSERT: checking that it was created with the correct data
      assertThat(result).isNotNull();
      assertThat(result.getWorkspaceMemberId()).isEqualTo(workspaceMemberId);
      assertThat(result.getProjectId()).isEqualTo(projectId);
      assertThat(result.getTaskId()).isEqualTo(taskId);
      assertThat(result.getIsRunning()).isTrue();
      assertThat(result.getIsPaused()).isFalse();
      assertThat(result.getPausedDurationSeconds()).isEqualTo(0L);

      verify(timerSessionRepository, times(1)).save(any(TimerSession.class));
    }
  }
}
