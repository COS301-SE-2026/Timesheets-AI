/*
Rewrote this to match the current component file, which always talks with HTTP endpoints
so HttpTestingController lets us to intercept the calls instead of hitting a actual backend
reference: https://angular.dev/guide/http/testing

Author: Zamokuhle Zwane
Date: 25/07/2026
*/

import { ComponentFixture, TestBed } from '@angular/core/testing';
import { LogtimeComponent } from './logtime.component';
import {
  HttpTestingController,
  provideHttpClientTesting,
} from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';

// Unit tests for log time page

describe('LogtimeComponent', () => {
  let component: LogtimeComponent;
  let fixture: ComponentFixture<LogtimeComponent>;
  let httpMock: HttpTestingController;

  //Real ids being from the seeded data, keeping it local to this spec so tests dont depend on the actual db being seeded

  const projectOneId = '00000000-0000-0000-0001-000000000040';
  const projectTwoId = '00000000-0000-0000-0001-000000000041';

  const taskOneId = '00000000-0000-0000-0002-000000000070';
  const taskTwoId = '00000000-0000-0000-0002-000000000071';

  const mockProjects = [
    { id: projectOneId, name: 'Mobile App Development' },
    { id: projectTwoId, name: 'Backend API' },
  ];

  const mockTasks = [
    { id: taskOneId, projectId: projectOneId, title: 'Implement Login Screen' },
    { id: taskTwoId, projectId: projectTwoId, title: 'Create Timesheet API' },
  ];

  /*
  duration is in seconds not minutes
  */
  const mockEntries = [
    {
      id: 'entry-1',
      timesheetId: 'timesheet-1',
      workspaceMemberId: 'member-1',
      projectId: projectOneId,
      taskId: taskOneId,
      entryType: 'MANUAL' as const,
      startTime: `${today()}T09:00:00`,
      endTime: `${today()}T11:00:00`,
      durationMinutes: 7200, // 2 hours, in seconds
      description: 'Existing entry for overlap testing.',
      status: undefined,
      isDeleted: false,
    },
  ];

  const mockTimesheet = {
    id: 'timesheet-1',
    periodStart: today(),
    periodEnd: today(),
    status: 'DRAFT' as const,
    isLocked: false,
  };

  function today(): string {
    return new Date().toISOString().slice(0, 10);
  }

  //flushes the 4 requests that the comstructors fires on the initial load. Called once per test in beforeEach so every test starts from the
  //same known state
  function flushInitialLoads(): void {
    httpMock.expectOne('/api/timesheets/me').flush([mockTimesheet]);
    httpMock.expectOne('/api/time-entries/me').flush(mockEntries);
    httpMock.expectOne('/api/projects').flush(mockProjects);
   
    //no active timer running at start of normal test, 204 No Content
    httpMock.expectOne('/api/timers/active').flush(null, {status: 204, statusText: 'No Content'})

    httpMock.expectOne(`/api/tasks/project/${projectOneId}`).flush(
      mockTasks.filter((t)=> t.projectId === projectOneId),);
  }

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [LogtimeComponent],
      providers: [provideHttpClient(), provideHttpClientTesting()],
    }).compileComponents();

    fixture = TestBed.createComponent(LogtimeComponent);
    component = fixture.componentInstance;
    httpMock = TestBed.inject(HttpTestingController);

    fixture.detectChanges(); //this will trigger the constructor, which fires the 4 loads
    flushInitialLoads();
    fixture.detectChanges();
  });

  afterEach(() => {
    httpMock.verify(); //fails if any request was made but not asserted
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
      'REJECTED',
    ]);

    expect(component.filterForm.controls.from.value).toBeTruthy();
    expect(component.filterForm.controls.to.value).toBeTruthy();

    //since there are no mock values, we are confirming data loaded actually populated the signals and
    //defaulted the forms
    expect(component.projects()).toEqual(mockProjects);
    expect(component.entryForm.controls.projectId.value).toBe(projectOneId);
    expect(component.timerForm.controls.projectId.value).toBe(projectOneId);

    //tasks signal keeps the "No task selected" placeholder at index 0
    expect(component.tasks()[0].title).toBe('No task selected');
    const projectOneTasks = mockTasks.filter((t) => t.projectId === projectOneId);
    expect(component.tasks()).toHaveLength(projectOneTasks.length + 1);
  });

  it('should load the current timesheet and expose its status', () => {
    expect(component.currentTimesheet()).toEqual(mockTimesheet);
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
      endTime: '10:45',
    });

    expect(component.entryForm.controls.durationMinutes.value).toBe(150);
    expect(
      component.formatDuration(
        component.entryForm.controls.durationMinutes.value * 60,
      ),
    ).toBe('2h 30m');
  });

  // Validation checks
  it('should show validation when end time is before start time', () => {
    component.openManualPanel();

    component.entryForm.patchValue({
      startTime: '11:00',
      endTime: '10:00',
    });

    expect(component.endTimeInvalid).toBe(true);

    component.saveEntry();

    expect(component.conflictMessage()).toBe(
      'End time must be after start time.',
    );
    //saveEntry() should return before making any HTTP calls when validation fails
  });

  // Save manual entry
  //entryType must be uppercase per CHECK constraint and use seconds not minutes
  it('should save a new manual entry', () => {
    component.openManualPanel();

    component.entryForm.patchValue({
      projectId: projectTwoId,
      taskId: taskTwoId,
      entryType: 'MANUAL',
      startTime: '22:00',
      endTime: '23:00',
      description: 'Designed the log time page.',
    });

    httpMock.expectOne(`/api/tasks/project/${projectTwoId}`).flush(
      mockTasks.filter((t) => t.projectId === projectTwoId),
    );

    component.saveEntry();

    const req = httpMock.expectOne('/api/time-entries');
    expect(req.request.method).toBe('POST');
    expect(req.request.body.entryType).toBe('MANUAL');
    expect(req.request.body.durationSeconds).toBe(3600); //this is 1 hour, in seconds
    expect(req.request.body).not.toHaveProperty('durationMinutes');

    //simulate the backend response
    req.flush({
      id: 'entry-new',
      timesheetId: 'timesheet-1',
      workspaceMemberId: 'member-1',
      projectId: projectTwoId,
      taskId: taskTwoId,
      entryType: 'MANUAL',
      startTime: `${today()}T22:00:00`,
      endTime: `${today()}T23:00:00`,
      durationMinutes: 3600,
      description: 'Designed the log time page.',
      isDeleted: false,
    });

    expect(component.conflictMessage()).toBe('');
    expect(component.entries()[0].projectId).toBe(projectTwoId);
    expect(component.entries()[0].durationMinutes).toBe(60); // corrected from seconds
    expect(component.toastMessage()).toBe('Time entry saved.');
    expect(component.activePanel()).toBeNull();
  });


  // Overlap validation
  it('should prevent overlapping manual entries', () => {
    component.openManualPanel();

    component.entryForm.patchValue({
      projectId: projectOneId,
      taskId: taskOneId,
      startTime: '10:00',
      endTime: '11:00',
    });

    component.saveEntry();

    expect(component.conflictMessage()).toBe(
      'This time entry overlaps with an existing entry.',
    );
  });

  // Filtering, status now derives from currentTimesheet(), not the entry according to Swagger
  it('should filter entries by status and project', () => {
    component.selectStatus('DRAFT'); //matches mockTimesheet.status
    component.selectedProjectId.set(projectOneId);

    expect(component.filteredEntries()).toHaveLength(1);
    expect(component.filteredEntries()[0].projectId).toBe(projectOneId);

    component.selectStatus('APPROVED'); //Doesnt match the mocktimesheet.status
    expect(component.filteredEntries()).toHaveLength(0);
  });

  // Edit entry
  it('should edit an existing entry', () => {
    const entry = component.entries()[0];

    component.editEntry(entry);

    component.entryForm.patchValue({
      startTime: '07:00',
      endTime: '08:00',
      description: 'Updated entry description.',
    });

    component.saveEntry();

    const req = httpMock.expectOne(`/api/time-entries/${entry.id}`);
    expect(req.request.method).toBe('PUT');
    expect(req.request.body.durationSeconds).toBe(3600);

    req.flush({
      ...entry,
      startTime: `${today()}T07:00:00`,
      endTime: `${today()}T08:00:00`,
      durationMinutes: 3600,
      description: 'Updated entry description.',
    });

    const updatedEntry = component
      .entries()
      .find((existingEntry) => existingEntry.id === entry.id);

    expect(updatedEntry?.durationMinutes).toBe(60);
    expect(updatedEntry?.description).toBe('Updated entry description.');
    expect(component.toastMessage()).toBe('Time entry updated.');
  });

  // Delete entry
  it('should delete an entry via the backend', () => {
    const entry = component.entries()[0];

    component.deleteEntry(entry);

    const req = httpMock.expectOne(`/api/time-entries/${entry.id}`);
    expect(req.request.method).toBe('DELETE');
    req.flush(null);

    expect(component.entries().some((e) => e.id === entry.id)).toBe(false);
    expect(component.toastMessage()).toBe('Time entry deleted.');
  });

  //Timesheet submit
  it('should submit the current timesheet', () => {
    component.submitTimesheet();

    const req = httpMock.expectOne(
      `/api/timesheets/${mockTimesheet.id}/submit`,
    );
    expect(req.request.method).toBe('POST');

    req.flush({ ...mockTimesheet, status: 'SUBMITTED' });

    expect(component.currentTimesheet()?.status).toBe('SUBMITTED');
    expect(component.toastMessage()).toBe('Timesheet submitted.');
  });

  it('should refuse to submit a locked timesheet', () => {
    component.currentTimesheet.set({ ...mockTimesheet, isLocked: true });

    component.submitTimesheet();

    expect(component.conflictMessage()).toBe(
      'This timesheet is locked and cannot be submitted again.',
    );
    // no HTTP call should fire
  });

  //Timer functionality
  it('should start and stop a timer entry', () => {
     jest.useFakeTimers();
     jest.setSystemTime(new Date(`${today()}T03:00:00`)); // fixed, off-hours — avoids the 09:00-11:00 mock entry

    const initialCount = component.entries().length;

    component.timerForm.patchValue({
      projectId: projectTwoId,
      taskId: taskTwoId,
      description: 'Timer tracked work.',
    });

    httpMock.expectOne(`/api/tasks/project/${projectTwoId}`).flush(
      mockTasks.filter((t) => t.projectId === projectTwoId),
    );

    component.startTimer();

    const startReq = httpMock.expectOne('/api/timers/start');
    expect(startReq.request.body).toEqual({
      projectId: projectTwoId,
      taskId: taskTwoId,
    });
    startReq.flush({});

    jest.advanceTimersByTime(61_000);

    expect(component.activeTimer()).toBeTruthy();
    expect(component.elapsedSeconds()).toBe(61);

    
    component.stopTimer();

    //stopTimer() refreshes entries first to check for conflicts
    const refreshReq = httpMock.expectOne('/api/time-entries/me');
    refreshReq.flush(mockEntries);

    //then calls /timers/stop...
    const stopReq = httpMock.expectOne('/api/timers/stop');
    expect(stopReq.request.method).toBe('POST');
    stopReq.flush({
      timerId: 'timer-1',
      stoppedAt: `${today()}T13:00:00`,
      durationMinutes: 3600, //1 hour, in seconds
      createdTimeEntry: {
        id: 'entry-timer',
        project: {
          id: projectTwoId,
          name: 'Backend API',
        },
        task: {
          id: taskTwoId,
          title: 'Create Timesheet API',
        },
        date: today(),
        startTime: `${today()}T12:00:00`,
        endTime: `${today()}T13:00:00`,
        durationMinutes: 3600,
        status: 'DRAFT',
      },
    });

  const attachNotesReq = httpMock.expectOne('/api/time-entries/entry-timer');
  expect(attachNotesReq.request.method).toBe('PUT');
  expect(attachNotesReq.request.body.description).toBe('Timer tracked work.');
  attachNotesReq.flush({
    ...mockEntries[0],
    id: 'entry-timer',
    description: 'Timer tracked work.',
  });

    //then reloads entries again via loadEntries()
    const reloadReq = httpMock.expectOne('/api/time-entries/me');
    reloadReq.flush([
      ...mockEntries,
      {
        id: 'entry-timer',
        projectId: projectTwoId,
        taskId: taskTwoId,
        entryType: 'TIMER',
        startTime: `${today()}T12:00:00`,
        endTime: `${today()}T13:00:00`,
        durationMinutes: 3600,
        description: 'Timer tracked work.',
        isDeleted: false,
      },
    ]);

    httpMock.expectOne(`/api/tasks/project/${projectOneId}`).flush(
      mockTasks.filter((t) => t.projectId === projectOneId),
    );

    expect(component.entries().length).toBe(initialCount + 1);
    expect(component.activeTimer()).toBeNull();
    expect(component.toastMessage()).toBe('Timer entry saved.');
  });

  //Pause/resume timer, purely local state, no HTTP calls involved
  it('should pause and resume the timer', () => {
    jest.useFakeTimers();

    component.timerForm.patchValue({
      projectId: projectOneId,
      taskId: taskOneId,
      description: 'Paused timer test',
    });

    component.startTimer();
    httpMock.expectOne('/api/timers/start').flush({});

    jest.advanceTimersByTime(10_000);
    component.pauseTimer();

    expect(component.isTimerPaused()).toBe(true);
    expect(component.elapsedSeconds()).toBe(10);

    jest.advanceTimersByTime(5_000);
    expect(component.elapsedSeconds()).toBe(10); //frozen while paused

    component.resumeTimer();
    jest.advanceTimersByTime(5_000);

    expect(component.isTimerPaused()).toBe(false);
    expect(component.elapsedSeconds()).toBe(15);
  });

  //Stop timer safely when no timer exists
  it('should safely return when stopping a timer that does not exist', () => {
    expect(component.activeTimer()).toBeNull();

    component.stopTimer();

    expect(component.activeTimer()).toBeNull();
    //no HTTP call should fire, verified by afterEach
  });

  //resume timer safely when no timer exists
  it('should safely return when resuming a timer that does not exist', () => {
    expect(component.activeTimer()).toBeNull();

    component.resumeTimer();

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

  // Reset manual task selection when not creating task
  it('should reset manual task selection when project changes and user is not creating a task', () => {
    component.entryForm.controls.taskId.setValue(taskOneId);

    component.entryForm.controls.projectId.setValue(projectTwoId);

    httpMock.expectOne(`/api/tasks/project/${projectTwoId}`).flush(
      mockTasks.filter((t) => t.projectId === projectTwoId),
    );

    expect(component.entryForm.controls.taskId.value).toBe('');
  });

  // Selectable task filtering
  it('should compute selectable tasks correctly', () => {
    component.entryForm.controls.projectId.setValue(projectTwoId);

    httpMock.expectOne(`/api/tasks/project/${projectTwoId}`)
    .flush(mockTasks.filter((t) => t.projectId === projectTwoId));

    const tasks = component.selectableTasks();

    expect(tasks.length).toBeGreaterThan(0);

    expect(tasks.every((task) => task.projectId === projectTwoId)).toBe(true);
  });

  // Timer selectable tasks filtering
  it('should compute selectable timer tasks correctly', () => {
    component.timerForm.controls.projectId.setValue(projectTwoId);

    httpMock.expectOne(`/api/tasks/project/${projectTwoId}`)
    .flush(mockTasks.filter((t) => t.projectId === projectTwoId));

    const tasks = component.selectableTimerTasks();

    expect(tasks.length).toBeGreaterThan(0);

    expect(tasks.every((task) => task.projectId === projectTwoId)).toBe(true);
  });

  // Task selection event
  it('should enable new task creation when create new option is selected', () => {
    const event = {
      target: {
        value: component.createNewTaskValue,
      },
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
        value: taskOneId,
      },
    } as unknown as Event;

    component.onTaskSelectionChange(event, 'manual');

    expect(component.newTaskFormContext()).toBeNull();
    expect(component.newTaskTitle()).toBe('');
  });

  // Duration formatting
  it('should format duration values correctly', () => {
    expect(component.formatDuration(30*60)).toBe('30m');
    expect(component.formatDuration(60*60)).toBe('1h');
    expect(component.formatDuration(75*60)).toBe('1h 15m');
    expect(component.formatDuration(0)).toBe('0s');
    expect(component.formatDuration(45)).toBe('45s');
    expect(component.formatDuration(90)).toBe('1m 30s');
  });

  // Time formatting
  it('should format elapsed seconds correctly', () => {
    expect(component.formatElapsed(0)).toBe('00:00:00');
    expect(component.formatElapsed(59)).toBe('00:00:59');
    expect(component.formatElapsed(3661)).toBe('01:01:01');
  });

  // Project/task fallback labels
  it('should return fallback labels for unknown project and task ids', () => {
    expect(component.getProjectName('unknown')).toBe('Unknown project');

    expect(component.getTaskTitle('unknown')).toBe('No task selected');
  });

  // Timer started label
  it('should generate the timer started label', () => {
    jest.useFakeTimers();

    component.timerForm.patchValue({
      projectId: projectOneId,
      taskId: taskTwoId,
      description: 'Started label test',
    });

    component.startTimer();
    httpMock.expectOne('/api/timers/start').flush({});
    expect(component.timerStartedLabel()).toContain('Started at');
  });

  // Computed total minutes
  it('should compute total filtered minutes correctly', () => {
    component.selectStatus('DRAFT');

    const total = component
      .filteredEntries()
      .reduce((sum, entry) => sum + entry.durationMinutes, 0);

    expect(component.totalMinutes()).toBe(total);
  });

  // Backend payload generation
  it('should build a backend-compatible create/update payload without response-only fields', () => {
    component.filterForm.controls.from.setValue('2026-05-21');
    httpMock.expectOne('/api/timesheets/me').flush([mockTimesheet]);
    
    component.entryForm.patchValue({
      projectId: projectOneId,
      taskId: '',
      startTime: '09:30',
      endTime: '10:45',
      durationMinutes: 75,
      entryType: 'MANUAL',
      description: 'Backend payload check.',
    });

    const request = component.buildTimeEntryRequestFromForm();

    expect(request).toEqual({
      projectId: projectOneId,
      taskId: null,
      startTime: '2026-05-21T09:30:00',
      endTime: '2026-05-21T10:45:00',
      durationSeconds: 4500, //75mins X 60
      entryType: 'MANUAL',
      description: 'Backend payload check.',
    });

    expect(request).not.toHaveProperty('id');
    expect(request).not.toHaveProperty('status');
    expect(request).not.toHaveProperty('durationMinutes');
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
      description: 'Destroy test',
    });

    component.startTimer();
    httpMock.expectOne('/api/timers/start').flush({});

    expect(component.activeTimer()).toBeTruthy();

    component.ngOnDestroy();

    jest.advanceTimersByTime(5_000);

    // elapsed seconds should not continue increasing
    expect(component.elapsedSeconds()).toBeLessThanOrEqual(1);
  });
});
