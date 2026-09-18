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
  imports: [
    NotificationPanelComponent
  ],
  templateUrl: './dashboard.component.html',
  styleUrl: './dashboard.component.scss'
})
export class DashboardComponent implements OnInit {

  private readonly notificationService = inject(NotificationService);

  public readonly showNotifications = signal<boolean>(false);

  public readonly unreadCount = this.notificationService.unreadCount;

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