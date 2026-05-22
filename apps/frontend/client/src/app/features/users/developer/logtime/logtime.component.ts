import { Component, computed, OnDestroy, signal } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule } from '@angular/forms';

// Type definitions and interface

type ViewOption = 'Day' | 'Week' | 'Month';
type StatusOption = 'All' | TimeEntryStatus;
type PanelType = 'manual' | 'timer' | null;
type TimeEntryStatus = 'DRAFT' | 'SUBMITTED' | 'APPROVED' | 'REJECTED';
type EntryType = 'DEVELOPMENT' | 'MEETING' | 'DOCUMENTATION' | 'DESIGN' | 'BREAK';

interface Project {
  id: string;
  name: string;
}

interface Task {
  id: string;
  projectId: string;
  title: string;
}

interface TimeEntry {
  id: string;
  workspaceMemberId?: string;
  projectId: string;
  taskId: string;
  entryType: EntryType;
  startTime: string; // ISO String (YYYY-MM-DDTHH:mm:ss)
  endTime: string;   // ISO String (YYYY-MM-DDTHH:mm:ss)
  durationMinutes: number;
  description: string;
  status: TimeEntryStatus;
  isLocked?: boolean;
  createdAt?: string;
  updatedAt?: string;
  rejectionReason?: string;
}

/**
 * Interface representing the sanitized data structure
 * expected by external backend APIs.
 */
interface TimeEntryRequest {
  projectId: string;
  taskId: string | null;
  startTime: string;
  endTime: string;
  durationMinutes: number;
  entryType: EntryType;
  description: string;
}

interface ActiveTimer {
  projectId: string;
  taskId: string;
  description: string;
  startedAt: Date;
}

@Component({
  selector: 'app-logtime',
  imports: [ReactiveFormsModule],
  templateUrl: './logtime.component.html',
  styleUrl: './logtime.component.scss'
})
export class LogtimeComponent implements OnDestroy {
  // Static configuration options for template elements
  readonly viewOptions: ViewOption[] = ['Day', 'Week', 'Month'];
  readonly statusOptions: StatusOption[] = ['All', 'DRAFT', 'SUBMITTED', 'APPROVED', 'REJECTED'];
  readonly createNewTaskValue = '__create_new__';

  // Angular signals ( For state management )

  readonly selectedView = signal<ViewOption>('Day');
  readonly selectedStatus = signal<StatusOption>('All');
  readonly selectedProjectId = signal('');
  readonly activePanel = signal<PanelType>(null);
  readonly conflictMessage = signal('');
  readonly toastMessage = signal('');
  readonly activeTimer = signal<ActiveTimer | null>(null);
  readonly elapsedSeconds = signal(0);
  readonly openMenuEntryId = signal<string | null>(null);
  readonly isEditMode = signal(false);
  readonly newTaskFormContext = signal<'manual' | 'timer' | null>(null);
  readonly newTaskTitle = signal('');
  readonly newTaskTitleError = signal(false);
  readonly isTimerPaused = signal(false);
  readonly pausedElapsedSeconds = signal(0);

  // Mock data mimicking database tables
  readonly projects = signal<Project[]>([
    { id: '00000000-0000-0000-0001-000000000001', name: 'Momently Platform' },
    { id: '00000000-0000-0000-0001-000000000002', name: 'Client Portal' },
    { id: '00000000-0000-0000-0001-000000000003', name: 'Internal Tools' }
  ]);

  readonly tasks = signal<Task[]>([
    { id: '', projectId: '', title: 'No task selected' },
    { id: '00000000-0000-0000-0002-000000000001', projectId: '00000000-0000-0000-0001-000000000001', title: 'Timesheet dashboard' },
    { id: '00000000-0000-0000-0002-000000000002', projectId: '00000000-0000-0000-0001-000000000001', title: 'Authentication flow' },
    { id: '00000000-0000-0000-0002-000000000003', projectId: '00000000-0000-0000-0001-000000000002', title: 'Client reporting' },
    { id: '00000000-0000-0000-0002-000000000004', projectId: '00000000-0000-0000-0001-000000000003', title: 'Developer tooling' }
  ]);

  readonly entries = signal<TimeEntry[]>([
    {
      id: '00000000-0000-0000-0003-000000000001',
      workspaceMemberId: '00000000-0000-0000-0003-000000000002',
      projectId: '00000000-0000-0000-0001-000000000001',
      taskId: '00000000-0000-0000-0002-000000000001',
      entryType: 'DEVELOPMENT',
      startTime: `${this.today()}T09:00:00`,
      endTime: `${this.today()}T11:30:00`,
      durationMinutes: 150,
      description: 'Built the time entries table.',
      status: 'DRAFT',
      isLocked: false,
      createdAt: `${this.today()}T09:00:00`,
      updatedAt: `${this.today()}T09:00:00`
    },
    {
      id: '00000000-0000-0000-0003-000000000002',
      workspaceMemberId: '00000000-0000-0000-0003-000000000002',
      projectId: '00000000-0000-0000-0001-000000000002',
      taskId: '00000000-0000-0000-0002-000000000003',
      entryType: 'MEETING',
      startTime: `${this.today()}T12:00:00`,
      endTime: `${this.today()}T13:00:00`,
      durationMinutes: 60,
      description: 'Client requirements sync.',
      status: 'SUBMITTED',
      isLocked: false,
      createdAt: `${this.today()}T12:00:00`,
      updatedAt: `${this.today()}T12:00:00`
    },
    {
      id: '00000000-0000-0000-0003-000000000003',
      workspaceMemberId: '00000000-0000-0000-0003-000000000002',
      projectId: '00000000-0000-0000-0001-000000000003',
      taskId: '00000000-0000-0000-0002-000000000004',
      entryType: 'DOCUMENTATION',
      startTime: `${this.today()}T14:00:00`,
      endTime: `${this.today()}T15:15:00`,
      durationMinutes: 75,
      description: 'Updated setup notes.',
      status: 'REJECTED',
      isLocked: false,
      createdAt: `${this.today()}T14:00:00`,
      updatedAt: `${this.today()}T14:00:00`,
      rejectionReason: 'Please add more detail.'
    },
    {
      id: '00000000-0000-0000-0003-000000000004',
      workspaceMemberId: '00000000-0000-0000-0003-000000000002',
      projectId: '00000000-0000-0000-0001-000000000001',
      taskId: '00000000-0000-0000-0002-000000000002',
      entryType: 'DESIGN',
      startTime: `${this.today()}T15:30:00`,
      endTime: `${this.today()}T17:00:00`,
      durationMinutes: 90,
      description: 'Created wireframes for dashboard.',
      status: 'APPROVED',
      isLocked: false,
      createdAt: `${this.today()}T15:30:00`,
      updatedAt: `${this.today()}T15:30:00`
    },
    {
      id: '00000000-0000-0000-0003-000000000005',
      workspaceMemberId: '00000000-0000-0000-0003-000000000002',
      projectId: '00000000-0000-0000-0001-000000000002',
      taskId: '00000000-0000-0000-0002-000000000003',
      entryType: 'DEVELOPMENT',
      startTime: `${this.today()}T17:15:00`,
      endTime: `${this.today()}T18:45:00`,
      durationMinutes: 90,
      description: 'Implemented reporting module.',
      status: 'DRAFT',
      isLocked: false,
      createdAt: `${this.today()}T17:15:00`,
      updatedAt: `${this.today()}T17:15:00`
    },
    {
      id: '00000000-0000-0000-0003-000000000006',
      workspaceMemberId: '00000000-0000-0000-0003-000000000002',
      projectId: '00000000-0000-0000-0001-000000000001',
      taskId: '00000000-0000-0000-0002-000000000001',
      entryType: 'BREAK',
      startTime: `${this.today()}T19:00:00`,
      endTime: `${this.today()}T19:30:00`,
      durationMinutes: 30,
      description: 'Lunch break.',
      status: 'DRAFT',
      isLocked: false,
      createdAt: `${this.today()}T19:00:00`,
      updatedAt: `${this.today()}T19:00:00`
    }
  ]);

  //Reactive from groups

  // Scopes dashboard visibility metrics
  readonly filterForm = new FormGroup({
    from: new FormControl(this.today(), { nonNullable: true }),
    to: new FormControl(this.today(), { nonNullable: true })
  });

  // Controls the explicit manual logging sidebar template
  readonly entryForm = new FormGroup({
    id: new FormControl('', { nonNullable: true }),
    projectId: new FormControl('00000000-0000-0000-0001-000000000001', { nonNullable: true }),
    taskId: new FormControl('', { nonNullable: true }),
    entryType: new FormControl<EntryType>('DEVELOPMENT', { nonNullable: true }),
    startTime: new FormControl('09:00', { nonNullable: true }),
    endTime: new FormControl('10:00', { nonNullable: true }),
    durationMinutes: new FormControl(60, { nonNullable: true }),
    description: new FormControl('', { nonNullable: true })
  });

  // Handles real-time progressive timer context entries
  readonly timerForm = new FormGroup({
    projectId: new FormControl('00000000-0000-0000-0001-000000000001', { nonNullable: true }),
    taskId: new FormControl('', { nonNullable: true }),
    description: new FormControl('', { nonNullable: true })
  });

  //Computed signals (Reactive  derived state )

  // Cascades tasks depending on the active project selected inside the manual logging form
  readonly filteredTasks = computed(() => this.tasks().filter((task) => !task.id || task.projectId === this.entryForm.controls.projectId.value));

  readonly selectableTasks = computed(() => this.tasks().filter(
    (task) => task.id && task.projectId === this.entryForm.controls.projectId.value
  ));

  readonly selectableTimerTasks = computed(() => this.tasks().filter(
    (task) => task.id && task.projectId === this.timerForm.controls.projectId.value
  ));

  readonly isCreatingNewManualTask = computed(() => this.newTaskFormContext() === 'manual');
  readonly isCreatingNewTimerTask = computed(() => this.newTaskFormContext() === 'timer');

  // Cascades tasks depending on the project selected inside the automated timer form
  readonly timerTasks = computed(() => this.tasks().filter((task) => !task.id || task.projectId === this.timerForm.controls.projectId.value));

  // Multi variable matrix filter sorting out visible logs matching active global criteria
  readonly filteredEntries = computed(() => {
    const selectedProjectId = this.selectedProjectId();
    const selectedStatus = this.selectedStatus();
    const from = this.filterForm.controls.from.value;
    const to = this.filterForm.controls.to.value;

    return this.entries().filter((entry) => {
      const matchesProject = !selectedProjectId || entry.projectId === selectedProjectId;
      const matchesStatus = selectedStatus === 'All' || entry.status === selectedStatus;
      const entryDate = this.dateFromDateTime(entry.startTime);
      const matchesFrom = !from || entryDate >= from;
      const matchesTo = !to || entryDate <= to;

      return matchesProject && matchesStatus && matchesFrom && matchesTo;
    });
  });

  // Aggregated total allocation metrics across matching filtered entities
  readonly totalMinutes = computed(() => this.filteredEntries().reduce((total, entry) => total + entry.durationMinutes, 0));

  // Native asynchronous timer allocation reference tracking context
  private timerIntervalId: ReturnType<typeof setInterval> | null = null;

  constructor() {
    // Cascade resets: clear task selection when project changes, but preserve
    // an in-progress "create new task" entry so the user doesn't lose their typing.
    this.entryForm.controls.projectId.valueChanges.subscribe(() => {
      if (this.newTaskFormContext() !== 'manual') {
        this.entryForm.controls.taskId.setValue('');
      }
    });
    this.timerForm.controls.projectId.valueChanges.subscribe(() => {
      if (this.newTaskFormContext() !== 'timer') {
        this.timerForm.controls.taskId.setValue('');
      }
    });

    // Dynamic calculation: Synchronize real-time total duration when inputs change
    this.entryForm.controls.startTime.valueChanges.subscribe(() => this.updateDuration());
    this.entryForm.controls.endTime.valueChanges.subscribe(() => this.updateDuration());
  }

  // Returns true if the template end time is at or before the input start time bounds
  get endTimeInvalid(): boolean {
    return this.calculateDuration(this.entryForm.controls.startTime.value, this.entryForm.controls.endTime.value) <= 0;
  }

  // Prevent memory leaks if the user navigates away while a tracking session is active
  ngOnDestroy(): void {
    this.clearTimerInterval();
  }

  // cComponent and actions workflow
  headerDateLabel(): string {
    return this.formatDateLabel(this.filterForm.controls.from.value);
  }

  dismissConflict(): void {
    this.conflictMessage.set('');
  }

  dismissToast(): void {
    this.toastMessage.set('');
  }

  openManualPanel(): void {
    this.resetEntryForm();
    this.isEditMode.set(false);
    this.resetNewTaskState();
    this.activePanel.set('manual');
  }

  onTaskSelectionChange(event: Event, context: 'manual' | 'timer'): void {
    const value = (event.target as HTMLSelectElement).value;
    const form = context === 'manual' ? this.entryForm : this.timerForm;

    if (value === this.createNewTaskValue) {
      this.newTaskFormContext.set(context);
      this.newTaskTitle.set('');
      this.newTaskTitleError.set(false);
      form.controls.taskId.setValue('', { emitEvent: false });
      return;
    }

    this.resetNewTaskState();
  }

  openTimerPanel(): void {
    this.resetNewTaskState();
    this.activePanel.set('timer');
  }

  closePanel(): void {
    this.activePanel.set(null);
    this.openMenuEntryId.set(null);
  }

  /**
   * Persists log form values into the records array.
   * Performs bounds verification and double booking collision screening.
   */
  saveEntry(): void {
    this.updateDuration();

    if (this.endTimeInvalid) {
      this.conflictMessage.set('End time must be after start time.');
      return;
    }

    if (this.newTaskFormContext() === 'manual' && !this.newTaskTitle().trim()) {
      this.newTaskTitleError.set(true);
      return;
    }

    const entry = this.buildEntryFromForm();

    // Collision checking evaluation logic
    const hasConflict = this.entries().some((existingEntry) => (
      existingEntry.id !== entry.id
      && this.dateFromDateTime(existingEntry.startTime) === this.dateFromDateTime(entry.startTime)
      && this.timesOverlap(
        this.timeFromDateTime(existingEntry.startTime),
        this.timeFromDateTime(existingEntry.endTime),
        this.timeFromDateTime(entry.startTime),
        this.timeFromDateTime(entry.endTime)
      )
    ));

    if (hasConflict) {
      this.conflictMessage.set('This time entry overlaps with an existing entry.');
      return;
    }

    if (this.isEditMode()) {
      // Functional state updating: replaces array entity if unique matching structural target matches
      this.entries.update((entries) => entries.map((existingEntry) => existingEntry.id === entry.id ? entry : existingEntry));
      this.toastMessage.set('Time entry updated.');
    } else {
      // Standard prepend operation for completely fresh objects
      this.entries.update((entries) => [entry, ...entries]);
      this.toastMessage.set('Time entry saved.');
    }

    this.conflictMessage.set('');
    this.closePanel();
  }

   // Instantiates the async timer interval loops, updating counters progressively.

  startTimer(): void {
    if (this.newTaskFormContext() === 'timer' && !this.newTaskTitle().trim()) {
      this.newTaskTitleError.set(true);
      return;
    }

    const timer = {
      projectId: this.timerForm.controls.projectId.value,
      taskId: this.resolveTaskId('timer'),
      description: this.timerForm.controls.description.value,
      startedAt: new Date()
    };

    this.activeTimer.set(timer);
    this.elapsedSeconds.set(0);
    this.isTimerPaused.set(false);
    this.pausedElapsedSeconds.set(0);
    this.clearTimerInterval();

    // Increment tracking properties sequentially per second pass
    this.timerIntervalId = setInterval(() => {
      this.elapsedSeconds.set(Math.floor((Date.now() - timer.startedAt.getTime()) / 1000));
    }, 1000);
  }

  pauseTimer(): void {
    this.clearTimerInterval();
    this.isTimerPaused.set(true);
    this.pausedElapsedSeconds.set(this.elapsedSeconds());
  }

  resumeTimer(): void {
    const timer = this.activeTimer();
    if (!timer) {
      return;
    }

    this.isTimerPaused.set(false);
    this.clearTimerInterval();

    // Calculate the offset to continue from paused time
    const pausedSeconds = this.pausedElapsedSeconds();
    const startTime = Date.now() - (pausedSeconds * 1000);

    this.timerIntervalId = setInterval(() => {
      this.elapsedSeconds.set(Math.floor((Date.now() - startTime) / 1000));
    }, 1000);
  }

   // Evaluates the active time tracking segment and builds a concrete log entry.

  stopTimer(): void {
    const timer = this.activeTimer();

    if (!timer) {
      return;
    }

    const end = new Date();
    const durationMinutes = Math.max(1, Math.round(this.elapsedSeconds() / 60)); // Standardizes round up minimum limits
    const request = this.buildTimerRequest(timer, end, durationMinutes);

    this.entries.update((entries) => [{
      id: this.createId(),
      ...request,
      taskId: request.taskId ?? '',
      status: 'DRAFT',
      isLocked: false,
      createdAt: new Date().toISOString().slice(0, 19),
      updatedAt: new Date().toISOString().slice(0, 19)
    }, ...entries]);

    this.activeTimer.set(null);
    this.elapsedSeconds.set(0);
    this.isTimerPaused.set(false);
    this.pausedElapsedSeconds.set(0);
    this.clearTimerInterval();
    this.toastMessage.set('Timer entry saved.');
    this.closePanel();
  }

  timerStartedLabel(): string {
    const timer = this.activeTimer();
    return timer ? `Started at ${timer.startedAt.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}` : '';
  }

  // Converts numerical raw seconds parameters into visual presentation standard "HH:MM:SS" formats
  formatElapsed(totalSeconds: number): string {
    const hours = Math.floor(totalSeconds / 3600);
    const minutes = Math.floor((totalSeconds % 3600) / 60);
    const seconds = totalSeconds % 60;

    return [hours, minutes, seconds].map((value) => value.toString().padStart(2, '0')).join(':');
  }

  // Custom visual modifier turning simple dynamic counts into compressed time descriptors
 ormatDuration(minutes?: number | null): string {
  const safeMinutes = minutes ?? 0;
    const hours = Math.floor(safeMinutes / 60);
    const remainingMinutes = safeMinutes % 60;

    if (!hours) {
      return `${remainingMinutes}m`;
    }

    return remainingMinutes ? `${hours}h ${remainingMinutes}m` : `${hours}h`;
  }

  selectStatus(status: StatusOption): void {
    this.selectedStatus.set(status);
  }

  // Resolves contextual CSS formatting handles depending on workflow state settings
  entryDotClass(entry: TimeEntry): string {
    return `status-dot status-dot--${entry.status.toLowerCase()}`;
  }

  formatEntryTimeRange(entry: TimeEntry): string {
    return `${this.timeFromDateTime(entry.startTime)} - ${this.timeFromDateTime(entry.endTime)}`;
  }

  getProjectName(projectId: string): string {
    return this.projects().find((project) => project.id === projectId)?.name ?? 'Unknown project';
  }

  getTaskTitle(taskId: string): string {
    return this.tasks().find((task) => task.id === taskId)?.title ?? 'No task selected';
  }

   //populates editing controls with targets extraction contexts to alter parameters.

  editEntry(entry: TimeEntry): void {
    this.resetNewTaskState();
    this.entryForm.setValue({
      id: entry.id,
      projectId: entry.projectId,
      taskId: entry.taskId,
      entryType: entry.entryType,
      startTime: this.timeFromDateTime(entry.startTime),
      endTime: this.timeFromDateTime(entry.endTime),
      durationMinutes: entry.durationMinutes,
      description: entry.description
    });
    this.isEditMode.set(true);
    this.activePanel.set('manual');
  }

  toggleEntryMenu(entryId: string): void {
    this.openMenuEntryId.set(this.openMenuEntryId() === entryId ? null : entryId);
  }

  submitEntry(entry: TimeEntry): void {
    this.updateEntryStatus(entry.id, 'SUBMITTED');
    this.openMenuEntryId.set(null);
    this.toastMessage.set('Time entry submitted.');
  }

  deleteEntry(entry: TimeEntry): void {
    this.entries.update((entries) => entries.filter((existingEntry) => existingEntry.id !== entry.id));
    this.openMenuEntryId.set(null);
    this.toastMessage.set('Time entry deleted.');
  }

  resubmitEntry(entry: TimeEntry): void {
    this.updateEntryStatus(entry.id, 'SUBMITTED');
    this.toastMessage.set('Time entry resubmitted.');
  }

  // data sanitization methods

  private resetEntryForm(): void {
    this.resetNewTaskState();
    this.entryForm.setValue({
      id: '',
      projectId: this.projects()[0]?.id ?? '',
      taskId: '',
      entryType: 'DEVELOPMENT',
      startTime: '09:00',
      endTime: '10:00',
      durationMinutes: 60,
      description: ''
    });
  }

  private resetNewTaskState(): void {
    this.newTaskFormContext.set(null);
    this.newTaskTitle.set('');
    this.newTaskTitleError.set(false);
  }

  private resolveTaskId(context: 'manual' | 'timer'): string {
    const form = context === 'manual' ? this.entryForm : this.timerForm;

    if (this.newTaskFormContext() !== context) {
      return form.controls.taskId.value;
    }

    const title = this.newTaskTitle().trim();
    if (!title) {
      return '';
    }

    const projectId = form.controls.projectId.value;
    const newTask: Task = {
      id: this.createTaskId(),
      projectId,
      title
    };

    this.tasks.update((tasks) => [...tasks, newTask]);
    form.controls.taskId.setValue(newTask.id);
    this.resetNewTaskState();

    return newTask.id;
  }

private createTaskId(): string {
  return `task-${Date.now()}-${crypto.randomUUID()}`;
}

  private updateDuration(): void {
    const duration = Math.max(0, this.calculateDuration(this.entryForm.controls.startTime.value, this.entryForm.controls.endTime.value));
    this.entryForm.controls.durationMinutes.setValue(duration, { emitEvent: false });
  }

  private buildEntryFromForm(): TimeEntry {
    const request = this.buildTimeEntryRequestFromForm();
    const now = new Date().toISOString().slice(0, 19);

    return {
      id: this.entryForm.controls.id.value || this.createId(),
      ...request,
      taskId: request.taskId ?? '',
      status: 'DRAFT',
      isLocked: false,
      createdAt: now,
      updatedAt: now
    };
  }

  /**
   * Constructs the structured object sent out downstream during API communication operations.
   * Eliminates entity variables handled solely by the backend architecture.
   */
  buildTimeEntryRequestFromForm(): TimeEntryRequest {
    const selectedDate = this.filterForm.controls.from.value || this.today();

    return {
      projectId: this.entryForm.controls.projectId.value,
      taskId: this.resolveTaskId('manual') || null,
      startTime: this.toDateTimeValue(selectedDate, this.entryForm.controls.startTime.value),
      endTime: this.toDateTimeValue(selectedDate, this.entryForm.controls.endTime.value),
      durationMinutes: this.entryForm.controls.durationMinutes.value,
      entryType: this.entryForm.controls.entryType.value,
      description: this.entryForm.controls.description.value || 'No description provided.'
    };
  }

  private buildTimerRequest(timer: ActiveTimer, end: Date, durationMinutes: number): TimeEntryRequest {
    return {
      projectId: timer.projectId,
      taskId: timer.taskId || null,
      startTime: this.toDateTimeValue(this.dateFromDate(timer.startedAt), this.toTimeValue(timer.startedAt)),
      endTime: this.toDateTimeValue(this.dateFromDate(end), this.toTimeValue(end)),
      durationMinutes,
      entryType: 'DEVELOPMENT',
      description: timer.description || 'Timer entry'
    };
  }

  private updateEntryStatus(entryId: string, status: TimeEntryStatus): void {
    this.entries.update((entries) => entries.map((entry) => (
      entry.id === entryId ? { ...entry, status, rejectionReason: undefined } : entry
    )));
  }

  private calculateDuration(startTime: string, endTime: string): number {
    return this.timeToMinutes(endTime) - this.timeToMinutes(startTime);
  }

  // Checks overlap conditions by comparing raw mathematical minute counters
  private timesOverlap(startA: string, endA: string, startB: string, endB: string): boolean {
    return this.timeToMinutes(startA) < this.timeToMinutes(endB) && this.timeToMinutes(startB) < this.timeToMinutes(endA);
  }

  // Parses textual time records into flat integer values relative to midnight
  private timeToMinutes(time: string): number {
    const timeValue = this.timeFromDateTime(time);
    const [safeHours, safeMinutes] = timeValue.split(':').map(Number);
    return (safeHours * 60) + safeMinutes;
  }

  private today(): string {
    return new Date().toISOString().slice(0, 10); // Returns YYYY-MM-DD
  }

  private dateFromDate(date: Date): string {
    return date.toISOString().slice(0, 10);
  }

  private dateFromDateTime(dateTime: string): string {
    return dateTime.slice(0, 10);
  }

  private timeFromDateTime(dateTime: string): string {
    return dateTime.includes('T') ? dateTime.slice(11, 16) : dateTime; // Extracts HH:MM from ISO string
  }

  private toTimeValue(date: Date): string {
    return date.toTimeString().slice(0, 5);
  }

  private toDateTimeValue(date: string, time: string): string {
    return `${date}T${time}:00`;
  }

  private formatDateLabel(dateValue: string): string {
    return new Date(`${dateValue}T00:00:00`).toLocaleDateString([], {
      weekday: 'short',
      month: 'short',
      day: 'numeric'
    });
  }

  // Local unique runtime identifier fallback algorithm
private createId(): string {
  return `entry-${crypto.randomUUID()}`;
}

  private clearTimerInterval(): void {
    if (this.timerIntervalId) {
      clearInterval(this.timerIntervalId);
      this.timerIntervalId = null;
    }
  }
}