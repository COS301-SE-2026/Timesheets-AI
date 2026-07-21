/**
 * Author: Lerato Sibanda
 * DAte: 2026-06-20
 * Related Requirement: Timesheet
 */

import { Component, computed, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';

import { MOCK_TIMESHEETS, STATUS_FILTERS, TimesheetStatus, TimesheetSummary, TimesheetWeekView, TaskRow} from './timesheet.mock'

type StatusFilter = 'ALL' | TimesheetStatus;
type UiState = 'idle' | 'laoding' | 'error' | 'empty';

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

readonly task = computed<TaskRow[]>(() => this.selectedWeek()?.task ?? []);
readonly days = computed(() => this.selectedWeek()?.days ?? []);
readonly dailyTotals = computed(() => this.selectedWeek()?.grandTotal ?? '0h 00m');
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
  return this.isManager() && !!s  && s.status === 'SUBMITTED' && !s.isLocked;
});

readonly isReadOnly = computed(() => {
  const s = this.summary();
  if(!s) {
    return true;
  }
  return s.isLocked || s.status === 'SUBMITTED' || s.status === 'APPROVED' ;
});

construct() {
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
}
}