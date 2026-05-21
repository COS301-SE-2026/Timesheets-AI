import { ComponentFixture, TestBed } from '@angular/core/testing';
import { LogtimeComponent } from './logtime.component';

// Unit test for log time page 

describe('LogtimeComponent', () => {
  let component: LogtimeComponent;
  let fixture: ComponentFixture<LogtimeComponent>;

  // Mock Guid/UUID constants mimicking backend entities for reliable data matching
  const projectOneId = '00000000-0000-0000-0001-000000000001';
  const projectTwoId = '00000000-0000-0000-0001-000000000002';
  const projectThreeId = '00000000-0000-0000-0001-000000000003';
  const taskOneId = '00000000-0000-0000-0002-000000000001';
  const taskThreeId = '00000000-0000-0000-0002-000000000003';
  const taskFourId = '00000000-0000-0000-0002-000000000004';

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      // LogtimeComponent is treated as a standalone component
      imports: [LogtimeComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(LogtimeComponent);
    component = fixture.componentInstance;
    
    // Triggers lifecycle hooks (ngOnInit) to populate default component states
    fixture.detectChanges();
  });

  // Basic sanity check to ensure the component initializes properly
  it('should create', () => {
    expect(component).toBeTruthy();
  });

  // UI State and Form Defaults Initialization
  it('should initialise the view options, status options, and forms used by the template', () => {
    // Assert: Check dropdown/filter menu option lists
    expect(component.viewOptions).toEqual(['Day', 'Week', 'Month']);
    expect(component.statusOptions).toEqual(['All', 'DRAFT', 'SUBMITTED', 'APPROVED', 'REJECTED']);
    
    // Assert: Date filters should have fallback initialization dates
    expect(component.filterForm.controls.from.value).toBeTruthy();
    expect(component.filterForm.controls.to.value).toBeTruthy();
    
    // Assert: Input forms should pre-select the default project ID
    expect(component.entryForm.controls.projectId.value).toBe(projectOneId);
    expect(component.timerForm.controls.projectId.value).toBe(projectOneId);
  });

  // Panel Toggling Logic
  it('should open and close the manual entry panel', () => {
    // Act: Expand the entry panel
    component.openManualPanel();

    // Assert: Check state changes to ensure it's in a pristine 'create' state
    expect(component.activePanel()).toBe('manual');
    expect(component.isEditMode()).toBe(false);

    // Act: Close the entry panel
    component.closePanel();

    // Assert: State should reset
    expect(component.activePanel()).toBeNull();
  });

  // Real time Duration Calculations
  it('should calculate duration when manual entry times change', () => {
    component.openManualPanel();
    
    // Act: Input a 2 hour and 30 minute time frame
    component.entryForm.patchValue({
      startTime: '08:15',
      endTime: '10:45'
    });

    // Assert: Form recalculates total minutes and formats the readable UI string
    expect(component.entryForm.controls.durationMinutes.value).toBe(150);
    expect(component.formatDuration(component.entryForm.controls.durationMinutes.value)).toBe('2h 30m');
  });

  // Validation: Logical Timeline Bounds
  it('should show validation when end time is before start time', () => {
    component.openManualPanel();
    
    // Act: Set chronological impossible times (End before Start)
    component.entryForm.patchValue({
      startTime: '11:00',
      endTime: '10:00'
    });

    // Assert: Component flags the field validity immediately
    expect(component.endTimeInvalid).toBe(true);

    // Act: Attempt to persist invalid entry
    component.saveEntry();

    // Assert: UI blocks saving and surfaces an error banner message
    expect(component.conflictMessage()).toBe('End time must be after start time.');
  });

  // Manual Entry Creation
  it('should save a new manual entry', () => {
    const initialCount = component.entries().length;

    component.openManualPanel();
    component.entryForm.patchValue({
      projectId: projectThreeId,
      taskId: taskFourId,
      entryType: 'DESIGN',
      startTime: '16:00',
      endTime: '17:00',
      description: 'Designed the log time page.'
    });
    
    // Act: Persist new entry
    component.saveEntry();

    // Assert: Validate tracking array grows, matches input payload, and resets UI state
    expect(component.entries().length).toBe(initialCount + 1);
    expect(component.entries()[0].projectId).toBe(projectThreeId);
    expect(component.entries()[0].startTime).toMatch(/^\d{4}-\d{2}-\d{2}T16:00:00$/); // Checks full ISO format string
    expect(component.entries()[0].durationMinutes).toBe(60);
    expect(component.toastMessage()).toBe('Time entry saved.');
    expect(component.activePanel()).toBeNull();
  });

  it('should create a new task when saving a manual entry with a custom task name', () => {
    const initialTaskCount = component.tasks().length;

    component.openManualPanel();
    component.newTaskFormContext.set('manual');
    component.newTaskTitle.set('API integration spike');
    component.entryForm.patchValue({
      projectId: projectOneId,
      startTime: '15:00',
      endTime: '16:00',
      description: 'Spike work on API integration.'
    });

    component.saveEntry();

    const createdTask = component.tasks().find((task) => task.title === 'API integration spike');
    expect(component.tasks().length).toBe(initialTaskCount + 1);
    expect(createdTask?.projectId).toBe(projectOneId);
    expect(component.entries()[0].taskId).toBe(createdTask?.id);
    expect(component.newTaskFormContext()).toBeNull();
  });

  it('should require a task name when creating a new task', () => {
    component.openManualPanel();
    component.newTaskFormContext.set('manual');
    component.newTaskTitle.set('   ');

    component.saveEntry();

    expect(component.newTaskTitleError()).toBe(true);
    expect(component.activePanel()).toBe('manual');
  });

  // Validation: Double Booking Prevention
  it('should prevent overlapping manual entries', () => {
    component.openManualPanel();
    
    // Act: Attempt to save an entry during a time window that is already occupied
    component.entryForm.patchValue({
      projectId: projectOneId,
      taskId: taskOneId,
      startTime: '10:00',
      endTime: '11:00'
    });
    component.saveEntry();

    // Assert: Conflict detection throws validation error banner
    expect(component.conflictMessage()).toBe('This time entry overlaps with an existing entry.');
  });

  // Dynamic Filtering Logic
  it('should filter entries by status and project', () => {
    // Act: Apply active status and project filter states
    component.selectStatus('SUBMITTED');
    component.selectedProjectId.set(projectTwoId);

    // Assert: The computed/filtered tracking array updates its subset reactively
    expect(component.filteredEntries()).toHaveLength(1);
    expect(component.filteredEntries()[0].status).toBe('SUBMITTED');
    expect(component.filteredEntries()[0].projectId).toBe(projectTwoId);
  });

  // Entry Updating/Editing
  it('should edit an existing entry', () => {
    // Arrange: Extract a reference target entry
    const entry = component.entries()[0];

    // Act: Switch form context to edit, alter details, and save
    component.editEntry(entry);
    component.entryForm.patchValue({
      startTime: '07:00',
      endTime: '08:00',
      description: 'Updated entry description.'
    });
    component.saveEntry();

    // Assert: Locate updated element in store and check payload adjustments
    const updatedEntry = component.entries().find((existingEntry) => existingEntry.id === entry.id);

    expect(updatedEntry?.startTime).toMatch(/^\d{4}-\d{2}-\d{2}T07:00:00$/);
    expect(updatedEntry?.durationMinutes).toBe(60);
    expect(updatedEntry?.description).toBe('Updated entry description.');
    expect(component.toastMessage()).toBe('Time entry updated.');
  });

  // State Workflows: Submission and Removal
  it('should submit, resubmit, and delete entries', () => {
    // Arrange: Find distinct records based on starting statuses
    const draftEntry = component.entries().find((entry) => entry.status === 'DRAFT');
    const rejectedEntry = component.entries().find((entry) => entry.status === 'REJECTED');

    expect(draftEntry).toBeTruthy();
    expect(rejectedEntry).toBeTruthy();

    // Act and Assert 1: Submit a regular fresh draft entry
    component.submitEntry(draftEntry!);
    expect(component.entries().find((entry) => entry.id === draftEntry!.id)?.status).toBe('SUBMITTED');

    // Act and Assert 2: Resubmit a previously rejected entry
    component.resubmitEntry(rejectedEntry!);
    expect(component.entries().find((entry) => entry.id === rejectedEntry!.id)?.status).toBe('SUBMITTED');

    // Act and Assert 3: Purge an entry completely from the collection array
    component.deleteEntry(draftEntry!);
    expect(component.entries().some((entry) => entry.id === draftEntry!.id)).toBe(false);
  });

  // Async Timer Operations (Using Jest Fake Timers)
  it('should start and stop a timer entry', () => {
    jest.useFakeTimers();
    const initialCount = component.entries().length;

    // Arrange: Set target tracking context
    component.timerForm.patchValue({
      projectId: projectTwoId,
      taskId: taskThreeId,
      description: 'Timer tracked work.'
    });
    
    // Act: Fire up the timer
    component.startTimer();

    // Act: Fast-forward virtual clock by 61 seconds 
    jest.advanceTimersByTime(61_000);

    // Assert: Verify state during active run
    expect(component.activeTimer()).toBeTruthy();
    expect(component.elapsedSeconds()).toBe(61);

    // Act: Stop timer to commit the runtime log
    component.stopTimer();

    // Assert: Verify clock record is converted cleanly into a permanent time entry
    expect(component.entries().length).toBe(initialCount + 1);
    expect(component.entries()[0].projectId).toBe(projectTwoId);
    expect(component.entries()[0].startTime).toMatch(/^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:00$/);
    expect(component.entries()[0].description).toBe('Timer tracked work.');
    expect(component.activeTimer()).toBeNull();
    expect(component.toastMessage()).toBe('Timer entry saved.');

    // Cleanup: Restore standard global timers to avoid bleed through across test files
    jest.useRealTimers();
  });

  // Template Render Data-Mapping Helpers
  it('should format helper values for the template', () => {
    const entry = component.entries()[0];

    // Assert: String display format transformers (seconds - hh:mm:ss)
    expect(component.formatElapsed(3661)).toBe('01:01:01');
    expect(component.formatEntryTimeRange(entry)).toBe('09:00 - 11:30');
    
    // Assert: Mock metadata lookup translation maps (ID - Display Text strings)
    expect(component.getProjectName(entry.projectId)).toBe('Momently Platform');
    expect(component.getTaskTitle(entry.taskId)).toBe('Timesheet dashboard');
    
    // Assert: Dynamic dynamic CSS class generation based on entry lifecycle state
    expect(component.entryDotClass(entry)).toBe(`status-dot status-dot--${entry.status.toLowerCase()}`);
    expect(component.headerDateLabel()).toBeTruthy();
  });

  // Data Integrity: API Contract Sanitation
  it('should build a backend-compatible create/update payload without response-only fields', () => {
    // Arrange: Mock active client context and form selections
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

    // Act: Parse form values into a structural POST payload model
    const request = component.buildTimeEntryRequestFromForm();

    // Assert: The outbound object maps correctly to the backend schema parameters
    expect(request).toEqual({
      projectId: projectOneId,
      taskId: null, // Converts empty strings safely to null
      startTime: '2026-05-21T09:30:00',
      endTime: '2026-05-21T10:45:00',
      durationMinutes: 75,
      entryType: 'DEVELOPMENT',
      description: 'Backend payload check.'
    });
    
    // Assert: Strips system-managed/read-only database fields to prevent bad request parameters (400)
    expect(request).not.toHaveProperty('id');
    expect(request).not.toHaveProperty('status');
    expect(request).not.toHaveProperty('workspaceMemberId');
    expect(request).not.toHaveProperty('createdAt');
    expect(request).not.toHaveProperty('updatedAt');
    expect(request).not.toHaveProperty('rejectionReason');
  });
});