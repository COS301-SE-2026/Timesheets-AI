import { DatePipe } from '@angular/common';
import { Component, OnDestroy, OnInit, computed, inject, signal, } from '@angular/core';
import { RouterLink } from '@angular/router';
import { catchError, finalize, forkJoin, of } from 'rxjs';
import { AuthService } from '../../core/services/auth.service';
import { NotificationService } from '../../core/services/notification.service';
import { ProjectResponse, ProjectService, } from '../../core/services/project.service';
import { TaskResponse, TaskService } from '../../core/services/task.service';
import { ActiveTimerResponse, TimerService } from '../../core/services/timer.service';
import { TimeEntryResponse, TimeEntryService, } from '../../core/services/time-entry.service';
import { TimesheetResponse,TimesheetService } from '../../core/services/timesheet.service';
import { AppEvent } from '../calendar/calendar.model';
import { CalendarService } from '../calendar/calendar.services';
import { NotificationPanelComponent } from '../notifications/notification-panel.component';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [ NotificationPanelComponent, DatePipe, RouterLink],
  templateUrl: './dashboard.component.html',
  styleUrl: './dashboard.component.scss'
})
export class DashboardComponent implements OnInit {

  private auth = inject(AuthService);
  private readonly notificationService = inject(NotificationService);
  private timers = inject(TimerService);
  private projectsApi = inject(ProjectService);
  private tasksApi = inject(TaskService);
  private timesheetsApi = inject(TimesheetService);
  private calendarApi = inject(CalendarService);
  public readonly showNotifications = signal<boolean>(false);
  public readonly unreadCount = this.notificationService.unreadCount;
  readonly isLoading = signal(true);
  readonly showTimerStoppedModal = signal(false);
  readonly activeTimer = signal<ActiveTimerResponse | null>(null);
  readonly projects = signal<ProjectResponse[]>([]);
  readonly tasks = signal<TaskResponse[]>([]);
  readonly calendarEvents = signal<AppEvent[]>([]);
  readonly pendingTimesheets = signal<TimesheetResponse[]>([]);
  readonly todayMinutes = signal(0);
  readonly weekMinutes = signal(0);
  private tick = signal(0);
  private ticker = window.setInterval(
    () => this.tick.update((v) => v + 1), 1000);



  public ngOnInit(): void {
    //this will load the number displayed on the notification bell
    this.notificationService.loadUnreadCount();
  }

  public toggleNotifications(): void {
    this.showNotifications.update(show => !show);
  }

  public closeNotifications(): void {
    this.showNotifications.set(false);
  }
}