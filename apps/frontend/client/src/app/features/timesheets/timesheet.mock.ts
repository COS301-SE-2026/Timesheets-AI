export interface Day {
  label: string;
  dateStr: string;
  isToday?: boolean;
}

export interface Task {
  id: string;
  title: string;
  project: string;
  category: string;
  iconClass: string;
  colorCode: string;
  loggedHours: string[];
  total: string;
}

/* ============================================================
   DAYS 
   ============================================================ */
const dayLabels = [
  'Mon, May 18',
  'Tue, May 19',
  'Wed, May 20',
  'Thu, May 21',
  'Fri, May 22',
  'Sat, May 23',
  'Sun, May 24'
];

export const DAYS: Day[] = dayLabels.map((label, index) => ({
  label,
  dateStr: String(18 + index),
  isToday: index === 3
}));

/* ============================================================
   SUMMARY DATA
   ============================================================ */
export const DAILY_TOTALS: string[] = [
  '5hr 30 m',
  '9hr 30 m',
  '6hr 12 m',
  '9hr 7 m',
  '8hr 25min',
  '4hr 8min',
  '12hr 56min'
];

export const GRAND_TOTAL = '55hr 48 m';

/* ============================================================
   TASK FACTORY 
   ============================================================ */
const createTask = (
  id: string,
  title: string,
  project: string,
  category: string,
  iconClass: string,
  colorCode: string,
  loggedHours: string[],
  total: string
): Task => ({
  id,
  title,
  project,
  category,
  iconClass,
  colorCode,
  loggedHours,
  total
});

/* ============================================================
   TASKS
   ============================================================ */
export const TASKS: Task[] = [
  createTask(
    'FE-101',
    'Build Login & Signup Screens',
    'Client Portal',
    'Software Development',
    'fa-solid fa-code',
    '#7C8CF8',
    ['2hr 15m', '3hr 40m', '1hr 20m', '-', '1hr 20m', '-', '-'],
    '7hr 15m'
  ),
  createTask(
    'FE-118',
    'Develop Dashboard Components',
    'Analytics Dashboard',
    'Software Development',
    'fa-solid fa-laptop-code',
    '#F59E9E',
    ['-', '2hr 30m', '4hr 10m', '1hr 45m', '-', '1hr 20m', '-'],
    '8hr 25m'
  ),
  createTask(
    'BE-204',
    'Create Authentication API',
    'Client Portal',
    'Software Development',
    'fa-solid fa-server',
    '#F7C66F',
    ['1hr 10m', '-', '2hr 00m', '2hr 20m', '-', '-', '1hr 20m'],
    '5hr 30m'
  ),
  createTask(
    'DEVOPS-031',
    'Configure AWS Deployment Pipeline',
    'Cloud Infrastructure',
    'Software Development',
    'fa-solid fa-cloud',
    '#6FD3C4',
    ['3hr 00m', '2hr 45m', '-', '-', '-', '1hr 20m', '-'],
    '5hr 45m'
  ),
  createTask(
    'QA-119',
    'Test Payment Gateway Integration',
    'E-Commerce Platform',
    'Software Development',
    'fa-solid fa-bug',
    '#C8A2FF',
    ['-', '-', '1hr 50m', '2hr 10m', '1hr 30m', '-', '-'],
    '5hr 30m'
  ),
  createTask(
    'PM-056',
    'Sprint Planning & Task Estimation',
    'Internal Management System',
    'Software Development',
    'fa-solid fa-list-check',
    '#89C36B',
    ['1hr 30m', '1hr 45m', '1hr 20m', '2hr 00m', '-', '-', '1hr 20m'],
    '5hr 15m'
  )


];