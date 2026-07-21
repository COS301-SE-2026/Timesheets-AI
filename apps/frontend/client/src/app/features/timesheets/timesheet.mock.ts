/**
 * Author : Lerato Sibanda
 * Date : 2026-07-21
 * Related requirement : -
 */

//Mock data for timesheet page UI

export type TimesheetStatus = 'DRAFT' | 'SUBMITTED' | 'APPROVED' | 'REJECTED';

export interface WeekDay {
  label: string;
  shortLabel: string;
  dateLabel: string;
  isoDate: string;
}

export interface TimesheetSummary {
  id: string;
  weekNumber: number;
  periodStart: string;
  periodEnd: string;
  periodLabel: string;
  status: TimesheetStatus;
  submittedAt: string | null;
  approvedAt: string | null;
  rejectedAt: string | null;
  rejectionReason: string | null;
  isLocked: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface TaskRow {
  taskId: string;
  title: string;
  project: string;
  category: string;
  colorCode: string;
  dailyHours: (string | null)[];
  total: string;
}

export interface TimesheetWeekView {
  summary: TimesheetSummary;
  days: WeekDay[];
  task: TaskRow[];
  dailyTotals: string[];
  grandTotal: string;
}

export const STATUS_FILTERS: Array<'ALL' | TimesheetStatus> = [
  'ALL',
  'DRAFT',
  'SUBMITTED',
  'APPROVED',
  'REJECTED'
];

const WEEK_29_DAYS: WeekDay[] = [
  { label:'MON', shortLabel: 'Mon', dateLabel: 'JUL 13', isoDate: '2026-07-13'},
  { label:'TUE', shortLabel: 'Tue', dateLabel: 'JUL 14', isoDate: '2026-07-14'},
  { label:'WED', shortLabel: 'Wed', dateLabel: 'JUL 15', isoDate: '2026-07-15'},
  { label:'THU', shortLabel: 'Thu', dateLabel: 'JUL 16', isoDate: '2026-07-16'},
  { label:'FRI', shortLabel: 'Fri', dateLabel: 'JUL 17', isoDate: '2026-07-17'},
  { label:'SAT', shortLabel: 'Sat', dateLabel: 'JUL 18', isoDate: '2026-07-18'},
  { label:'SUN', shortLabel: 'Sun', dateLabel: 'JUL 19', isoDate: '2026-07-19'}

];

