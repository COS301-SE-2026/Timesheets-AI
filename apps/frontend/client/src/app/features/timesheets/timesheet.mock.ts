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

const DAY_LABELS = [
  'Mon, May 18',
  'Tue, May 19',
  'Wed, May 20',
  'Thu, May 21',
  'Fri, May 22',
  'Sat, May 23',
  'Sun, May 24'
];

export const DAYS: Day[] = DAY_LABELS.map((label, index) => ({
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
   TASK DATA
   ============================================================ */

type TaskSeed = Omit<Task, 'total'> & { total: string };

const TASK_SEEDS: TaskSeed[] = [
  {
    id: 'FE-101',
    title: 'Build Login & Signup Screens',
    project: 'Client Portal',
    category: 'Software Development',
    iconClass: 'fa-solid fa-code',
    colorCode: '#7C8CF8',
    loggedHours: ['2hr 15m', '3hr 40m', '1hr 20m', '-', '1hr 20m', '-', '-'],
    total: '7hr 15m'
  },
  {
    id: 'FE-118',
    title: 'Develop Dashboard Components',
    project: 'Analytics Dashboard',
    category: 'Software Development',
    iconClass: 'fa-solid fa-laptop-code',
    colorCode: '#F59E9E',
    loggedHours: ['-', '2hr 30m', '4hr 10m', '1hr 45m', '-', '1hr 20m', '-'],
    total: '8hr 25m'
  },
  {
    id: 'BE-204',
    title: 'Create Authentication API',
    project: 'Client Portal',
    category: 'Software Development',
    iconClass: 'fa-solid fa-server',
    colorCode: '#F7C66F',
    loggedHours: ['1hr 10m', '-', '2hr 00m', '2hr 20m', '-', '-', '1hr 20m'],
    total: '5hr 30m'
  },
  {
    id: 'DEVOPS-031',
    title: 'Configure AWS Deployment Pipeline',
    project: 'Cloud Infrastructure',
    category: 'Software Development',
    iconClass: 'fa-solid fa-cloud',
    colorCode: '#6FD3C4',
    loggedHours: ['3hr 00m', '2hr 45m', '-', '-', '-', '1hr 20m', '-'],
    total: '5hr 45m'
  },
  {
    id: 'QA-119',
    title: 'Test Payment Gateway Integration',
    project: 'E-Commerce Platform',
    category: 'Software Development',
    iconClass: 'fa-solid fa-bug',
    colorCode: '#C8A2FF',
    loggedHours: ['-', '-', '1hr 50m', '2hr 10m', '1hr 30m', '-', '-'],
    total: '5hr 30m'
  },
  {
    id: 'PM-056',
    title: 'Sprint Planning & Task Estimation',
    project: 'Internal Management System',
    category: 'Software Development',
    iconClass: 'fa-solid fa-list-check',
    colorCode: '#89C36B',
    loggedHours: ['1hr 30m', '1hr 45m', '1hr 20m', '2hr 00m', '-', '-', '1hr 20m'],
    total: '5hr 15m'
  }
];

/* ============================================================
   FINAL TASKS EXPORT 
   ============================================================ */

export const TASKS: Task[] = TASK_SEEDS.map(task => ({ ...task }));