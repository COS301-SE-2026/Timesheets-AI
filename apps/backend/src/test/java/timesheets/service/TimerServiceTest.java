package timesheets.service;

import timesheets.domain.*;
import timesheets.dto.request.StartTimerRequest;
import timesheets.repository.*;
import exception.ConflictException;
import exception.ResourceNotFoundException;
import exception.UnauthorizedException;
import timesheets.enums.TimeEntryStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;


//@ExtendWith = JUnit 5 - enables Mockito support
//MockitoExtension.class = tells JUnit to process Mockito annotations
@ExtendWith(MockitoExtension.class)
class TimerServiceTest {

    //this will Mock the DB
    @Mock
    private TimerSessionRepository timerSessionRepository;  //fake timer DB

    @Mock
    private TimeEntryRepository timeEntryRepository;        //fake time entry DB

    @Mock
    private WorkspaceMemberRepository workspaceMemberRepository;  //fake member DB

    @Mock
    private ProjectRepository projectRepository;            //fake project DB

    @Mock
    private TaskRepository taskRepository;                  //fake task DB

    @Mock
    private ProjectMemberRepository projectMemberRepository; //fake project-member DB

    //this is the class we are actually testing
    @InjectMocks //this will create a real service and inject all the @Mocks above

    private TimerService timerService;

    private UUID workspaceMemberId;
    private UUID userId;
    private UUID projectId;
    private UUID taskId;
    private WorkspaceMember workspaceMember;
    private Project project;
    private Task task;
    private StartTimerRequest request;

    
    @BeforeEach //JUnit will run before every test method, it will set new data so that the data does not interfere with the tests
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
        request.setNotes("Testing timer");
    }

    

    //the user will start the timer successfully, correct values should be returned
    @Test
    void startTimer_Success_ShouldCreateAndReturnTimer() {
        when(workspaceMemberRepository.findById(workspaceMemberId)).thenReturn(Optional.of(workspaceMember));

        when(workspaceMemberRepository.findAllByUserId(userId)).thenReturn(List.of(workspaceMember));
        when(timerSessionRepository.findFirstByWorkspaceMemberIdInAndIsRunningTrue(any())).thenReturn(Optional.empty());  //no active timer exists
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(projectMemberRepository.existsByProjectIdAndWorkspaceMemberId(projectId, workspaceMemberId)).thenReturn(true);  //user is assigned to project
        when(taskRepository.findById(taskId)).thenReturn(Optional.of(task));
        when(timerSessionRepository.save(any(TimerSession.class))).thenAnswer(invocation -> invocation.getArgument(0));  //return what was saved

        //this will call the actual method being tested
        //! think ACT
        TimerSession result = timerService.startTimer(workspaceMemberId, request);


        //should check if the values being tested are actually there
        //! this ASSERT
        assertThat(result).isNotNull();
        assertThat(result.getWorkspaceMemberId()).isEqualTo(workspaceMemberId);
        assertThat(result.getProjectId()).isEqualTo(projectId);
        assertThat(result.getTaskId()).isEqualTo(taskId);
        assertThat(result.getIsRunning()).isTrue();
        assertThat(result.getNotes()).isEqualTo("Testing timer");
        
        verify(timerSessionRepository, times(1)).save(any(TimerSession.class));
        //verify() is using  Mockito - checks that save() was called exactly once
    }


    //should see if starting a timer without a task is possible??
    //the timerId should still be null
    @Test
    void startTimer_WithoutTask_Success() {
        request.setTaskId(null);  //no task specified
        
        when(workspaceMemberRepository.findById(workspaceMemberId)).thenReturn(Optional.of(workspaceMember));
        when(workspaceMemberRepository.findAllByUserId(userId)).thenReturn(List.of(workspaceMember));
        when(timerSessionRepository.findFirstByWorkspaceMemberIdInAndIsRunningTrue(any())).thenReturn(Optional.empty());
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(projectMemberRepository.existsByProjectIdAndWorkspaceMemberId(projectId, workspaceMemberId)).thenReturn(true);
        when(timerSessionRepository.save(any(TimerSession.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TimerSession result = timerService.startTimer(workspaceMemberId, request);

        assertThat(result.getTaskId()).isNull();

        //verify taskRepository.findById was NEVER called (since no taskId provided)
        verify(taskRepository, never()).findById(any());
    }



    //the workspace member not found in the database
    @Test
    void startTimer_WorkspaceMemberNotFound_ShouldThrowException() {
        when(workspaceMemberRepository.findById(workspaceMemberId)).thenReturn(Optional.empty());  //member doesn't exist

        //assertThatThrownBy = AssertJ - asserts that an exception is thrown
        assertThatThrownBy(() -> timerService.startTimer(workspaceMemberId, request)).isInstanceOf(ResourceNotFoundException.class).hasMessageContaining("Workspace member not found");
    }



    //test 4: User already has an active timer
    //whats expected: Throws ConflictException (HTTP 409)
    @Test
    void startTimer_ActiveTimerExists_ShouldThrowConflictException() {
        TimerSession existingTimer = new TimerSession();
        existingTimer.setId(UUID.randomUUID());
        
        when(workspaceMemberRepository.findById(workspaceMemberId)).thenReturn(Optional.of(workspaceMember));
        when(workspaceMemberRepository.findAllByUserId(userId)).thenReturn(List.of(workspaceMember));
        when(timerSessionRepository.findFirstByWorkspaceMemberIdInAndIsRunningTrue(any())).thenReturn(Optional.of(existingTimer));  //active timer found!

        assertThatThrownBy(() -> timerService.startTimer(workspaceMemberId, request)).isInstanceOf(ConflictException.class).hasMessageContaining("Timer already active in another workspace");
    }



    //test 5: Project not found
    //whats expected: Throws ResourceNotFoundException
    @Test
    void startTimer_ProjectNotFound_ShouldThrowException() {
        when(workspaceMemberRepository.findById(workspaceMemberId)).thenReturn(Optional.of(workspaceMember));
        when(workspaceMemberRepository.findAllByUserId(userId)).thenReturn(List.of(workspaceMember));
        when(timerSessionRepository.findFirstByWorkspaceMemberIdInAndIsRunningTrue(any())).thenReturn(Optional.empty());
        when(projectRepository.findById(projectId)).thenReturn(Optional.empty());  //project doesn't exist

        assertThatThrownBy(() -> timerService.startTimer(workspaceMemberId, request)).isInstanceOf(ResourceNotFoundException.class).hasMessageContaining("Project not found");
    }



    //test 6: User is not assigned to the project
    //whats expected: Throws UnauthorizedException
    @Test
    void startTimer_UserNotAssignedToProject_ShouldThrowUnauthorizedException() {
        when(workspaceMemberRepository.findById(workspaceMemberId)).thenReturn(Optional.of(workspaceMember));
        when(workspaceMemberRepository.findAllByUserId(userId)).thenReturn(List.of(workspaceMember));
        when(timerSessionRepository.findFirstByWorkspaceMemberIdInAndIsRunningTrue(any())).thenReturn(Optional.empty());
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(projectMemberRepository.existsByProjectIdAndWorkspaceMemberId(projectId, workspaceMemberId)).thenReturn(false);  //user not a member of this project!

        assertThatThrownBy(() -> timerService.startTimer(workspaceMemberId, request)).isInstanceOf(UnauthorizedException.class).hasMessageContaining("not assigned to this project");
    }



    //test 7: Task provided does NOT belong to the specified project
    //whats expected: Throws IllegalArgumentException
    @Test
    void startTimer_TaskDoesNotBelongToProject_ShouldThrowException() {
        Task wrongTask = new Task();
        wrongTask.setId(taskId);
        wrongTask.setProjectId(UUID.randomUUID()); //Different project!
        
        when(workspaceMemberRepository.findById(workspaceMemberId)).thenReturn(Optional.of(workspaceMember));
        when(workspaceMemberRepository.findAllByUserId(userId)).thenReturn(List.of(workspaceMember));
        when(timerSessionRepository.findFirstByWorkspaceMemberIdInAndIsRunningTrue(any())).thenReturn(Optional.empty());
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(projectMemberRepository.existsByProjectIdAndWorkspaceMemberId(projectId, workspaceMemberId)).thenReturn(true);
        when(taskRepository.findById(taskId)).thenReturn(Optional.of(wrongTask));  //task belongs to different project

        assertThatThrownBy(() -> timerService.startTimer(workspaceMemberId, request)).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("Task does not belong to the specified project");
    }




    
    //?STOP TIMER TESTS - Testing the stopTimer() method

    //test 8: Stop timer successfully
    //whats expected: timer is stopped, DRAFT time entry is created
    @Test
    void stopTimer_Success_ShouldStopTimerAndCreateTimeEntry() {

        TimerSession activeTimer = new TimerSession();

        activeTimer.setId(UUID.randomUUID());
        activeTimer.setWorkspaceMemberId(workspaceMemberId);
        activeTimer.setProjectId(projectId);
        activeTimer.setTaskId(taskId);

        activeTimer.setStartedAt(LocalDateTime.now().minusHours(1));
        activeTimer.setIsRunning(true);
        activeTimer.setNotes("Testing notes");

        activeTimer.setPausedDurationSeconds(0L);

        when(workspaceMemberRepository.findById(workspaceMemberId)).thenReturn(Optional.of(workspaceMember));
        when(timerSessionRepository.findByWorkspaceMemberIdAndIsRunningTrue(workspaceMemberId)).thenReturn(Optional.of(activeTimer));
        when(timeEntryRepository.save(any(TimeEntry.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TimeEntry result = timerService.stopTimer(workspaceMemberId);

        assertThat(result).isNotNull();
        assertThat(result.getWorkspaceMemberId()).isEqualTo(workspaceMemberId);
        assertThat(result.getStatus()).isEqualTo(TimeEntryStatus.DRAFT);
        assertThat(result.getEntryType()).isEqualTo("timer");
        
        //check that timer was marked as stopped
        assertThat(activeTimer.getIsRunning()).isFalse();
        assertThat(activeTimer.getEndedAt()).isNotNull();
        
        verify(timerSessionRepository, times(1)).save(activeTimer);
        verify(timeEntryRepository, times(1)).save(any(TimeEntry.class));
    }





    //test 9: Stop timer with paused duration
    //whats expected: Paused time is subtracted from total duration
    @Test
    void stopTimer_WithPausedDuration_ShouldSubtractPausedTime() {
        TimerSession activeTimer = new TimerSession();
        activeTimer.setId(UUID.randomUUID());
        activeTimer.setWorkspaceMemberId(workspaceMemberId);
        activeTimer.setProjectId(projectId);
        activeTimer.setStartedAt(LocalDateTime.now().minusHours(2)); //120 minutes total
        activeTimer.setIsRunning(true);
        activeTimer.setPausedDurationSeconds(1800L); //30 minutes paused (1800 seconds)
        
        when(workspaceMemberRepository.findById(workspaceMemberId)).thenReturn(Optional.of(workspaceMember));
        when(timerSessionRepository.findByWorkspaceMemberIdAndIsRunningTrue(workspaceMemberId)).thenReturn(Optional.of(activeTimer));
        when(timeEntryRepository.save(any(TimeEntry.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TimeEntry result = timerService.stopTimer(workspaceMemberId);

        //120 minutes - 30 minutes = 90 minutes
        assertThat(result.getDurationMinutes()).isEqualTo(90);
    }

    //test 10: No active timer to stop
    //whats expected: Throws IllegalStateException
    @Test
    void stopTimer_NoActiveTimer_ShouldThrowException() {
        when(workspaceMemberRepository.findById(workspaceMemberId)).thenReturn(Optional.of(workspaceMember));
        when(timerSessionRepository.findByWorkspaceMemberIdAndIsRunningTrue(workspaceMemberId)).thenReturn(Optional.empty());  //no active timer found

        assertThatThrownBy(() -> timerService.stopTimer(workspaceMemberId)).isInstanceOf(IllegalStateException.class).hasMessageContaining("No active timer found");
    }

    
    //?DISCARD TIMER TESTS - Testing discardTimer() method
    

    //test 11: Discard timer successfully
    //whats expected: timer is deleted, no time entry is created
    @Test
    void discardTimer_Success_ShouldDeleteTimerWithoutCreatingEntry() {
        TimerSession activeTimer = new TimerSession();
        activeTimer.setId(UUID.randomUUID());
        activeTimer.setWorkspaceMemberId(workspaceMemberId);
        activeTimer.setIsRunning(true);

        when(timerSessionRepository.findByWorkspaceMemberIdAndIsRunningTrue(workspaceMemberId)).thenReturn(Optional.of(activeTimer));
        doNothing().when(timerSessionRepository).delete(activeTimer);

        timerService.discardTimer(workspaceMemberId);

        verify(timerSessionRepository, times(1)).delete(activeTimer);

        //verify timeEntryRepository.save was NEVER called
        verify(timeEntryRepository, never()).save(any(TimeEntry.class));
    }

    //test 12: No active timer to discard
    //whats expected: Throws IllegalStateException
    @Test
    void discardTimer_NoActiveTimer_ShouldThrowException() {
        when(timerSessionRepository.findByWorkspaceMemberIdAndIsRunningTrue(workspaceMemberId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> timerService.discardTimer(workspaceMemberId)).isInstanceOf(IllegalStateException.class).hasMessageContaining("No active timer found to discard");
    }

    

    //?GET ACTIVE TIMER TESTS - Testing getActiveTimer() method

    //test 13: Get active timer when one exists
    //whats expected: Returns the TimerSession object
    @Test
    void getActiveTimer_WhenTimerExists_ShouldReturnTimer() {
        TimerSession expectedTimer = new TimerSession();
        expectedTimer.setId(UUID.randomUUID());
        
        when(timerSessionRepository.findByWorkspaceMemberIdAndIsRunningTrue(workspaceMemberId)).thenReturn(Optional.of(expectedTimer));

        TimerSession result = timerService.getActiveTimer(workspaceMemberId);

        assertThat(result).isEqualTo(expectedTimer);
    }


    //test 14: Get active timer when none exists
    //whats expected: Returns null (not an exception)
    @Test
    void getActiveTimer_WhenNoTimer_ShouldReturnNull() {
        when(timerSessionRepository.findByWorkspaceMemberIdAndIsRunningTrue(workspaceMemberId)).thenReturn(Optional.empty());

        TimerSession result = timerService.getActiveTimer(workspaceMemberId);

        assertThat(result).isNull();
    }
}

