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

const WEEK_28_DAYS: WeekDay[] = [
  { label:'MON', shortLabel: 'Mon', dateLabel: 'JUL 6', isoDate: '2026-07-6'},
  { label:'TUE', shortLabel: 'Tue', dateLabel: 'JUL 7', isoDate: '2026-07-7'},
  { label:'WED', shortLabel: 'Wed', dateLabel: 'JUL 8', isoDate: '2026-07-8'},
  { label:'THU', shortLabel: 'Thu', dateLabel: 'JUL 9', isoDate: '2026-07-9'},
  { label:'FRI', shortLabel: 'Fri', dateLabel: 'JUL 10', isoDate: '2026-07-10'},
  { label:'SAT', shortLabel: 'Sat', dateLabel: 'JUL 11', isoDate: '2026-07-11'},
  { label:'SUN', shortLabel: 'Sun', dateLabel: 'JUL 12', isoDate: '2026-07-12'}

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

// Prior week

export const MOCK_TIMESHEET_WEEK_28: TimesheetWeekView = {
  summary: {
  id: 'ts-week-28',
  weekNumber: 28,
  periodStart: '2026-07-06',
  periodEnd: '2026-07-12',
  periodLabel: 'JUL 6 - JUL 12, 2026',
  status: 'SUBMITTED',
  submittedAt: '2026-07-12T17:30:00',
  approvedAt: null,
  rejectedAt: null,
  rejectionReason: null,
  isLocked: true,
  createdAt: '2026-07-06T00:00:00',
  updatedAt: '2026-07-12T17:30:00'
  },
  days: WEEK_28_DAYS,

  task: [
    {
    taskId: 'task-login',
    title: 'Build login & Signup screens',
    project: 'Client Portal',
    category: 'Software Development',
    colorCode: '#4A5AB5',
    dailyHours: ['4h 00m', '3h 00m', '4h 00m', '3h 30m', '2h 00m', null, null],
    total: '8h 35m'
    },
    {
    taskId: 'task-auth-api',
    title: 'Create Authentication API',
    project: 'Client Portal',
    category: 'Software Development',
    colorCode: '#F0D49A',
    dailyHours: ['1h 10m', null , '2h 00m', '2h 20m', null, null, '1h 20m'],
    total: '6h 50m'
    },
    {
    taskId: 'task-dashboard',
    title: 'Develop Dashboard Components',
    project: 'Analytics Dashboard',
    category: 'Software Development',
    colorCode: '#E07830',
    dailyHours: [ null, '2h 30m' , '4h 10m', '1h 45m', null, '1h 20m', null ],
    total: '9h 45m'
    }
  ],
  dailyTotals: ['3h 55min', '6h 40m', '8h 00m', '4h 35m', '1h 50m', '1h 20m', '1h 20min'],
  grandTotal: '27h 40m'
};

// Current week

export const MOCK_TIMESHEET_WEEK_29: TimesheetWeekView = {
  summary: {
  id: 'ts-week-28',
  weekNumber: 28,
  periodStart: '2026-07-13',
  periodEnd: '2026-07-19',
  periodLabel: 'JUL 13 - JUL 19, 2026',
  status: 'DRAFT',
  submittedAt: null,
  approvedAt: null,
  rejectedAt: null,
  rejectionReason: null,
  isLocked: false,
  createdAt: '2026-07-13T00:00:00',
  updatedAt: '2026-07-17T10:00:00'
  },
  days: WEEK_29_DAYS,

  task: [
    {
    taskId: 'task-login',
    title: 'Build login & Signup screens',
    project: 'Client Portal',
    category: 'Software Development',
    colorCode: '#4A5AB5',
    dailyHours: ['2h 15m', '3h 40m', '1h 20m', null, '1h 20m', null, null],
    total: '7h 35m'
    },
    {
    taskId: 'task-auth-api',
    title: 'Create Authentication API',
    project: 'Client Portal',
    category: 'Software Development',
    colorCode: '#F0D49A',
    dailyHours: [null, '2h 30m' , '4h 10m', '1h 45m', '1h 20m', null, null],
    total: '9h 45m'
    },
    {
    taskId: 'task-dashboard',
    title: 'Develop Dashboard Components',
    project: 'Analytics Dashboard',
    category: 'Software Development',
    colorCode: '#E07830',
    dailyHours: [ '1h 10m' , null , '2h 10m', '1h 45m', '1h 5m', null, null ],
    total: '6h 45m'
    }
  ],
  dailyTotals: ['4h 30min', '3h 30m', '4h 30m', '4h 00m', '2h 30m', '00h 20m', '00h 00min'],
  grandTotal: '27h 40m'
};

export const MOCK_TIMESHEETS: TimesheetWeekView[] = [
  MOCK_TIMESHEET_WEEK_28,
  MOCK_TIMESHEET_WEEK_29
];
