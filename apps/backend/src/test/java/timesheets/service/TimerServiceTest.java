package timesheets.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
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

  private TimerSession createActiveTimer() {
    TimerSession timer = new TimerSession();
    timer.setId(UUID.randomUUID());
    timer.setWorkspaceMemberId(workspaceMemberId);
    timer.setProjectId(projectId);
    timer.setTaskId(taskId);
    timer.setStartedAt(LocalDateTime.now().minusHours(1));
    timer.setIsRunning(true);
    timer.setIsPaused(false);
    timer.setPausedDurationSeconds(0L);
    timer.setPausedAt(null);
    return timer;
  }

  private TimerSession createPausedTimer() {
    TimerSession timer = createActiveTimer();
    timer.setIsPaused(true);
    timer.setPausedAt(LocalDateTime.now().minusMinutes(30));
    timer.setPausedDurationSeconds(1800L);
    return timer;
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

  @Nested
  @DisplayName("Pause Timer Tests")
  class PauseTimerTests {

    @Test
    @DisplayName("should pause active timer successfully")
    void pauseTimer() {
      // ARRANGE:creating an active timer
      TimerSession activeTimer = createActiveTimer();

      /*
      - when returning the current user, test user should be
      - for an active timer, return the one that was just created
       */
      when(securityUtils.getDefaultWorkspaceMemberId()).thenReturn(workspaceMemberId);
      when(timerSessionRepository.findByWorkspaceMemberIdAndIsRunningTrue(workspaceMemberId))
          .thenReturn(Optional.of(activeTimer));

      // when a timer is saved, just give back whatever was saved
      when(timerSessionRepository.save(any(TimerSession.class)))
          .thenAnswer(invocation -> invocation.getArgument(0));

      // ACT: calling the pause timer function
      TimerSession result = timerService.pauseTimer();

      // ASSERT:verifying that the timer is actually paused
      assertThat(result).isNotNull();
      assertThat(result.getIsPaused()).isTrue();
      assertThat(result.getPausedAt()).isNotNull();

      // checking that the timer was paused exactly 1 time
      verify(timerSessionRepository, times(1)).save(activeTimer);
    }
  }

  @Nested
  @DisplayName("Resume Timer Tests")
  class ResumeTimerTests {

    @Test
    @DisplayName("should resume paused timer successfully")
    void resumeTimer() {

      // ARRANGE: trying to simulate the paused timer state
      TimerSession pausedTimer = createPausedTimer();

      when(securityUtils.getDefaultWorkspaceMemberId()).thenReturn(workspaceMemberId);

      // when an active timer is returned, the paused timer is the one that is returned
      when(timerSessionRepository.findByWorkspaceMemberIdAndIsRunningTrue(workspaceMemberId))
          .thenReturn(Optional.of(pausedTimer));
      when(timerSessionRepository.save(any(TimerSession.class)))
          .thenAnswer(invocation -> invocation.getArgument(0));

      // ACT: actually calling the actual resume function
      TimerSession result = timerService.resumeTimer();

      // ASSERT
      assertThat(result).isNotNull(); // the timer still exists
      assertThat(result.getIsPaused()).isFalse(); // no longer paused
      assertThat(result.getPausedAt()).isNull(); // timestamp cleared
      assertThat(result.getPausedDurationSeconds()).isGreaterThan(0);

      verify(timerSessionRepository, times(1)).save(pausedTimer);
    }
  }

  @Nested
  @DisplayName("Get Active Timer Tests")
  class GetActiveTimerTests {

    @Test
    @DisplayName("returns an active timer")
    void getActiveTimerExists() {

      // ARRANGE: to simulate an active timer
      TimerSession activeTimer = createActiveTimer();

      when(securityUtils.getDefaultWorkspaceMemberId()).thenReturn(workspaceMemberId);

      // the test timer should be returned
      when(timerSessionRepository.findByWorkspaceMemberIdAndIsRunningTrue(workspaceMemberId))
          .thenReturn(Optional.of(activeTimer));

      // ACT: calling the method we are testing
      TimerSession result = timerService.getActiveTimer();

      // ASSERT
      assertThat(result).isNotNull();
      assertThat(result.getId())
          .isEqualTo(
              activeTimer.getId()); // making sure that the actual test timer is what is returned
      assertThat(result.getWorkspaceMemberId()).isEqualTo(workspaceMemberId);
      assertThat(result.getIsRunning()).isTrue();

      // checking that the repo was called to get the actual timer
      verify(timerSessionRepository, times(1))
          .findByWorkspaceMemberIdAndIsRunningTrue(workspaceMemberId);
    }
  }

  @Nested
  @DisplayName("Stop Timer Tests")
  class StopTimerTests {

    @Test
    @DisplayName("this should stop a timer")
    void stopTimer() {

      /*
        ARRANGE
      - remember how a time-entry is added to a timesheet when a timer is stopped
      - so we are trying to simulate doing that
       */
      TimerSession activeTimer = createActiveTimer();
      Timesheet timesheet = new Timesheet();
      timesheet.setId(UUID.randomUUID());

      when(securityUtils.getDefaultWorkspaceMemberId()).thenReturn(workspaceMemberId);

      // when looking for an active timer, should return the one I just created
      when(timerSessionRepository.findByWorkspaceMemberIdAndIsRunningTrue(workspaceMemberId))
          .thenReturn(Optional.of(activeTimer));
      when(timesheetService.getOrCreateTimesheet(any(LocalDate.class), any(LocalDate.class)))
          .thenReturn(timesheet);

      // to create a time entry and timer session, just pass what I give it
      when(timeEntryRepository.save(any(TimeEntry.class)))
          .thenAnswer(invocation -> invocation.getArgument(0));
      when(timerSessionRepository.save(any(TimerSession.class)))
          .thenAnswer(invocation -> invocation.getArgument(0));

      // ACT: calling the actual stop timer method
      TimeEntry result = timerService.stopTimer();

      // ASSERT
      assertThat(result).isNotNull();
      assertThat(result.getWorkspaceMemberId())
          .isEqualTo(workspaceMemberId); // should belong to the same user
      assertThat(result.getEntryType())
          .isEqualTo("TIMER"); // should mark as how the entry was entered
      assertThat(result.getIsLocked()).isFalse(); // should not be locked yet since its a draft
      assertThat(activeTimer.getIsRunning()).isFalse(); // timer should not be running
      assertThat(activeTimer.getEndedAt()).isNotNull();

      // want to check that the timer got saved and a time entry was saved
      verify(timerSessionRepository, times(1)).save(activeTimer);
      verify(timeEntryRepository, times(1)).save(any(TimeEntry.class));
    }
  }

  @Nested
  @DisplayName("Discard Timer Tests")
  class DiscardTimerTests {

    @Test
    @DisplayName("should discard active timer successfully")
    void discardTimer() {

      // ARRANGE: an active timer exists
      TimerSession activeTimer = createActiveTimer();

      when(securityUtils.getDefaultWorkspaceMemberId()).thenReturn(workspaceMemberId);

      // to essentially return the timer that was just created, when looking for an active timer
      when(timerSessionRepository.findByWorkspaceMemberIdAndIsRunningTrue(workspaceMemberId))
          .thenReturn(Optional.of(activeTimer));

      // ACT: should call the actual discard timer function
      timerService.discardTimer();

      // ASSERT: verifying that the timer was actually deleted and that nothing was saved
      verify(timerSessionRepository, times(1)).delete(activeTimer);
      verify(timerSessionRepository, never()).save(any());
    }


    @Test
    @DisplayName("should throw an exception when there is no timer to discard")
    void discardTimerWhenNoActiveTimer() {

      // ARRANGE: simulating what happens when there is no active timer
      when(securityUtils.getDefaultWorkspaceMemberId()).thenReturn(workspaceMemberId);

      //simulating how when a repo is called and then no timer is returned
      when(timerSessionRepository.findByWorkspaceMemberIdAndIsRunningTrue(workspaceMemberId))
          .thenReturn(Optional.empty());

       
      //ACT & ASSERT: a proper error message should be returned
      assertThatThrownBy(() -> timerService.discardTimer())
          .isInstanceOf(IllegalStateException.class)
          .hasMessage("No active timer found to discard");

      //confirming that it was never deleted
      verify(timerSessionRepository, never()).delete(any());
    }
  }
}
