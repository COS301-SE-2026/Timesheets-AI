import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HeaderComponent } from '../../shared/components/header/header.component';
import { ProgressBarComponent } from '../../shared/components/progress-bar/progress-bar.component';

/* ============================================================
   TYPES
   ============================================================ */
interface Day {
  label: string;
  dateStr: string;
  isToday?: boolean;
}

interface Task {
  id: string;
  title: string;
  project: string;
  category: string;
  iconClass: string;
  colorCode: string;
  loggedHours: string[];
  total: string;
}

@Component({
  selector: 'app-timesheets',
  standalone: true,
  imports: [CommonModule, ProgressBarComponent, HeaderComponent],
  templateUrl: './timesheets.component.html',
  styleUrl: './timesheets.component.scss'
})
export class TimesheetsComponent {

  /* ============================================================
     DAYS 
     ============================================================ */
  private readonly dayLabels = [
    'Mon, May 18',
    'Tue, May 19',
    'Wed, May 20',
    'Thu, May 21',
    'Fri, May 22',
    'Sat, May 23',
    'Sun, May 24'
  ];

  days: Day[] = this.dayLabels.map((label, index) => ({
    label,
    dateStr: `${18 + index}`,
    isToday: index === 3
  }));

  /* ============================================================
     SUMMARY DATA
     ============================================================ */
  dailyTotals: string[] = [
    '5hr 30 m',
    '9hr 30 m',
    '6hr 12 m',
    '9hr 7 m',
    '8hr 25min',
    '4hr 8min',
    '12hr 56min'
  ];

  grandTotal = '55hr 48 m';

  /* ============================================================
     TASKS 
     ============================================================ */
  tasks: Task[] = [
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
    },
    {
      id: 'FE-145',
      title: 'Optimize Responsive Mobile Layout',
      project: 'Marketing Website',
      category: 'Software Development',
      iconClass: 'fa-solid fa-mobile-screen',
      colorCode: '#FFB86B',
      loggedHours: ['-', '1hr 20m', '2hr 10m', '-', '1hr 40m', '1hr 20m', '-'],
      total: '5hr 10m'
    }
  ];
}