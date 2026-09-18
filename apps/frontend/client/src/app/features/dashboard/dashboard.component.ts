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

  readonly isManagerView = computed(() =>
  (this.auth.currentUser()?.roles ?? []).some((role) =>
  ['MANAGER', 'ROLE_MANAGER'].includes(role)),
  );

  readonly firstName = computed(
    () => this.auth.currentUser()?.firstName || 'there',
  );

  readonly greeting = computed(() => {
    const h = new Date().getHours();
    return h < 12 ? 'Good morning' : h < 18 ? 'Good afternoon' : 'Good evening';
  });

  readonly activeProjects = computed(() =>
  this.projects().filter(
    (p) => 
      p.status === 'ACTIVE' &&
      (!this.isManagerView() ||
      p.myRole === 'MANAGER')
  ),);

  readonly inProgressTaskCount = computed(
    () =>
      this.tasks().filter(
      (t) => t.status === 'IN_PROGRESS' || t.status === 'TODO').length);
  
  readonly inProgressTasks = computed(() =>
  this.tasks().filter((t) => t.status === 'IN_PROGRESS' || t.status === 'TODO').slice(0,3));


  readonly todayEvents = computed(() =>
  this.calendarEvents().filter(
    (e) => this.dateKey(new Date(e.start)) === this.dateKey(new Date()),).slice(0, 4)
  );

  readonly deadlineWindowDays = computed(() => 7);

  readonly upcomingDeadlines = computed(() => {
    const today = this.startOfDay(new Date());
    const end = new Date(today);
    end.setDate(today.getDate() + this.deadlineWindowDays());
    const taskDeadlines = this.tasks()
    .filter(
      (t) => t.dueDate && new Date(t.dueDate) >= today && new Date(t.dueDate) <= end && t.status !== 'DONE',
    )
    .map((t) => ({
      id: `task-${t.id}`,
      targetId: t.id,
      projectId: t.projectId,
      type: t.title,
      projectName: t.projectName || 'Project task',
      dueDate: t.dueDate!,
    }))
    const projectDeadlines = this.activeProjects()
    .filter(
      (p) =>
        p.endDate &&
        new Date(p.endDate) >= today &&
        new Date(p.endDate) <= end,
    )
    .map((p) => ({
      id: `project-${p.id}`,
      targetId: p.id,
      type: 'project' as const,
      title: `${p.name} project due`,
      projectName: 'Project deadline',
      dueDate: p.endDate!
    }));
    return [...taskDeadlines, ...projectDeadlines]
    .sort((a, b) => +new Date(a.dueDate) - +new Date(b.dueDate))
    .slice(0, 4);
  });

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