/**
 * Author: Kgaugelo Matsena & Lerato Sibanda
 * Date: 2026-05-19
 * Purpose: Display weekly timesheets overview with task breakdown and totals.
 * Related Requirement: -
 */

import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';

import { MOCK_TIMESHEETS, STATUS_FILTERS, TimesheetStatus, TimesheetSummary, TimesheetWeekView, TaskRow} from './timesheet.mock'

type StatusFilter = 'ALL' | TimesheetStatus;
type UiState = 'idle' | 'loading' | 'error' | 'empty';

@Component({
  selector: 'app-timesheets',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './timesheets.component.html',
  styleUrl: './timesheets.component.scss'
})

export class TimesheetsComponent {
readonly statusFilters = STATUS_FILTERS;

// INTEGRATION : Set from auth/session
// Managers see Approve / Reject when status is submitted

readonly isManager = signal(false);
  
//  INTEGRATION: Replace with GET /api/timesheets/me (or filtered status endpoints)

private readonly allTimesheets = signal<TimesheetWeekView[]>(MOCK_TIMESHEETS);

readonly selectedFilter = signal<StatusFilter>('ALL');
readonly selectedTimesheetId = signal<string>(MOCK_TIMESHEETS[0]?.summary.id ?? '');
readonly uiState = signal<UiState>('idle');
readonly errorMessage = signal<string | null>(null);
readonly toastMessage = signal<string | null>(null);

readonly showRejectDialog = signal(false);
readonly rejectReason = signal('');

readonly showSubmitDialog = signal(false);
readonly showSubmitSuccessDialog = signal(false);

readonly actionPending = signal(false);

readonly filteredTimesheets = computed(() =>{
  const filter = this.selectedFilter();
  const list = this.allTimesheets();
  if(filter === 'ALL') {
    return list;
  }
  return list.filter((ts) => ts.summary.status === filter);
});

readonly selectedWeek = computed<TimesheetWeekView | null> (() => {
const list = this.filteredTimesheets();
if(list.length === 0 ) {
  return null;
}
const match = list.find((ts) => ts.summary.id === this.selectedTimesheetId());
return match ?? list[0];
});

readonly summary = computed<TimesheetSummary | null> (
  () => this.selectedWeek()?.summary ?? null );

readonly tasks = computed<TaskRow[]>(() => this.selectedWeek()?.tasks ?? []);
readonly days = computed(() => this.selectedWeek()?.days ?? []);
readonly dailyTotals = computed(() => this.selectedWeek()?.dailyTotals ?? '0h 00m');
readonly grandTotal = computed<string>(() => this.selectedWeek()?.grandTotal ?? '0h 00m');
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
  return (this.isManager() && !!s  && s.status === 'SUBMITTED');
});

readonly isReadOnly = computed(() => {
  const s = this.summary();
  if(!s) {
    return true;
  }
  return s.isLocked || s.status === 'SUBMITTED' || s.status === 'APPROVED' ;
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

  //Simulated load - remove when wiring HTTP

  queueMicrotask(() => {
    try{
      const list = this.filteredTimesheets();
      if(list.length === 0) {
        this.uiState.set('empty');
        return;
      }
      if(!list.some((ts) => ts.summary.id === this.selectedTimesheetId())) {
        this.selectedTimesheetId.set(list[0].summary.id);
      }
      this.uiState.set('idle');
    } catch {
      this.uiState.set('error');
      this.errorMessage.set('Failed to load timesheets. Please try again.');
    }
  });
}

onFilterChange(filter: StatusFilter): void {
  this.selectedFilter.set(filter);
  // INTEGRATION: GET /api/timesheets/me/status/{status} when filter !== ALL
  //              GET /api/timesheets/me when filter === ALL

  this.loadTimesheets();
}

onWeekChange(timesheetId: string): void {
  this.selectedTimesheetId.set(timesheetId);
  // INTEGRATION: GET /api/timesjeets/{id} + GET /api/timesheets/{id}/entries
}

statusLabel(status: StatusFilter | TimesheetStatus) : string {
  if(status === 'ALL') {
    return 'ALL';
  }
  return status.charAt(0) + status.slice(1).toLowerCase();
}

formatDateTime(value: string | null): string {
  if(!value) {
    return '-';
  }
  const date = new Date(value);
  return date.toLocaleString('en-ZA', {
    month: 'short',
    day: 'numeric',
    year: 'numeric',
    hour: 'numeric',
    minute: '2-digit',
    hour12: true
  })
}

formatDate(value: string | null): string {
  if(!value) {
    return '-';
  }
  const date = new Date(value);
  return date.toLocaleDateString('en-ZA', {
    month:'short',
    day: 'numeric',
    year: 'numeric'
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
  if (!this.canSubmit() || this.actionPending()){
    return;
  }

  this.showSubmitDialog.set(true);
}

closeSubmitDialog(): void {
  if (this.actionPending()){
    return;
  }
  this.showSubmitDialog.set(false);
}

confirmSubmitTimesheet(): void {
  const s = this.summary();

  if(!s || !this.canSubmit() || this.actionPending()) {
    return;
  }
  this.actionPending.set(true);

  // INTEGRATION replace this mock update with: this.timesheetsService.submit(s.id).subscribe({...})

  this.patchLocalStatus(s.id, 'SUBMITTED', {
    submittedAt: new Date().toISOString(),
    isLocked: true
  });

  this.actionPending.set(false);
  this.showSubmitDialog.set(false);
  this.showSubmitSuccessDialog.set(true);
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
  if(!confirmed){
    return;
  }
  this.actionPending.set(true);
  // INTEGRATION: this.timesheetService.approve(s.id).subscribe({...})
  this.patchLocalStatus(s.id, 'APPROVED', {
    approvedAt: new Date().toISOString(), isLocked: true
  });
  this.actionPending.set(false);
  this.showToast('Timesheet Approved');
}

openRejectDialog(): void {
  if(!this.canApproveOrReject()) {
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
  this.patchLocalStatus(s.id, 'REJECTED', { rejectedAt: new Date().toISOString(), rejectionReason: reason, isLocked: false});
  this.actionPending.set(false);
  this.closeRejectDialog();
  this.showToast('Timesheet Rejected.');
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

private patchLocalStatus( id: string, status: TimesheetStatus, extras: Partial<TimesheetSummary>) : void {
    this.allTimesheets.update((list) =>
    list.map((week) => {
      if (week.summary.id !== id) {
        return week;
      }
      return {
        ...week, summary: {
          ...week.summary, status, ...extras, updatedAt: new Date().toISOString()
        }
      };
    })
    );
  }

}