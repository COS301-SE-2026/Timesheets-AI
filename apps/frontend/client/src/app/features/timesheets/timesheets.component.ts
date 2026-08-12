/**
 * Author: Kgaugelo Matsena & Lerato Sibanda
 * Date: 2026-05-19
 * Purpose: Weekly timesheets + mnager review/approve/reject flow
 * Related Requirement: UC3, UC7
 *
 * Patched: Zamokuhle Zwane, 25/07/2026
 * Patched: Zamokuhle Zwane, 03/08/2026
 * i fixed errors with the total duration and daily totals, they were showing up as 0hr 0m even when there were entries
 * I fixed the logic to calculate the totals correctly
 * Patched: Lerato Sibanda, 12/08.2026 - manager review Timesheets tab + modal
 */

import { Component, computed, inject, signal } from '@angular/core';
import {
//   TimeEntryService,
//   TimeEntryResponse,
// } from '../../core/services/time-entry.service';
// import {
  TimesheetService,
  TimesheetResponse,
} from '../../core/services/timesheet.service';
import {
  ProjectService,
  ProjectMemberInfo,
  ProjectResponse,
} from '../../core/services/project.service';
import { TaskService, TaskResponse } from '../../core/services/task.service';
import { AuthService } from '../../core/services/auth.service';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import {forkJoin, Observable, of, catchError, tap, map, switchMap } from 'rxjs';
import { TimeEntryResponse } from '../../core/services/time-entry.service';

type StatusFilter = 'ALL' | TimesheetStatus;
type ReviewStatusFilter = 'ALL' | 'SUBMITTED' | 'APPROVED' | 'REJECTED';
type PageTab = 'mine' | 'review';
type UiState = 'idle' | 'loading' | 'error' | 'empty';
type TimesheetStatus = TimesheetResponse['status'];

//types
export interface Day {
  label: string; //Mon, Jul 21
  shortLabel: string;
  dateStr: string;
  isToday?: boolean;
}
interface TimesheetSummary {
  id: string;
  workspaceMemberId: string;
  status: TimesheetStatus;
  isLocked: boolean;
  periodStart: string;
  periodEnd: string;
  weekNumber: number;
  periodLabel: string; //e.g. "Jul 21 - Jul 27"
  submittedAt: string | null;
  approvedAt: string | null;
  rejectedAt: string | null;
  rejectionReason: string | null;
  updatedAt: string;
}

interface TimesheetWeekView {
  summary: TimesheetSummary;
  days: Day[];
  tasks: TaskRow[];
  dailyTotals: string[];
  grandTotal: string;
  grandTptalShort: string;
}

interface TaskRow {
  id: string;
  title: string;
  project: string;
  iconClass: string;
  colorCode: string;
  loggedHours: string[]; //7 entries
  loggedHoursShort: string[];
  total: string;
  totalShort: string;
}

interface MemberInfo {
  name: string;
  role: string;
  initials: string;
  avatarColor: string;
}

interface ReviewRow {
  summary: TimesheetSummary;
  employeeName: string;
  employeeRole: string;
  initials: string;
  avatarColor: string;
  totalHours: string;
  days: Day[];
  tasks: TaskRow[];
  dailyTotals: string[];
  grandTotalShort: string;
}

const TASK_STYLE_PALETTE: { iconClass: string; colorCode: string }[] = [
  { iconClass: 'fa-solid fa-code', colorCode: '#7C8CF8' },
  { iconClass: 'fa-solid fa-laptop-code', colorCode: '#F59E9E' },
  { iconClass: 'fa-solid fa-server', colorCode: '#F7C66F' },
  { iconClass: 'fa-solid fa-bug', colorCode: '#8CD6C0' },
  { iconClass: 'fa-solid fa-chart-simple', colorCode: '#B78CF8' },
  { iconClass: 'fa-solid fa-gear', colorCode: '#6BA5E7' },
];

//tiny string hash, good enough for picking a stable palette index
function hashTaskId(id: string): number {
  let hash = 0;
  for (let i = 0; i < id.length; i++) {
    hash = (hash << 5) - hash + id.codePointAt(i)!;
    hash = Math.trunc(hash); // keep it a 32 bit int
  }
  return Math.abs(hash);
}

@Component({
  selector: 'app-timesheets',
  standalone: true,
  imports: [FormsModule, RouterLink],
  templateUrl: './timesheets.component.html',
  styleUrl: './timesheets.component.scss',
})
export class TimesheetsComponent {
  private readonly timeEntryService = inject(TimeEntryService);
  private readonly timesheetService = inject(TimesheetService);
  private readonly projectService = inject(ProjectService);
  private readonly taskService = inject(TaskService);

  // INTEGRATION : Set from auth/session
  // Managers see Approve / Reject when status is submitted

  private readonly authService = inject(AuthService);
  readonly isManager = computed(
    () => this.authService.currentUser()?.roles?.includes('MANAGER') ?? false,
  );

  //  INTEGRATION: Replace with GET /api/timesheets/me (or filtered status endpoints)
  private readonly rawProjects = signal<ProjectResponse[]>([]);
  private readonly rawTasks = signal<TaskResponse[]>([]);

  private readonly allTimesheets = signal<TimesheetWeekView[]>([]);

  readonly statusFilters: StatusFilter[] = [
    'ALL',
    'DRAFT',
    'SUBMITTED',
    'APPROVED',
    'REJECTED',
  ];
  readonly selectedFilter = signal<StatusFilter>('ALL');
  readonly selectedTimesheetId = signal<string>('');
  readonly uiState = signal<UiState>('idle');
  readonly errorMessage = signal<string | null>(null);
  readonly toastMessage = signal<string | null>(null);

  readonly showRejectDialog = signal(false);
  readonly rejectReason = signal('');

  readonly showSubmitDialog = signal(false);
  readonly showSubmitSuccessDialog = signal(false);

  readonly actionPending = signal(false);

  readonly filteredTimesheets = computed(() => {
    const filter = this.selectedFilter();
    const list = this.allTimesheets();
    if (filter === 'ALL') {
      return list;
    }
    return list.filter((ts) => ts.summary.status === filter);
  });

  readonly selectedWeek = computed<TimesheetWeekView | null>(() => {
    const list = this.filteredTimesheets();
    if (list.length === 0) {
      return null;
    }
    const match = list.find(
      (ts) => ts.summary.id === this.selectedTimesheetId(),
    );
    return match ?? list[0];
  });

  readonly summary = computed<TimesheetSummary | null>(
    () => this.selectedWeek()?.summary ?? null,
  );

  readonly tasks = computed<TaskRow[]>(() => this.selectedWeek()?.tasks ?? []);
  readonly days = computed(() => this.selectedWeek()?.days ?? []);
  readonly dailyTotals = computed<string[]>(
    () => this.selectedWeek()?.dailyTotals ?? [],
  );
  readonly grandTotal = computed<string>(
    () => this.selectedWeek()?.grandTotal ?? '0h 00m',
  );
  readonly hasEntries = computed(() => this.tasks().length > 0);

  readonly weekPickerLabel = computed(() => {
    const s = this.summary();
    if (!s) {
      return 'Select week';
    }
    return `Week ${s.weekNumber} . ${s.periodLabel}`;
  });

  readonly canSubmit = computed(() => {
    const s = this.summary();
    return !!s && s.status === 'DRAFT' && !s.isLocked;
  });

  readonly canApproveOrReject = computed(() => {
    const s = this.summary();
    return this.isManager() && !!s && s.status === 'SUBMITTED';
  });

  readonly isReadOnly = computed(() => {
    const s = this.summary();
    if (!s) {
      return true;
    }
    return s.isLocked || s.status === 'SUBMITTED' || s.status === 'APPROVED';
  });

  constructor() {
    // INTEGRATION: Call loadTimesheets() on init once timesheetService exist.
    this.loadTimesheets();
  }

  // INTEGRATION: Replcae body with:
  // this.uiState.set('loading');
  // this.timesheetService.getMyTimesheets(this.selectedFilter()).subscribe
  // Then for the selected id, GET /api/timesheets/{id}/entries and map entries intp task rows (resolve project/task name via project & task APIs)

  loadTimesheets(): void {
    this.uiState.set('loading');
    this.errorMessage.set(null);

    const filter = this.selectedFilter();
    const timesheets$ =
      filter === 'ALL'
        ? this.timesheetService.getMyTimesheets()
        : this.timesheetService.getMyTimesheetsByStatus(filter);

    forkJoin({
      timesheets: timesheets$,
      projects: this.projectService.getProjects(),
      tasks: this.taskService.getMyTasks(),
    }).subscribe({
      next: ({ timesheets, projects, tasks }) => {
        this.rawProjects.set(projects);
        this.rawTasks.set(tasks);

        //build lightweight summaries only, no entries yet, it keeps this to a 3
        //request total instead of n+1 per timesheet

        const summaries = timesheets.map((ts) => this.toSummary(ts)).sort((a,b) => a.periodStart.localeCompare(b.periodStart));
        this.allTimesheets.set(
          summaries.map((summary) => ({
            summary,
            days: this.buildWeekDays(summary.periodStart),
            tasks: [],
            dailyTotals: [],
            grandTotal: '0hr 0m',
          })),
        );

        if (summaries.length === 0) {
          this.uiState.set('empty');
          return;
        }

        const todayStr = this.toIsoDate(new Date());
        const currentWeek = summaries.find(
          (s) => s.periodStart <= todayStr && s.periodEnd >= todayStr,
        );

        const targetId = summaries.some(
          (s) => s.id === this.selectedTimesheetId(),
        )
          ? this.selectedTimesheetId()
          : (currentWeek ?? summaries[0]).id;

        this.selectedTimesheetId.set(targetId);
        this.loadEntriesForWeek(targetId); //this will fetch just the selected weeks entries
        this.uiState.set('idle');
      },
      error: () => {
        this.uiState.set('error');
        this.errorMessage.set('Failed to load timesheets. Please try again');
      },
    });
  }

  private loadEntriesForWeek(timesheetId: string): void {
    this.timesheetService.getEntriesForTimesheet(timesheetId).subscribe({
      next: (entries) => {
        //resolve any taskIds missing because it automatically says "Unknown tasks" silently
        this.resolveMissingTasks(entries).subscribe(() => {
          this.allTimesheets.update((list) =>
            list.map((week) => {
              if (week.summary.id !== timesheetId) return week;
              const { tasks, dailyTotals, grandTotal } = this.buildTaskRows(
                entries,
                week.days,
              );
              return { ...week, tasks, dailyTotals, grandTotal };
            }),
          );
        });
      },
      error: () =>
        this.errorMessage.set('Unable to load entries for this week'),
    });
  }

  //helper function to fetch any missing tasks that are referenced by entries but not in the rawTasks list

  private resolveMissingTasks(entries: TimeEntryResponse[]): Observable<unknown> {
    const knownIds = new Set(this.rawTasks().map((t) => t.id));
    const missingIds = Array.from(
      new Set(
        entries
        .map((e) => e.taskId)
        .filter((id): id is string => !!id&& !knownIds.has(id)),
      ),
    );

    if(missingIds.length === 0) {
      return of(null); //nothing missing skip the trip entirely
    }
    return forkJoin(
      missingIds.map((id) =>
        this.taskService.getTaskById(id).pipe(
          catchError((err) => {
            console.error(
            `[TimesheetsComponent] getTaskById failed for missing task ${id}: will display as "Unknown task"`, err,
          );
          return of(null); 
        }),
        ),
      ),
    ).pipe(
      tap((results) => {
        const resolved = results.filter((t): t is TaskResponse => !!t);
        if(resolved.length > 0){
          this.rawTasks.update((list) => [...list, ...resolved]);
        }
      }),
    );
  }

  private buildTaskRows(
    entries: TimeEntryResponse[],
    days: Day[],
  ): { tasks: TaskRow[]; dailyTotals: string[]; grandTotal: string } {
    const tasksById = new Map(this.rawTasks().map((t) => [t.id, t]));
    const projectById = new Map(this.rawProjects().map((p) => [p.id, p.name]));
    const secondsByGroup = new Map<string, number[]>();
    const dayTotals = new Array(days.length).fill(0);
    console.log('[TimesheetsComponent] buildTaskRows received', entries.length, 'entries');
    
    const NO_TASK_KEY = '__no_task__';
   
    for (const entry of entries) {
      const dayIndex = days.findIndex(
        (d) => d.dateStr === entry.startTime.slice(0, 10),
      );
      if (dayIndex == -1){
        console.log('[TimesheetsComponent] skipping entry, no matching day:', {
          entryStartTime: entry.startTime,
          expectedDates: days.map(d => d.dateStr),
        });
        continue;
      }
      const groupKey = entry.taskId? entry.taskId: `${NO_TASK_KEY}:${entry.projectId}`;

      const seconds = entry.durationMinutes;
      if (!secondsByGroup.has(groupKey)) {
        secondsByGroup.set(groupKey, new Array(days.length).fill(0));
      }
      secondsByGroup.get(groupKey)![dayIndex] += seconds;
      dayTotals[dayIndex] += seconds;
    }
    const tasks = Array.from(secondsByGroup.entries()).map(
      ([groupKey, secondsPerDay]) => {
        const isNoTask = groupKey.startsWith(`${NO_TASK_KEY}:`);
        const totalSecondsForGroup = secondsPerDay.reduce((a, b) => a + b, 0);
        const style =
            TASK_STYLE_PALETTE[hashTaskId(groupKey) % TASK_STYLE_PALETTE.length];

        if(isNoTask){
          const projectId = groupKey.slice(NO_TASK_KEY.length+1);
          return {
            id: groupKey,
            title: 'No task',
            project: projectById.get(projectId) ?? 'Unknown project',
            iconClass: style.iconClass,
            colorCode: style.colorCode,
            loggedHours: secondsPerDay.map((s) =>
              s > 0 ? this.formatDuration(s) : '-',
            ),
            total: this.formatDuration(totalSecondsForGroup),
        };
      }

      const taskId = groupKey;
      const task = tasksById.get(taskId);
      return {
        id: taskId,
        title: task?.title ?? 'Unknown task',
        project: projectById.get(task?.projectId ?? '') ?? 'Unknown project',
        iconClass: style.iconClass,
        colorCode: style.colorCode,
        loggedHours: secondsPerDay.map((s) =>
          s > 0 ? this.formatDuration(s) : '-',
        ),
        total: this.formatDuration(totalSecondsForGroup),
      };
    },
    );

    return {
      tasks,
      dailyTotals: dayTotals.map((s) => (s > 0 ? this.formatDuration(s) : '-')),
      grandTotal: this.formatDuration(dayTotals.reduce((a, b) => a + b, 0)),
    };
  }

  private toSummary(ts: TimesheetResponse): TimesheetSummary {
    const start = new Date(ts.periodStart);
    return {
      id: ts.id,
      status: ts.status,
      isLocked: ts.isLocked,
      periodStart: ts.periodStart,
      periodEnd: ts.periodEnd,
      weekNumber: this.getIsoWeekNumber(start),
      periodLabel: `${this.formatDate(ts.periodStart)} - ${this.formatDate(ts.periodEnd)}`,
      submittedAt: ts.submittedAt,
      approvedAt: ts.approvedAt,
      rejectedAt: ts.rejectedAt,
      rejectionReason: ts.rejectionReason,
      updatedAt: ts.updatedAt,
    };
  }

  //ISO 8601 week number, reference:  https://stackoverflow.com/questions/6117814/get-week-of-year-in-javascript-like-in-php
  private getIsoWeekNumber(date: Date): number {
    const d = new Date(
      Date.UTC(date.getFullYear(), date.getMonth(), date.getDate()),
    );
    d.setUTCDate(d.getUTCDate() + 4 - (d.getUTCDay() || 7));
    const yearStart = new Date(Date.UTC(d.getUTCFullYear(), 0, 1));
    return Math.ceil(((d.getTime() - yearStart.getTime()) / 86400000 + 1) / 7);
  }

  private buildWeekDays(periodStart: string): Day[] {
    //Noticed a bug in the weekdays that means entries logged on Log time werent showing up on the matching day column here
    //i think they date values were off by one
    const [year, month, day] = periodStart.split('-').map(Number);
    const todayStr = this.toIsoDate(new Date());
    return Array.from({ length: 7 }, (_, i) => {
      const date = new Date(year, month - 1, day + i); //local time throughout, instead of UTC
      const dateStr = this.toIsoDate(date);
      return {
        label: date.toLocaleDateString([], {
          weekday: 'short',
          month: 'short',
          day: 'numeric',
        }),
        dateStr,
        isToday: dateStr === todayStr,
      };
    });
  }
  //helper function to format a date to be a YYYY-MM-DD without the toISOString()'s UTC
  private toIsoDate(date: Date): string {
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    return `${year}-${month}-${day}`;
  }

  private formatDuration(totalSeconds: number): string {
    if (totalSeconds <= 0) return '-';
    const hours = Math.floor(totalSeconds / 3600);
    const mins = Math.floor((totalSeconds % 3600) / 60);
    const seconds = Math.floor(totalSeconds % 60);
    if(hours > 0) return seconds > 0 ? `${hours}hr ${mins}m ${seconds}s` : `${hours}hr ${mins}m`;
    if(mins > 0) return seconds > 0 ? `${mins}m ${seconds}s`: `${mins}m`;
    return `${seconds}s`;
  }

  onFilterChange(filter: StatusFilter): void {
    this.selectedFilter.set(filter);
    // INTEGRATION: GET /api/timesheets/me/status/{status} when filter !== ALL
    //              GET /api/timesheets/me when filter === ALL

    this.loadTimesheets();
  }

  onWeekChange(timesheetId: string): void {
    this.selectedTimesheetId.set(timesheetId);
    this.loadEntriesForWeek(timesheetId);
  }

  statusLabel(status: StatusFilter | TimesheetStatus): string {
    if (status === 'ALL') {
      return 'ALL';
    }
    return status.charAt(0) + status.slice(1).toLowerCase();
  }

  formatDateTime(value: string | null): string {
    if (!value) {
      return '-';
    }
    const date = new Date(value);
    return date.toLocaleString('en-ZA', {
      month: 'short',
      day: 'numeric',
      year: 'numeric',
      hour: 'numeric',
      minute: '2-digit',
      hour12: true,
    });
  }

  formatDate(value: string | null): string {
    if (!value) {
      return '-';
    }
    const date = new Date(value);
    return date.toLocaleDateString('en-ZA', {
      month: 'short',
      day: 'numeric',
      year: 'numeric',
    });
  }

  displayHours(value: string | null): string {
    return value ?? '-';
  }

  // INTEGRATION: Navigate to task/entry detail or open a side panel listiing entries for this task withing the selected timesheet period.

  onViewTask(task: TaskRow): void {
    this.showToast(`View entries for "${task.title}" (wire up navigation).`);
  }

  // INTEGRATION: Conftim, then POST /api/timesheets/{id}/submit

  openSubmitDialog(): void {
    if (!this.canSubmit() || this.actionPending()) {
      return;
    }

    this.showSubmitDialog.set(true);
  }

  closeSubmitDialog(): void {
    if (this.actionPending()) {
      return;
    }
    this.showSubmitDialog.set(false);
  }

  confirmSubmitTimesheet(): void {
    const s = this.summary();

    if (!s || !this.canSubmit() || this.actionPending()) {
      return;
    }
    this.actionPending.set(true);

    // INTEGRATION replace this mock update with: this.timesheetsService.submit(s.id).subscribe({...})

    this.actionPending.set(true);
    this.timesheetService.submitTimesheet(s.id).subscribe({
      next: (updated) => {
        this.patchLocalStatus(s.id, updated.status, {
          submittedAt: updated.submittedAt,
          isLocked: updated.isLocked,
        });

        this.actionPending.set(false);
        this.showSubmitDialog.set(false);
        this.showSubmitSuccessDialog.set(true);
      },
      error: () => {
        this.actionPending.set(false);
        this.errorMessage.set('Unable to submit the timesheet.');
      },
    });
  }

  closeSubmissionSuccessDialog(): void {
    this.showSubmitSuccessDialog.set(false);
  }

  // INTEGRATION: Confirm then POST /api/timesheets/{id}/approve

  onApproveTimesheet(): void {
    const s = this.summary();
    if (!s || !this.canApproveOrReject()) {
      return;
    }
    const confirmed = window.confirm('Approve this timesheet?');
    if (!confirmed) {
      return;
    }
    this.actionPending.set(true);
    // INTEGRATION: this.timesheetService.approve(s.id).subscribe({...})
    this.timesheetService.approveTimesheet(s.id).subscribe({
      next: (updated) => {
        this.patchLocalStatus(s.id, updated.status, {
          approvedAt: updated.approvedAt,
          isLocked: updated.isLocked,
        });
        this.actionPending.set(false);
        this.showToast('Timesheet Approved');
      },
      error: () => {
        this.actionPending.set(false);
        this.errorMessage.set('Unable to approve the timesheet.');
      },
    });
  }

  openRejectDialog(): void {
    if (!this.canApproveOrReject()) {
      return;
    }
    this.rejectReason.set('');
    this.showRejectDialog.set(true);
  }

  closeRejectDialog(): void {
    this.showRejectDialog.set(false);
    this.rejectReason.set('');
  }

  // INTEGRATION POST /api/timesheets/{id} reject with body {reason}

  onConfirmReject(): void {
    const s = this.summary();
    const reason = this.rejectReason().trim();
    if (!s || !reason) {
      return;
    }
    this.actionPending.set(true);
    // INTEGRATIOON: this.timesheetService.reject(s.id, reasons).subscribe({...})
    this.timesheetService.rejectTimesheet(s.id, reason).subscribe({
      next: (updated) => {
        this.patchLocalStatus(s.id, updated.status, {
          rejectedAt: updated.rejectedAt,
          rejectionReason: updated.rejectionReason,
          isLocked: updated.isLocked,
        });
        this.actionPending.set(false);
        this.closeRejectDialog();
        this.showToast('Timesheet Rejected.');
      },
      error: () => {
        this.actionPending.set(false);
        this.errorMessage.set('Unable to reject the timesheet.');
      },
    });
  }

  dismissToast(): void {
    this.toastMessage.set(null);
  }

  private showToast(message: string): void {
    this.toastMessage.set(message);
    window.setTimeout(() => {
      if (this.toastMessage() === message) {
        this.toastMessage.set(null);
      }
    }, 4000);
  }

  // Local mock mutation - remove when API responses update state

  private patchLocalStatus(
    id: string,
    status: TimesheetStatus,
    extras: Partial<TimesheetSummary>,
  ): void {
    this.allTimesheets.update((list) =>
      list.map((week) => {
        if (week.summary.id !== id) {
          return week;
        }
        return {
          ...week,
          summary: {
            ...week.summary,
            status,
            ...extras,
            updatedAt: new Date().toISOString(),
          },
        };
      }),
    );
  }
}