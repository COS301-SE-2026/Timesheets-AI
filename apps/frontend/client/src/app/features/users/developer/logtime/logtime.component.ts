/**
 * Author: Lerato Sibanda
 * Date: 2026-05-17
 * Purpose: Handles time entry logging with manual and timer based entry methods.
 * Related requirement: -
 *
 * Patched: Zamokuhle Zwane, 2026-07-23
 * Below is a list of changes i made. the proble was just we had too many moxk values. and some db changes affected, like MANUAL and TIMER
 * - entryType values changed to uppercase (MANUAL/TIMER) to match the entryType CHECK constraint on the time_entries table
 * - TimeEntryRequest now sends durationSeconds instead of durationMinutes, matching the real POST /api/time-entries request schema
 * - StartTimerRequest no longer sends 'notes', matches real StartTimerRequest schema (projectId, taskId only), response shape still unconfirmed with backend
 * - requestOptions() no longer sets Authorization manually, that's handled by the shared JWT interceptor in core/services/auth.service.ts
 * - submit moved from per-entry to per-timesheet, status/submittedAt/ pprovedAt/isLocked all live on the timesheets table, not time_entries, see submitTimesheet() and loadCurrentTimesheet() for details
 *
 * Patched: Zamokuhle Zwane, 2026-07-28
 * SO that the timer doesnt persist, so when i go to the next page, it goes away and thats a problem because that means the timer would never stop and its a silent error
 *
 * Patched: Zamokuhle Zwane, 03 August 2026
 * Fixed time related issues, it was 2 hours behind, 
 * so i added a new function to format the duration to show hours, minutes and seconds
 * i also fixed the total duration to show the correct total duration across all filtered entries
 */

import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Component, computed, inject, OnDestroy, signal } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import {
  TimerService,
  ActiveTimerResponse,
  StopTimerResponse,
} from '../../../../core/services/timer.service';
import {
  TimeEntryService,
  TimeEntryRequest as TimeEntryApiRequest,
} from '../../../../core/services/time-entry.service';
//type definitions and interface

type ViewOption = 'Day' | 'Week' | 'Month';
type StatusOption = 'All' | TimeEntryStatus;
type PanelType = 'manual' | 'timer' | null;
type TimeEntryStatus = 'DRAFT' | 'SUBMITTED' | 'APPROVED' | 'REJECTED';

//was: type EntryType = 'manual' | 'timer' | 'import' | 'api';
//backend CHECK constraint on time_entries.entryType only allows MANUAL and
//TIMER, uppercase. confirmed against swagger schema string values and
//team memory notes re: the entryType column constraint
type EntryType = 'MANUAL' | 'TIMER';

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
  taskId: string | null;
  entryType: EntryType;
  startTime: string; // ISO String (YYYY-MM-DDTHH:mm:ss)
  endTime: string; // ISO String (YYYY-MM-DDTHH:mm:ss)
  durationSeconds: number;
  durationMinutes: number;
  description: string;
  status: TimeEntryStatus;
  isLocked?: boolean;
  createdAt?: string;
  updatedAt?: string;
  rejectionReason?: string;
}

/*
Interface representing the sanitized data structure expected by external backend APIs durationSeconds not durationMinutes, this matches the real
 POST /api/time-entries request body in swagger, the response comes back with durationMinutes but the request wants seconds. these are not the
 same field renamed, they're genuinely different units, so conversion happens explicitly in buildTimeEntryRequestFromForm()
 */
interface TimeEntryRequest {
  projectId: string;
  taskId: string | null;
  startTime: string;
  endTime: string;
  durationSeconds: number;
  entryType: EntryType;
  description: string;
}

interface ActiveTimer {
  id?: string;
  projectId: string;
  taskId: string | null;
  notes: string;
  startedAt: Date;
}

/*
this was: interface StartTmerRequest { projectId, taskId, notes }
real StartTimerRequest schema in swagger only has projectId and taskId, no notes field. the timer's notes/description stays local to the
ActiveTimer signal and only gets attached when the entry is eventually created via buildTimerRequest()
*/
interface StartTimerRequest {
  projectId: string;
  taskId: string | null;
}

/* 
just minimal shape needed from TimesheetResponse, just enough to drive the submit button and status label, not pulling in every field since the
component doesn't need budget/approval details right now
*/
interface Timesheet {
  id: string;
  periodStart: string;
  periodEnd: string;
  status: TimeEntryStatus;
  isLocked: boolean;
  rejectionReason?: string | null;
}

//minimal shape needed from ProjectResponse, matches GET /api/projects
interface ProjectApiResponse {
  id: string;
  name: string;
}

//minimal shape needed from TaskResponse, matches GET /api/tasks/my-tasks
interface TaskApiResponse {
  id: string;
  projectId: string;
  title: string;
}

@Component({
  selector: 'app-logtime',
  imports: [ReactiveFormsModule],
  templateUrl: './logtime.component.html',
  styleUrl: './logtime.component.scss',
})
export class LogtimeComponent implements OnDestroy {
  /*
  was `inject(HttpClient, { optional: true })` with a
  backendEnabled flag gating mock-data fallbacks throughout this file, so after integrating we're on real seed data against a live backend now, and the
  mock branches were dead weight
  */
  private readonly http = inject(HttpClient);
  private readonly apiBaseUrl = '/api';
  private readonly timerService = inject(TimerService);
  private readonly timeEntryService = inject(TimeEntryService);
  private readonly route = inject(ActivatedRoute);
  private readonly pendingTaskId: string | null = null;

  //I used Enzokuhle Khumalo's workspace_members.id (real seed data), because it didnt need mfa
  private readonly workspaceMemberId =
    localStorage.getItem('workspaceMemberId') ??
    '00000000-0000-0000-0002-000000000021';

  //Static configuration options for template elements
  readonly viewOptions: ViewOption[] = ['Day', 'Week', 'Month'];
  readonly statusOptions: StatusOption[] = [
    'All',
    'DRAFT',
    'SUBMITTED',
    'APPROVED',
    'REJECTED',
  ];
  readonly createNewTaskValue = '__create_new__';

  //Angular signals ( For state management )

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
  readonly filterFrom = signal(this.today());
  readonly filterTo = signal(this.today());
  readonly durationPreviewSeconds = signal(3600);

  /*  
  tracks the current period's timesheet, submit now happens at this level not per entry, since status/submittedAt/approvedAt/isLocked all live on
  the timesheets table per backend schema, not on time_entries
  */
  readonly currentTimesheet = signal<Timesheet | null>(null);

  /*
  I populated from GET /api/projects and GET /api/tasks/my-tasks on initial load
  see the loadProjects()/loadTasks(), starts empty rather than pre-seeded with mock rows, same reasoning as the entries signal below.
  */

  readonly projects = signal<Project[]>([]);

  readonly tasks = signal<Task[]>([
    { id: '', projectId: '', title: 'No task selected' },
  ]);

  /*
  it starts off empty and gets populated by loadEntries() on initial load, fetching real rows from the backend. All mocks have been removed from this component (see loadProjects,
  loadTasks, loadEntries, loadCurrentTimesheet, saveEntry, startTimer, stopTimer, deleteEntry, submitTimesheet), so this now assumes a live backend connection is always available.
  */
  readonly entries = signal<TimeEntry[]>([]);

  //Reactive from groups

  //Scopes dashboard visibility metrics
  readonly filterForm = new FormGroup({
    from: new FormControl(this.today(), { nonNullable: true }),
    to: new FormControl(this.today(), { nonNullable: true }),
  });

  //Controls the explicit manual logging sidebar template
  readonly entryForm = new FormGroup({
    id: new FormControl('', { nonNullable: true }),
    projectId: new FormControl('', { nonNullable: true }), // set once loadProjects() resolves
    taskId: new FormControl('', { nonNullable: true }),
    entryType: new FormControl<EntryType>('MANUAL', { nonNullable: true }),
    startTime: new FormControl('09:00', { nonNullable: true }),
    endTime: new FormControl('10:00', { nonNullable: true }),
    durationMinutes: new FormControl(60, { nonNullable: true }),
    description: new FormControl('', { nonNullable: true }),
  });

  //Handles real-time progressive timer context entries
  readonly timerForm = new FormGroup({
    projectId: new FormControl('', { nonNullable: true }), // its set once loadProjects() resolves
    taskId: new FormControl('', { nonNullable: true }),
    description: new FormControl('', { nonNullable: true }),
  });

  //Computed signals (Reactive  derived state)

  //Cascades tasks depending on the active project selected inside the manual logging form
  readonly filteredTasks = computed(() => this.tasks());

  readonly selectableTasks = computed(() =>
    this.tasks().filter((task) => task.id),
  );

  readonly selectableTimerTasks = computed(() =>
    this.tasks().filter((task) => task.id),
  );

  readonly isCreatingNewManualTask = computed(
    () => this.newTaskFormContext() === 'manual',
  );
  readonly isCreatingNewTimerTask = computed(
    () => this.newTaskFormContext() === 'timer',
  );

  //Cascades tasks depending on the project selected inside the automated timer form
  readonly timerTasks = computed(() =>
    this.tasks().filter(
      (task) =>
        !task.id || task.projectId === this.timerForm.controls.projectId.value,
    ),
  );

  //Multi variable matrix filter sorting out visible logs matching active global criteria
  readonly filteredEntries = computed(() => {
    const selectedProjectId = this.selectedProjectId();
    const selectedStatus = this.selectedStatus();
    const from = this.filterFrom();
    const to = this.filterTo();
    const timesheetStatus = this.currentTimesheet()?.status ?? 'DRAFT';

    return this.entries().filter((entry) => {
      const matchesProject =
        !selectedProjectId || entry.projectId === selectedProjectId;
      const matchesStatus =
        selectedStatus === 'All' || timesheetStatus === selectedStatus;
      const entryDate = this.dateFromDateTime(entry.startTime);
      const matchesFrom = !from || entryDate >= from;
      const matchesTo = !to || entryDate <= to;

      return matchesProject && matchesStatus && matchesFrom && matchesTo;
    });
  });

  //Aggregated total allocation metrics across matching filtered entities
  readonly totalMinutes = computed(() =>
    this.filteredEntries().reduce(
      (total, entry) => total + entry.durationMinutes,
      0,
    ),
  );

  readonly totalSeconds = computed(() =>
    this.filteredEntries().reduce(
      (total, entry) => total + entry.durationMinutes * 60,
      0,
    ),
  );

  /* Added a dynamic label for the entries card header, it was hardcoded as "Today's Entries" in the template but it didnt make sense because of the date filter
    so this will replace the actual range being viewed. so the heading isnt lying
  */
  readonly entriesCardTitle = computed(() => {
    const from = this.filterFrom();
    const to = this.filterTo();
    const today = this.today();

    if (from === today && to === today) {
      return "Today's Entries";
    }

    if (from === to) {
      return `Entries: ${this.formatDateLabel(from)}`;
    }

    return `Entries: ${this.formatDateLabel(from)} – ${this.formatDateLabel(to)}`;
  });

  readonly canSubmitTimesheet = computed (() => {
    const timesheet = this.currentTimesheet();
    return (
      !!timesheet &&
      !timesheet.isLocked &&
      (timesheet.status === 'DRAFT' || timesheet.status === 'REJECTED')
    );
  });

  readonly canEditEntries = computed(() => this.canSubmitTimesheet());

  //Native asynchronous timer allocation reference tracking context
  private timerIntervalId: ReturnType<typeof setInterval> | null = null;

  constructor() {
    /*
    Cascade resets: clear task selection when project changes, but preserve
    an in-progress "create new task" entry so the user doesn't lose their typing.
    */

    const query = this.route.snapshot.queryParamMap;
    const from = query.get('from') ?? query.get('date');
    const  to = query.get('to') ?? from;
    this.pendingTaskId = query.get('taskId');
    if(from) {
      this.filterForm.controls.from.setValue(from, {emitEvent: false });
      this.filterFrom.set(from);
    }
    if (to) {
      this.filterForm.controls.to.setValue(to, { emitEvent: false });
      this.filterTo.set(to);
    }

    this.entryForm.controls.projectId.valueChanges.subscribe((projectId) => {
      this.entryForm.controls.taskId.setValue('');
      this.loadTasksForProject(projectId);
    });
    this.timerForm.controls.projectId.valueChanges.subscribe((projectId) => {
      this.timerForm.controls.taskId.setValue('');
      this.loadTasksForProject(projectId);
    });

    //Dynamic calculation: Synchronize real-time total duration when inputs change
    this.entryForm.controls.startTime.valueChanges.subscribe(() =>
      this.updateDuration(),
    );
    this.entryForm.controls.endTime.valueChanges.subscribe(() =>
      this.updateDuration(),
    );
    this.filterForm.controls.from.valueChanges.subscribe(() => {
      this.filterFrom.set(this.filterForm.controls.from.value);
      this.onDateRangeChange();
    });
    this.filterForm.controls.to.valueChanges.subscribe(() => {
      this.filterTo.set(this.filterForm.controls.to.value);
      this.onDateRangeChange();
    });
    /*
    load the active timesheet on startup, this drives whether the submit
    button shows and what it submits against
    */
    this.loadCurrentTimesheet();

    /*
    so it will fetch real entries from the backend on init so the mock seed rows (ids prefixed 0003-...) get replaced instead of
    lingering in the list and causing 404s/500s when deleted or edited, since those ids were never inserted into the real time_entries table
    */
    this.loadEntries();

    /*
    so this will restore the inprogress timer on load back to this page, without
    this navigating away makes it seem like its lost because the forntend doesnt know it exists  
    */
    this.loadActiveTimer();

    /*
    will fetch real projects/tasks too, these were previously hardcoded mock arrays mixed with one real seed row each, which
    meant most of the project/task dropdown was picking IDs the backend had never heard of. Loading real data removes that trap entirely
    */
    this.loadProjects();
  }
  private onDateRangeChange(): void {
    this.loadCurrentTimesheet();
  }

  //Returns true if the template end time is at or before the input start time bounds
  get endTimeInvalid(): boolean {
    return (
      this.calculateDuration(
        this.entryForm.controls.startTime.value,
        this.entryForm.controls.endTime.value,
      ) <= 0
    );
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
  }

  /*
   Persists log form values into the records array.
   Performs bounds verification and double booking collision screening.
   */
  saveEntry(): void {
    this.updateDuration();

    if (this.endTimeInvalid) {
      this.conflictMessage.set('End time must be after start time.');
      return;
    }
    //when the user picks new task but never typed a title, block the save entirely.
    if (this.isCreatingNewManualTask() && !this.newTaskTitle().trim()) {
      this.newTaskTitleError.set(true);
      return;
    }
    //Backlog: this generates the new task client side
    const request = this.buildTimeEntryRequestFromForm();
    const entry = this.buildEntryFromForm(request);

    // Collision checking evaluation logic
    const hasConflict = this.entries().some(
      (existingEntry) =>
        existingEntry.id !== entry.id &&
        this.dateFromDateTime(existingEntry.startTime) ===
          this.dateFromDateTime(entry.startTime) &&
        this.timesOverlap(
          this.timeFromDateTime(existingEntry.startTime),
          this.timeFromDateTime(existingEntry.endTime),
          this.timeFromDateTime(entry.startTime),
          this.timeFromDateTime(entry.endTime),
        ),
    );

    if (hasConflict) {
      this.conflictMessage.set(
        'This time entry overlaps with an existing entry.',
      );
      return;
    }

    const onSaved = (savedEntry: TimeEntry) => {
      /*
      the backend's `durationMinutes` field on the response is actually returning seconds, not minutes, confirmed by
      a 1-hour entry (09:00–10:00) displaying as "60h" and a 1-minute entry (08:00–08:01) displaying as "1h", both exactly 60 times too large
      Converting here so the rest of the app (formatDuration, totals, etc) works with real minutes. If the backend fixes this field, remove this division
      */
      const normalisedEntry: TimeEntry = {
        ...this.normaliseEntryDuration(savedEntry),
        status:
          savedEntry.status ??
          (this.isEditMode()
            ? this.entries().find((e) => e.id === savedEntry.id)?.status
            : undefined) ??
          'DRAFT',
      };

      if (this.isEditMode()) {
        this.entries.update((entries) =>
          entries.map((existingEntry) =>
            existingEntry.id === normalisedEntry.id
              ? normalisedEntry
              : existingEntry,
          ),
        );
        this.toastMessage.set('Time entry updated.');
      } else {
        this.entries.update((entries) => [normalisedEntry, ...entries]);
        this.toastMessage.set('Time entry saved.');
      }
      this.conflictMessage.set('');
      this.closePanel();
    };

    const response = this.isEditMode()
      ? this.http.put<TimeEntry>(
          `${this.apiBaseUrl}/time-entries/${entry.id}`,
          request,
          this.requestOptions(),
        )
      : this.http.post<TimeEntry>(
          `${this.apiBaseUrl}/time-entries`,
          request,
          this.requestOptions(),
        );

    response.subscribe({
      next: (savedEntry) => onSaved(savedEntry),
      error: (error) =>
        this.conflictMessage.set(
          error.error?.message ?? 'Unable to save the time entry.',
        ),
    });
  }

  //Instantiates the async timer interval loops, updating counters progressively

  startTimer(): void {
    const timer: ActiveTimer = {
      projectId: this.timerForm.controls.projectId.value,
      taskId: this.resolveTaskId('timer') || null,
      notes: this.timerForm.controls.description.value,
      startedAt: new Date(),
    };

    const activateTimer = (activeTimer: ActiveTimer) => {
      this.activeTimer.set(activeTimer);
      this.elapsedSeconds.set(0);
      this.isTimerPaused.set(false);
      this.pausedElapsedSeconds.set(0);
      this.clearTimerInterval();
      /*
      disable via the FormGroup API rather than a template [disabled] binding, per Angular's reactive-forms guidance (avoids the
      "disabled attribute with a reactive form directive" warning and potential ExpressionChangedAfterItHasBeenChecked errors)
      */
      this.timerForm.disable({ emitEvent: false });
      //Increment tracking properties sequentially per second pass
      this.timerIntervalId = setInterval(() => {
        this.elapsedSeconds.set(
          Math.floor((Date.now() - activeTimer.startedAt.getTime()) / 1000),
        );
      }, 1000);
    };

    const request: StartTimerRequest = {
      projectId: timer.projectId,
      taskId: timer.taskId,
    };
    this.http
      .post(`${this.apiBaseUrl}/timers/start`, request, this.requestOptions())
      .subscribe({
        next: () => activateTimer(timer),
        error: (error) =>
          this.conflictMessage.set(
            error.error?.message ?? 'Unable to start the timer.',
          ),
      });
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
    const startTime = Date.now() - pausedSeconds * 1000;

    this.timerIntervalId = setInterval(() => {
      this.elapsedSeconds.set(Math.floor((Date.now() - startTime) / 1000));
    }, 1000);
  }

  //Evaluates the active time tracking segment and builds a concrete log entry

  stopTimer(): void {
    const timer = this.activeTimer();

    if (!timer) {
      return;
    }

    const end = new Date();
    const startDateStr = this.dateFromDate(timer.startedAt);
    const endDateStr = this.dateFromDate(end);

    /*
    Edge case: a timer that runs past midnight can't be safely checked by the same single-date overlap logic saveEntry() uses (that logic
    assumes one entry = one calendar day). Rather than silently produce a wrong overlap check (or worse, silently save a corrupt entry),
    block and ask the user to split it manually, This is rare but not impossible for a timer left running overnight
    Known limitation: During anamoly detection this will flag unusual task hours
    */
    if (startDateStr !== endDateStr) {
      this.conflictMessage.set(
        "This timer ran past midnight and spans two days. Please stop and re-log it as two separate manual entries — automatic entries can't cross a day boundary yet.",
      );
      return;
    }

    const startTimeValue = this.toDateTimeValue(
      startDateStr,
      this.toTimeValue(timer.startedAt),
    );
    const endTimeValue = this.toDateTimeValue(
      endDateStr,
      this.toTimeValue(end),
    );

    const checkConflictAndProceed = () => {
      /*
      Same collision check as saveEntry(). Runs against whatever is in this.entries() at call time, see the reload above, which ensures
      that's fresh rather than whatever was loaded when the panel opened.
      */
      const hasConflict = this.entries().some(
        (existingEntry) =>
          this.dateFromDateTime(existingEntry.startTime) === startDateStr &&
          this.timesOverlap(
            this.timeFromDateTime(existingEntry.startTime),
            this.timeFromDateTime(existingEntry.endTime),
            this.timeFromDateTime(startTimeValue),
            this.timeFromDateTime(endTimeValue),
          ),
      );

      if (hasConflict) {
        this.conflictMessage.set(
          'This timer overlaps with an existing time entry. Adjust or delete the conflicting entry before saving.',
        );
        return;
      }

      this.persistTimerEntry();
    };

    /*
    Refresh from the backend first so the overlap check isn't relying on potentially stale client-side state (e.g. an entry created in another tab/session while this timer was running).
    */
    this.http
      .get<TimeEntry[]>(
        `${this.apiBaseUrl}/time-entries/me`,
        this.requestOptions(),
      )
      .subscribe({
        next: (fetchedEntries) => {
          this.entries.set(
            fetchedEntries.map((entry) => ({
              ...entry,
              durationSeconds: entry.durationMinutes, 
              durationMinutes: Math.round(entry.durationMinutes / 60),
            })),
          );
          checkConflictAndProceed();
        },
        error: () => {
          /*
          if the refresh itself fails, fall back to checking against
          whatever's already loaded rather than blocking the user
          entirely, better to check stale data than no data
          */
          checkConflictAndProceed();
        },
      });
  }

  //discard the active time because it doesnt stop it properly
  cancelTimer(): void {
    const timer = this.activeTimer();
    if (!timer) {
      //no timer running, nothing to cancel, just close the panel
      this.closePanel();
      return;
    }

    this.timerService.discardTimer().subscribe({
      next: () => this.resetTimerState('Timer entry discarded.'),
      error: (error) =>
        this.conflictMessage.set(
          error.error?.message ?? 'Unable to discard the timer.',
        ),
    });
  }

  /*
   this persists a stopped timer as a time entry via the backend, split out from stopTimer() so the overlap-check step above can gate this
   without duplicating the save logic
  */
  private persistTimerEntry(): void {
    const timer = this.activeTimer();
    const attachNotesIfPresent = (createdEntryId: string): void => {
      const notes = timer?.notes?.trim();
      if (!notes) {
        return;
      }
      const updateRequest: TimeEntryApiRequest = {
        projectId: timer!.projectId,
        taskId: timer!.taskId ?? '',
        startTime: this.toDateTimeValue(
          this.dateFromDate(timer!.startedAt),
          this.toTimeValue(timer!.startedAt),
        ),
        endTime: this.toDateTimeValue(
          this.dateFromDate(new Date()),
          this.toTimeValue(new Date()),
        ),
        durationSeconds: this.elapsedSeconds(),
        entryType: 'TIMER',
        description: notes,
      };

      this.timeEntryService
        .updateEntry(createdEntryId, updateRequest)
        .subscribe({
          error: (error) =>
            console.error(
              'LogtimeComponent: Failed to attach notes to timer entry',
              { entryId: createdEntryId, error },
            ),
        });
    };

    this.http
      .post<StopTimerResponse>(
        `${this.apiBaseUrl}/timers/stop`,
        null,
        this.requestOptions(),
      )
      .subscribe({
        next: (response) => {
          if(!response.createdTimeEntry?.id) {
            console.error(
              '[LogTimeComponent] StopTimerResponse missing createdTimeEntry.id',
              response,
            );
          }else{
            attachNotesIfPresent(response.createdTimeEntry.id);
          }
          this.loadEntries();
          this.resetTimerState('Timer entry saved.');
        },
        error: (error) =>
          this.conflictMessage.set(
            error.error?.message ?? 'Unable to stop the timer.',
          ),
      });
  }

  timerStartedLabel(): string {
    const timer = this.activeTimer();
    return timer
      ? `Started at ${timer.startedAt.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}`
      : '';
  }

  // Converts numerical raw seconds parameters into visual presentation standard "HH:MM:SS" formats
  formatElapsed(totalSeconds: number): string {
    const hours = Math.floor(totalSeconds / 3600);
    const minutes = Math.floor((totalSeconds % 3600) / 60);
    const seconds = totalSeconds % 60;

    return [hours, minutes, seconds]
      .map((value) => value.toString().padStart(2, '0'))
      .join(':');
  }

  // Custom visual modifier turning simple dynamic counts into compressed time descriptors
  formatDuration(totalSeconds = 0): string {
    const hours = Math.floor(totalSeconds / 3600);
    const minutes = Math.floor((totalSeconds % 3600) / 60);
    const seconds = totalSeconds % 60;

    if (hours === 0 && minutes === 0) return `${seconds}s`;
    if (hours === 0) return seconds === 0 ? `${minutes}m` : `${minutes}m ${seconds}s`;
    if (minutes === 0 && seconds === 0) return `${hours}h`;

    return `${hours}h ${minutes}m`;
  }

  selectStatus(status: StatusOption): void {
    this.selectedStatus.set(status);
  }

  // Resolves contextual CSS formatting handles depending on workflow state settings
  entryDotClass(entry: TimeEntry): string {
    return `status-dot status-dot--${(entry.status ?? 'draft').toLowerCase()}`;
  }

  formatEntryTimeRange(entry: TimeEntry): string {
    return `${this.timeFromDateTime(entry.startTime)} - ${this.timeFromDateTime(entry.endTime)}`;
  }

  getProjectName(projectId: string): string {
    return (
      this.projects().find((project) => project.id === projectId)?.name ??
      'Unknown project'
    );
  }

  getTaskTitle(taskId: string | null): string {
    return (
      this.tasks().find((task) => task.id === taskId)?.title ??
      'No task selected'
    );
  }

  //populates editing controls with targets extraction contexts to alter parameters

  editEntry(entry: TimeEntry): void {
    this.resetNewTaskState();
    this.entryForm.setValue({
      id: entry.id,
      projectId: entry.projectId,
      taskId: entry.taskId ?? '',
      entryType: entry.entryType,
      startTime: this.timeFromDateTime(entry.startTime),
      endTime: this.timeFromDateTime(entry.endTime),
      durationMinutes: entry.durationMinutes,
      description: entry.description,
    });
    this.isEditMode.set(true);
    this.activePanel.set('manual');
  }

  toggleEntryMenu(entryId: string): void {
    this.openMenuEntryId.set(
      this.openMenuEntryId() === entryId ? null : entryId,
    );
  }

  /*
  This submits the whole current timesheet, not a single entry.
  because status, submittedAt, approvedAt, and isLocked all live on the timesheets table per backend schema (see TimesheetResponse in swagger),
  TimeEntryResponse has none of these fields. approve/reject also operate on the timesheet as a unit (POST /api/timesheets/{id}/approve), so
  submitting entry by entry doesn't line up with how the backend models the approval workflow. this replaces the old per-entry submit button
   */
  submitTimesheet(): void {
    const timesheet = this.currentTimesheet();

    if (!timesheet) {
      this.conflictMessage.set('No timesheet found for this period yet.');
      return;
    }

    if (timesheet.isLocked) {
      this.conflictMessage.set(
        'This timesheet is locked and cannot be submitted again.',
      );
      return;
    }

    const markSubmitted = (updated: Timesheet) => {
      this.currentTimesheet.set(updated);
      /*
      reflect the change across every entry in the current view, since
      entries no longer carry their own submittable state independently
      */
      this.entries.update((entries) =>
        entries.map((entry) => ({
          ...entry,
          status: 'SUBMITTED' as TimeEntryStatus,
        })),
      );
      this.toastMessage.set('Timesheet submitted.');
    };

    this.http
      .post<Timesheet>(
        `${this.apiBaseUrl}/timesheets/${timesheet.id}/submit`,
        null,
        this.requestOptions(),
      )
      .subscribe({
        next: (updated) => markSubmitted(updated),
        error: (error) =>
          this.conflictMessage.set(
            error.error?.message ?? 'Unable to submit the timesheet.',
          ),
      });
  }

  deleteEntry(entry: TimeEntry): void {
    const deleted = () => {
      this.entries.update((entries) =>
        entries.filter((existingEntry) => existingEntry.id !== entry.id),
      );
      this.openMenuEntryId.set(null);
      this.toastMessage.set('Time entry deleted.');
    };

    this.http
      .delete(
        `${this.apiBaseUrl}/time-entries/${entry.id}`,
        this.requestOptions(),
      )
      .subscribe({
        next: deleted,
        error: (error) =>
          this.conflictMessage.set(
            error.error?.message ?? 'Unable to delete the time entry,',
          ),
      });
  }

  // data sanitization methods

  private resetEntryForm(): void {
    this.resetNewTaskState();
    this.entryForm.setValue({
      id: '',
      projectId: this.projects()[0]?.id ?? '',
      taskId: '',
      entryType: 'MANUAL',
      startTime: '09:00',
      endTime: '10:00',
      durationMinutes: 60,
      description: '',
    });
  }

  private resetNewTaskState(): void {
    this.newTaskFormContext.set(null);
    this.newTaskTitle.set('');
    this.newTaskTitleError.set(false);
  }

  private resolveTaskId(context: 'manual' | 'timer'): string {
    const form = context === 'manual' ? this.entryForm : this.timerForm;
    return form.controls.taskId.value;
  }
  
  private updateDuration(): void {
    const duration = Math.max(
      0,
      this.calculateDuration(
        this.entryForm.controls.startTime.value,
        this.entryForm.controls.endTime.value,
      ),
    );
    this.entryForm.controls.durationMinutes.setValue(duration, {
      emitEvent: false,
    });
    this.durationPreviewSeconds.set(duration * 60);
  }
  
  private buildEntryFromForm(request: TimeEntryRequest): TimeEntry {
    /*
    note: buildTimeEntryRequestFromForm() now returns durationSeconds not durationMinutes (matches the real request schema), but the local
    TimeEntry model still tracks durationMinutes for display purposes reading straight off the form control here instead of the request
    object, since spreading 'request' would leave durationMinutes missing
    */
    const now = new Date().toISOString().slice(0, 19);

    return {
      id: this.entryForm.controls.id.value || this.createId(),
      projectId: request.projectId,
      taskId: request.taskId ?? '',
      entryType: request.entryType,
      startTime: request.startTime,
      endTime: request.endTime,
      durationSeconds: request.durationSeconds,
      durationMinutes: this.entryForm.controls.durationMinutes.value,
      description: request.description,
      status: 'DRAFT',
      isLocked: false,
      createdAt: now,
      updatedAt: now,
    };
  }

  buildTimeEntryRequestFromForm(): TimeEntryRequest {
    const selectedDate = this.filterForm.controls.from.value || this.today();
    const durationMinutes = this.entryForm.controls.durationMinutes.value;

    return {
      projectId: this.entryForm.controls.projectId.value,
      taskId: this.resolveTaskId('manual') || null,
      startTime: this.toDateTimeValue(
        selectedDate,
        this.entryForm.controls.startTime.value,
      ),
      endTime: this.toDateTimeValue(
        selectedDate,
        this.entryForm.controls.endTime.value,
      ),
      durationSeconds: durationMinutes * 60,
      entryType: this.entryForm.controls.entryType.value,
      description:
        this.entryForm.controls.description.value || 'No description provided.',
    };
  }

  private calculateDuration(startTime: string, endTime: string): number {
    return this.timeToMinutes(endTime) - this.timeToMinutes(startTime);
  }

  // Checks overlap conditions by comparing raw mathematical minute counters
  private timesOverlap(
    startA: string,
    endA: string,
    startB: string,
    endB: string,
  ): boolean {
    return (
      this.timeToMinutes(startA) < this.timeToMinutes(endB) &&
      this.timeToMinutes(startB) < this.timeToMinutes(endA)
    );
  }

  // Parses textual time records into flat integer values relative to midnight
  private timeToMinutes(time: string): number {
    const timeValue = this.timeFromDateTime(time);
    const [safeHours, safeMinutes] = timeValue.split(':').map(Number);
    return safeHours * 60 + safeMinutes;
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

  /*
  i removed the authorization header removed from here, it's handled by the shared JWT interceptor in core/services/auth.service.ts. keeping token
  logic in one place avoids drift if the interceptor's token source changes later (e.g. moves off localStorage). X-Workspace-Member-Id
  stays here since the interceptor has no reason to know about it
  */
  private requestOptions(): { headers: HttpHeaders } {
    const headers = new HttpHeaders().set(
      'X-Workspace-Member-Id',
      this.workspaceMemberId,
    );
    return { headers };
  }

  private loadEntries(): void {
    this.http
      .get<TimeEntry[]>(
        `${this.apiBaseUrl}/time-entries/me`,
        this.requestOptions(),
      )
      .subscribe({
        next: (entries) =>
          this.entries.set(
            //same durationMinutes-is-actually-seconds fix as onSaved() in saveEntry() — see that comment for the full explanation
            entries.map((entry) => this.normaliseEntryDuration(entry))
          ),
        error: (error) =>
          this.conflictMessage.set(
            error.error?.message ?? 'Unable to load time entries.',
          ),
      });
  }

  /*
  fetches real projects from GET /api/projects (replaces the old hardcoded mock array) and once loaded, if the entry/timer forms still
  have an empty projectId (their initial state), default them to the first project so the dropdowns aren't sitting on a blank selection
  */
  private loadProjects(): void {
    this.http
      .get<ProjectApiResponse[]>(
        `${this.apiBaseUrl}/projects`,
        this.requestOptions(),
      )
      .subscribe({
        next: (projects) => {
          this.projects.set(projects.map((p) => ({ id: p.id, name: p.name })));
          const firstProjectId = projects[0]?.id ?? '';
          if (!this.entryForm.controls.projectId.value) {
            this.entryForm.controls.projectId.setValue(firstProjectId);
          }
          if (!this.timerForm.controls.projectId.value) {
            this.timerForm.controls.projectId.setValue(firstProjectId);
          }
          this.loadTasksForProject(firstProjectId); //populate tasks for whichever project
        },
        error: (error) =>
          this.conflictMessage.set(
            error.error?.message ?? 'Unable to load projects.',
          ),
      });
  }

  /*
  I decided to deviate from the previous use of the api/tasks/my-tasks GET
  because i think the system to should load every task on the project
  because "assigned to me" just means you're the accountable owner, not that youre the only one allowed to touch it
  so now tasks i fetched per project with the api/tasks/project GET
  */
  private lastLoadedTaskProjectId: string | null = null;
  private loadTasksForProject(projectId: string): void {
    if (!projectId) {
      this.tasks.set([{ id: '', projectId: '', title: 'No task selected' }]);
      return;
    }
    if (projectId === this.lastLoadedTaskProjectId) {
      return;
    }
    this.lastLoadedTaskProjectId = projectId;
    this.http
      .get<TaskApiResponse[]>(
        `${this.apiBaseUrl}/tasks/project/${projectId}`,
        this.requestOptions(),
      )
      .subscribe({
        next: (tasks) => {
          this.tasks.set([
            { id: '', projectId: '', title: 'No task selected' },
            ...tasks.map((t) => ({
              id: t.id,
              projectId: t.projectId,
              title: t.title,
            })),
          ]);
        },
        error: (error) =>
          this.conflictMessage.set(
            error.error?.message ?? 'Unable to load tasks.',
          ),
      });
  }

  private loadActiveTimer(): void {
    this.timerService.getActiveTimer().subscribe({
      next: (response) => this.restoreActiveTimer(response),
      error: (error) => {
        if (error.status !== 204 && error.status !== 200) {
          console.error('[LogtimeComponent] loadActiveTimer failed:', error);
        }
      },
    });
  }
  private restoreActiveTimer(response: ActiveTimerResponse | null): void {
    if (!response?.active) {
      return; //if nothing is runninh, there's nothing to restore here
    }

    const timer: ActiveTimer = {
      id: response.id,
      projectId: response.project.id,
      taskId: response.task?.id ?? null,
      notes: '',
      startedAt: this.parseServerTimestamp(response.startedAt), //was new Date(response.startedAt), but that was failing to parse the string correctly in some browsers, so now we force it into a valid ISO format with parseServerTimestamp()
    };

    this.activeTimer.set(timer);
    this.isTimerPaused.set(response.isPaused ?? false);
    this.timerForm.patchValue(
      { projectId: timer.projectId, taskId: timer.taskId ?? '' },
      { emitEvent: false },
    );

    this.timerForm.disable({ emitEvent: false });
    this.clearTimerInterval();

    if (response.isPaused) {
      this.pausedElapsedSeconds.set(response.elapsedSeconds ?? 0);
      this.elapsedSeconds.set(response.elapsedSeconds ?? 0);
    } else {
      this.elapsedSeconds.set(response.elapsedSeconds ?? 0);
      this.timerIntervalId = setInterval(() => {
        this.elapsedSeconds.set(
          Math.floor((Date.now() - timer.startedAt.getTime()) / 1000),
        );
      }, 1000);
    }
  }

  //helper function to force the string timestamp into a valid ISO format for Date parsing, appending 'Z' if no timezone is present
  private parseServerTimestamp(value: string): Date {
    const hasTimeZone = /Z$|[+-]\d{2}:\d{2}$/.test(value);
    return new Date(hasTimeZone ? value : `${value}Z`); // Append 'Z' if no timezone is present
  }

  /*
  fetches the current period's timesheet so the submit button has something to act on. Uses GET /api/timesheets/me, which returns an
  array, taking the entry matching the current filter period since there's no single "current timesheet" endpoint documented in swagger
  flagging: if the backend later adds a dedicated "current" endpoint, swap this out, this filter-by-date approach is a reasonable stopgap
  not a permanent pattern
  */
  private loadCurrentTimesheet(): void {
    this.http
      .get<Timesheet[]>(
        `${this.apiBaseUrl}/timesheets/me`,
        this.requestOptions(),
      )
      .subscribe({
        next: (timesheets) => {
          const from = this.filterForm.controls.from.value;
          const match = timesheets.find(
            (ts) => ts.periodStart <= from && ts.periodEnd >= from,
          );
          this.currentTimesheet.set(match ?? null);
        },
        error: (error) =>
          console.error(
            '[LogtimeComponent] loadCurrentTimesheet failed:',
            error,
          ),
      });
  }

  private formatDateLabel(dateValue: string): string {
    return new Date(`${dateValue}T00:00:00`).toLocaleDateString([], {
      weekday: 'short',
      month: 'short',
      day: 'numeric',
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

  /*
  sonarqube was complaining about the duplicated lines as a maintenance risk, so i factored them out
  into a single helper function to reduce duplication and make it easier to change the logic in one place if needed
  */
 private resetTimerState(toastMessage: string): void {
    this.activeTimer.set(null);
    this.elapsedSeconds.set(0);
    this.isTimerPaused.set(false);
    this.pausedElapsedSeconds.set(0);
    this.clearTimerInterval();
    this.timerForm.enable({ emitEvent: false });
    this.timerForm.reset({
      projectId: this.projects()[0]?.id ?? '',
      taskId: '',
      description: '',
    });
    this.toastMessage.set(toastMessage);
  }

  private normaliseEntryDuration<T extends { durationMinutes: number; durationSeconds: number }>(
    entry: T,
  ): T & { durationSeconds: number}{
    return {
      ...entry,
      durationSeconds: entry.durationMinutes, 
      durationMinutes: Math.round(entry.durationMinutes / 60),
    };
  }
}
