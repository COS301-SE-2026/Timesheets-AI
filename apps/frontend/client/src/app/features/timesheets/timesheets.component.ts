import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HeaderComponent } from '../../shared/components/header/header.component';
import { ProgressBarComponent } from '../../shared/components/progress-bar/progress-bar.component';

import { DAYS, TASKS, DAILY_TOTALS, Day, Task, GRAND_TOTAL } from './timesheet.mock';

@Component({
  selector: 'app-timesheets',
  standalone: true,
  imports: [CommonModule, ProgressBarComponent, HeaderComponent],
  templateUrl: './timesheets.component.html',
  styleUrl: './timesheets.component.scss'
})
export class TimesheetsComponent {

  days: Day[] = DAYS;
  tasks: Task[] = TASKS;
  dailyTotals: string[] = DAILY_TOTALS;
  grandTotal = GRAND_TOTAL;
}