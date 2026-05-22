import { ComponentFixture, TestBed } from '@angular/core/testing';
import { LogtimeComponent } from './logtime.component';

// Unit tests for log time page

describe('LogtimeComponent', () => {
  let component: LogtimeComponent;
  let fixture: ComponentFixture<LogtimeComponent>;

  // Mock Guid/UUID constants mimicking backend entities for reliable data matching
  const projectOneId = '00000000-0000-0000-0001-000000000001';
  const projectTwoId = '00000000-0000-0000-0001-000000000002';
  const projectThreeId = '00000000-0000-0000-0001-000000000003';

  const taskOneId = '00000000-0000-0000-0002-000000000001';
  const taskTwoId = '00000000-0000-0000-0002-000000000002';
  const taskThreeId = '00000000-0000-0000-0002-000000000003';
  const taskFourId = '00000000-0000-0000-0002-000000000004';

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [LogtimeComponent]
    }).compileComponents();

    fixture = TestBed.createComponent(LogtimeComponent);
    component = fixture.componentInstance;

    fixture.detectChanges();
  });

  afterEach(() => {
    jest.useRealTimers();
  });

  // Basic sanity check
  it('should create', () => {
    expect(component).toBeTruthy();
  });

  // Initialisation checks
  it('should initialise the view options, status options, and forms used by the template', () => {
    expect(component.viewOptions).toEqual(['Day', 'Week', 'Month']);
    expect(component.statusOptions).toEqual([
      'All',
      'DRAFT',
      'SUBMITTED',
      'APPROVED',
      'REJECTED'
    ]);

    expect(component.filterForm.controls.from.value).toBeTruthy();
    expect(component.filterForm.controls.to.value).toBeTruthy();

    expect(component.entryForm.controls.projectId.value).toBe(projectOneId);
    expect(component.timerForm.controls.projectId.value).toBe(projectOneId);
  });

  // Manual panel open/close
  it('should open and close the manual entry panel', () => {
    component.openManualPanel();

    expect(component.activePanel()).toBe('manual');
    expect(component.isEditMode()).toBe(false);

    component.closePanel();

    expect(component.activePanel()).toBeNull();
  });

  // Timer panel
  it('should open the timer panel', () => {
    component.openTimerPanel();

    expect(component.activePanel()).toBe('timer');
  });

  // Duration calculation
  it('should calculate duration when manual entry times change', () => {
    component.openManualPanel();

    component.entryForm.patchValue({
      startTime: '08:15',
      endTime: '10:45'
    });

    expect(component.entryForm.controls.durationMinutes.value).toBe(150);
    expect(
      component.formatDuration(
        component.entryForm.controls.durationMinutes.value
      )
    ).toBe('2h 30m');
  });

  // Validation checks
  it('should show validation when end time is before start time', () => {
    component.openManualPanel();

    component.entryForm.patchValue({
      startTime: '11:00',
      endTime: '10:00'
    });

    expect(component.endTimeInvalid).toBe(true);

    component.saveEntry();

    expect(component.conflictMessage()).toBe(
      'End time must be after start time.'
    );
  });

  // Save manual entry
  it('should save a new manual entry', () => {
    const initialCount = component.entries().length;

    component.openManualPanel();

    component.entryForm.patchValue({
      projectId: projectThreeId,
      taskId: taskFourId,
      entryType: 'DESIGN',
      startTime: '22:00',
      endTime: '23:00',
      description: 'Designed the log time page.'
    });

    component.saveEntry();

    expect(component.conflictMessage()).toBe('');
    expect(component.entries().length).toBe(initialCount + 1);
    expect(component.entries()[0].projectId).toBe(projectThreeId);
    expect(component.entries()[0].durationMinutes).toBe(60);
    expect(component.toastMessage()).toBe('Time entry saved.');
    expect(component.activePanel()).toBeNull();
  });

  // New task creation
  it('should create a new task when saving a manual entry with a custom task name', () => {
    const initialTaskCount = component.tasks().length;

    component.openManualPanel();

    component.newTaskFormContext.set('manual');
    component.newTaskTitle.set('API integration spike');

    component.entryForm.patchValue({
      projectId: projectTwoId,
      startTime: '22:00',
      endTime: '23:00',
      description: 'Spike work on API integration.'
    });

    component.saveEntry();

    const createdTask = component
      .tasks()
      .find((task) => task.title === 'API integration spike');

    expect(component.tasks().length).toBe(initialTaskCount + 1);
    expect(createdTask?.projectId).toBe(projectTwoId);
    expect(component.entries()[0].taskId).toBe(createdTask?.id);
    expect(component.newTaskFormContext()).toBeNull();
  });

  // Task name required
  it('should require a task name when creating a new task', () => {
    component.openManualPanel();

    component.newTaskFormContext.set('manual');
    component.newTaskTitle.set('   ');

    component.saveEntry();

    expect(component.newTaskTitleError()).toBe(true);
    expect(component.activePanel()).toBe('manual');
  });

  // Overlap validation
  it('should prevent overlapping manual entries', () => {
    component.openManualPanel();

    component.entryForm.patchValue({
      projectId: projectOneId,
      taskId: taskOneId,
      startTime: '10:00',
      endTime: '11:00'
    });

    component.saveEntry();

    expect(component.conflictMessage()).toBe(
      'This time entry overlaps with an existing entry.'
    );
  });

  // Filtering
  it('should filter entries by status and project', () => {
    component.selectStatus('SUBMITTED');
    component.selectedProjectId.set(projectTwoId);

    expect(component.filteredEntries()).toHaveLength(1);
    expect(component.filteredEntries()[0].status).toBe('SUBMITTED');
    expect(component.filteredEntries()[0].projectId).toBe(projectTwoId);
  });

  // Edit entry
  it('should edit an existing entry', () => {
    const entry = component.entries()[0];

    component.editEntry(entry);

    component.entryForm.patchValue({
      startTime: '07:00',
      endTime: '08:00',
      description: 'Updated entry description.'
    });

    component.saveEntry();

    const updatedEntry = component
      .entries()
      .find((existingEntry) => existingEntry.id === entry.id);

    expect(updatedEntry?.durationMinutes).toBe(60);
    expect(updatedEntry?.description).toBe(
      'Updated entry description.'
    );

    expect(component.toastMessage()).toBe(
      'Time entry updated.'
    );
  });

  // Entry status workflow
  it('should submit, resubmit, and delete entries', () => {
    const draftEntry = component
      .entries()
      .find((entry) => entry.status === 'DRAFT');

    const rejectedEntry = component
      .entries()
      .find((entry) => entry.status === 'REJECTED');

    expect(draftEntry).toBeTruthy();
    expect(rejectedEntry).toBeTruthy();

if (draftEntry) {
  component.submitEntry(draftEntry);
}    expect(
      component.entries().find(
        (entry) => entry.id === draftEntry!.id
      )?.status
    ).toBe('SUBMITTED');

    if (rejectedEntry) {
  component.resubmitEntry(rejectedEntry);
}
    expect(
      component.entries().find(
        (entry) => entry.id === rejectedEntry!.id
      )?.status
    ).toBe('SUBMITTED');

if (draftEntry) {
  component.deleteEntry(draftEntry);
}
    expect(
      component.entries().some(
        (entry) => entry.id === draftEntry!.id
      )
    ).toBe(false);
  });

  // Timer functionality
  it('should start and stop a timer entry', () => {
    jest.useFakeTimers();

    const initialCount = component.entries().length;

    component.timerForm.patchValue({
      projectId: projectTwoId,
      taskId: taskThreeId,
      description: 'Timer tracked work.'
    });

    component.startTimer();

    jest.advanceTimersByTime(61_000);

    expect(component.activeTimer()).toBeTruthy();
    expect(component.elapsedSeconds()).toBe(61);

    component.stopTimer();

    expect(component.entries().length).toBe(initialCount + 1);
    expect(component.entries()[0].projectId).toBe(projectTwoId);
    expect(component.entries()[0].description).toBe(
      'Timer tracked work.'
    );

    expect(component.activeTimer()).toBeNull();

    expect(component.toastMessage()).toBe(
      'Timer entry saved.'
    );
  });

  // Pause/resume timer
  it('should pause and resume the timer', () => {
    jest.useFakeTimers();

    component.timerForm.patchValue({
      projectId: projectOneId,
      taskId: taskOneId,
      description: 'Paused timer test'
    });

    component.startTimer();

    jest.advanceTimersByTime(10_000);

    component.pauseTimer();

    const pausedValue = component.elapsedSeconds();

    expect(component.isTimerPaused()).toBe(true);
    expect(pausedValue).toBe(10);

    jest.advanceTimersByTime(5_000);

    expect(component.elapsedSeconds()).toBe(10);

    component.resumeTimer();

    jest.advanceTimersByTime(5_000);

    expect(component.isTimerPaused()).toBe(false);
    expect(component.elapsedSeconds()).toBe(15);
  });

  // Stop timer safely when no timer exists
  it('should safely return when stopping a timer that does not exist', () => {
    expect(component.activeTimer()).toBeNull();

    component.stopTimer();

    expect(component.activeTimer()).toBeNull();
  });

  // Resume timer safely when no timer exists
  it('should safely return when resuming a timer that does not exist', () => {
    expect(component.activeTimer()).toBeNull();

    component.resumeTimer();

    expect(component.activeTimer()).toBeNull();
  });

  // Timer task validation
  it('should require a task title when starting a timer with a new task', () => {
    component.newTaskFormContext.set('timer');
    component.newTaskTitle.set('   ');

    component.startTimer();

    expect(component.newTaskTitleError()).toBe(true);
    expect(component.activeTimer()).toBeNull();
  });

  // Menu toggling
  it('should toggle the entry menu open and closed', () => {
    const entryId = component.entries()[0].id;

    component.toggleEntryMenu(entryId);

    expect(component.openMenuEntryId()).toBe(entryId);

    component.toggleEntryMenu(entryId);

    expect(component.openMenuEntryId()).toBeNull();
  });

  // Toast dismissal
  it('should dismiss the toast message', () => {
    component.toastMessage.set('Test toast');

    component.dismissToast();

    expect(component.toastMessage()).toBe('');
  });

  // Conflict dismissal
  it('should dismiss the conflict message', () => {
    component.conflictMessage.set('Test conflict');

    component.dismissConflict();

    expect(component.conflictMessage()).toBe('');
  });

  // Preserve manual task creation state on project change
  it('should preserve manual new task state when changing projects', () => {
    component.newTaskFormContext.set('manual');
    component.newTaskTitle.set('New task');

    component.entryForm.controls.taskId.setValue('');

    component.entryForm.controls.projectId.setValue(projectTwoId);

    expect(component.newTaskFormContext()).toBe('manual');
    expect(component.newTaskTitle()).toBe('New task');
  });

  // Reset manual task selection when not creating task
  it('should reset manual task selection when project changes and user is not creating a task', () => {
    component.entryForm.controls.taskId.setValue(taskOneId);

    component.entryForm.controls.projectId.setValue(projectTwoId);

    expect(component.entryForm.controls.taskId.value).toBe('');
  });

  // Selectable task filtering
  it('should compute selectable tasks correctly', () => {
    component.entryForm.controls.projectId.setValue(projectOneId);

    const tasks = component.selectableTasks();

    expect(tasks.length).toBeGreaterThan(0);

    expect(
      tasks.every((task) => task.projectId === projectOneId)
    ).toBe(true);
  });

  // Timer selectable tasks filtering
  it('should compute selectable timer tasks correctly', () => {
    component.timerForm.controls.projectId.setValue(projectTwoId);

    const tasks = component.selectableTimerTasks();

    expect(tasks.length).toBeGreaterThan(0);

    expect(
      tasks.every((task) => task.projectId === projectTwoId)
    ).toBe(true);
  });

  // Task selection event
  it('should enable new task creation when create new option is selected', () => {
    const event = {
      target: {
        value: component.createNewTaskValue
      }
    } as unknown as Event;

    component.onTaskSelectionChange(event, 'manual');

    expect(component.newTaskFormContext()).toBe('manual');
    expect(component.newTaskTitle()).toBe('');
  });

  // Reset new task state on normal selection
  it('should reset new task state when selecting an existing task', () => {
    component.newTaskFormContext.set('manual');
    component.newTaskTitle.set('Temporary task');

    const event = {
      target: {
        value: taskOneId
      }
    } as unknown as Event;

    component.onTaskSelectionChange(event, 'manual');

    expect(component.newTaskFormContext()).toBeNull();
    expect(component.newTaskTitle()).toBe('');
  });

  // Duration formatting
  it('should format duration values correctly', () => {
    expect(component.formatDuration(30)).toBe('30m');
    expect(component.formatDuration(60)).toBe('1h');
    expect(component.formatDuration(75)).toBe('1h 15m');
    expect(component.formatDuration(null)).toBe('0m');
  });

  // Time formatting
  it('should format elapsed seconds correctly', () => {
    expect(component.formatElapsed(0)).toBe('00:00:00');
    expect(component.formatElapsed(59)).toBe('00:00:59');
    expect(component.formatElapsed(3661)).toBe('01:01:01');
  });

  // Project/task fallback labels
  it('should return fallback labels for unknown project and task ids', () => {
    expect(component.getProjectName('unknown')).toBe(
      'Unknown project'
    );

    expect(component.getTaskTitle('unknown')).toBe(
      'No task selected'
    );
  });

  // Timer started label
  it('should generate the timer started label', () => {
    jest.useFakeTimers();

    component.timerForm.patchValue({
      projectId: projectOneId,
      taskId: taskTwoId,
      description: 'Started label test'
    });

    component.startTimer();

    expect(component.timerStartedLabel()).toContain('Started at');
  });

  // Computed total minutes
  it('should compute total filtered minutes correctly', () => {
    component.selectStatus('DRAFT');

    const total = component
      .filteredEntries()
      .reduce(
        (sum, entry) => sum + entry.durationMinutes,
        0
      );

    expect(component.totalMinutes()).toBe(total);
  });

  // Backend payload generation
  it('should build a backend-compatible create/update payload without response-only fields', () => {
    component.filterForm.controls.from.setValue('2026-05-21');

    component.entryForm.patchValue({
      projectId: projectOneId,
      taskId: '',
      startTime: '09:30',
      endTime: '10:45',
      durationMinutes: 75,
      entryType: 'DEVELOPMENT',
      description: 'Backend payload check.'
    });

    const request =
      component.buildTimeEntryRequestFromForm();

    expect(request).toEqual({
      projectId: projectOneId,
      taskId: null,
      startTime: '2026-05-21T09:30:00',
      endTime: '2026-05-21T10:45:00',
      durationMinutes: 75,
      entryType: 'DEVELOPMENT',
      description: 'Backend payload check.'
    });

    expect(request).not.toHaveProperty('id');
    expect(request).not.toHaveProperty('status');
    expect(request).not.toHaveProperty('workspaceMemberId');
    expect(request).not.toHaveProperty('createdAt');
    expect(request).not.toHaveProperty('updatedAt');
    expect(request).not.toHaveProperty('rejectionReason');
  });

  // Cleanup lifecycle
  it('should clear timers on destroy', () => {
    jest.useFakeTimers();

    component.timerForm.patchValue({
      projectId: projectOneId,
      taskId: taskOneId,
      description: 'Destroy test'
    });

    component.startTimer();

    expect(component.activeTimer()).toBeTruthy();

    component.ngOnDestroy();

    jest.advanceTimersByTime(5_000);

    // elapsed seconds should not continue increasing
    expect(component.elapsedSeconds()).toBeLessThanOrEqual(1);
  });
});