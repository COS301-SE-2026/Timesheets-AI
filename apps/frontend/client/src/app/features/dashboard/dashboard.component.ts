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
import { AvailableTeamUser, TeamService } from '../../core/services/team.service';
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
export class DashboardComponent implements OnInit, OnDestroy {

  private auth = inject(AuthService);
  private readonly notificationService = inject(NotificationService);
  private timers = inject(TimerService);
  private projectsApi = inject(ProjectService);
  private tasksApi = inject(TaskService);
  private entriesApi = inject(TimeEntryService);
  private timesheetsApi = inject(TimesheetService);
  private calendarApi = inject(CalendarService);
  private teamApi = inject(TeamService);
  public readonly showNotifications = signal<boolean>(false);
  public readonly unreadCount = this.notificationService.unreadCount;
  readonly isLoading = signal(true);
  readonly showTimerStoppedModal = signal(false);
  readonly activeTimer = signal<ActiveTimerResponse | null>(null);
  readonly projects = signal<ProjectResponse[]>([]);
  readonly tasks = signal<TaskResponse[]>([]);
  readonly calendarEvents = signal<AppEvent[]>([]);
  readonly pendingTimesheets = signal<TimesheetResponse[]>([]);
  readonly availableUsers = signal<AvailableTeamUser[]>([]);
  readonly todayMinutes = signal(0);
  readonly weekMinutes = signal(0);
  private tick = signal(0);
  private ticker = window.setInterval(
    () => this.tick.update((v) => v + 1), 1000);

  readonly isManagerView = computed(() =>
  (this.auth.currentUser()?.roles ?? []).some((role) =>
  ['MANAGER', 'ROLE_MANAGER'].includes(role)),
  );

  readonly isAdminView = computed(() => 
  (this.auth.currentUser()?.roles ?? []).some((role) => 
  ['ADMIN', 'ROLE_ADMIN'].includes(role)),
);

  readonly waitingForWorkspaceCount = computed(() =>
  this.availableUsers().filter((user) => !user.isInWorkspace).length);

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
      (this.isAdminView() || 
      !this.isManagerView() ||
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
      type: 'task' as const,
      title: t.title,
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

  readonly approvalCount = computed(
    () => this.pendingTimesheets().filter(
      (timesheet) => timesheet.status === 'SUBMITTED',
    ).length,
  );

  readonly timerSeconds = computed(() => {
    this.tick();
    const t = this.activeTimer();
    if (!t) return 0;
    const elapsed = t.elapsedSeconds ?? t.elapsedMinutes * 60;
    return t.active && !t.isPaused
    ? elapsed +
    Math.max(0, Math.floor((Date.now() - +new Date(t.startedAt)) / 1000) - elapsed,) : elapsed;
  });

  readonly weekLabel = this.formatWeek(new Date());
  constructor() {
    this.loadDashboard();
  }

  public ngOnInit(): void {
    //this will load the number displayed on the notification bell
    this.notificationService.loadUnreadCount();
  }

  ngOnDestroy(): void {
    window.clearInterval(this.ticker);
  }

  public toggleNotifications(): void {
    this.showNotifications.update(show => !show);
  }

  public closeNotifications(): void {
    this.showNotifications.set(false);
  }

  pauseOrResumeTimer(): void {
    const t = this.activeTimer();
    if (t)
    (t.isPaused
      ? this.timers.resumeTimer()
      : this.timers.pauseTimer()
    ).subscribe({ next: (v) => this.activeTimer.set(v) });
  }

  stopTimer(): void {
    this.timers.stopTimer().subscribe({
      next: () => {
        this.activeTimer.set(null);
        this.loadTimeTotals();
        this.showTimerStoppedModal.set(true);
      },
    });
  }

  closeTimerStoppedModal(): void {
    this.showTimerStoppedModal.set(false);
  }

  formatDuration(s: number): string {
    return [Math.floor(s / 3600), Math.floor((s % 3600) / 60), s % 60]
    .map((v) => String(v).padStart(2, '0'))
    .join(':');
  }

  formatHours(m: number): string {
    return `${Math.floor(m / 60)}h ${m % 60}m`;
  }

  progress(m: number, target: number): number {
    return Math.min(100, Math.round((m / target) * 100));
  }

  taskStatus(s: TaskResponse['status']): string {
    return s
    .replace('_', ' ')
    .toLowerCase()
    .replace(/\b\w/g, (c) => c.toUpperCase());
  }

  initials(n: string | null): string {
    return (n || 'MY')
    .split(' ').map((p) => p[0]).join('').slice(0, 2).toUpperCase();
  }

  private loadDashboard(): void {
    const today = this.startOfDay( new Date()),
    tomorrow = new Date(today);
    tomorrow.setDate(today.getDate() + 1);
    forkJoin({
      timer: this.timers.getActiveTimer().pipe(catchError(()=> of(null))),
      projects: this.projectsApi.getProjects().pipe(catchError(() => of([]))),
      myTasks: this.tasksApi.getMyTasks().pipe(catchError(() => of([]))),
      entries: this.entriesApi.getMyEntries().pipe(catchError(() => of([]))),
      events: this.calendarApi
        .getEvents(this.dateTimeKey(today), this.dateTimeKey(tomorrow))
        .pipe(catchError(() => of([]))),
        availableUsers: this.teamApi.getAvailableUsers().pipe(catchError(() => of([]))),
    })
    .pipe(finalize(() => this.isLoading.set(false)))
    .subscribe((data) => {
      this.activeTimer.set(data.timer);
      this.projects.set(data.projects);
      this.calendarEvents.set(data.events);
      this.availableUsers.set(data.availableUsers);
      this.setTimeTotals(data.entries);
      this.tasks.set(data.myTasks);
      if (this.isManagerView()) this.loadPendingApprovals();
    });
  }

  private loadPendingApprovals(): void {
    this.timesheetsApi
      .getPendingWorkspaceTimesheets()
      .subscribe((timesheets) =>
        this.pendingTimesheets.set(
          timesheets.filter((timesheet) => timesheet.status === 'SUBMITTED'),
        ),);
  }

  private loadTimeTotals(): void {
    this.entriesApi
      .getMyEntries()
      .pipe(catchError(() => of([])))
      .subscribe((v) => this.setTimeTotals(v));
  }

  private setTimeTotals(entries: TimeEntryResponse[]): void {
    const today = this.startOfDay(new Date()),
      week = new Date(today);
    week.setDate(today.getDate() - ((today.getDay() + 6) % 7));
    const end = new Date(week);
    end.setDate(week.getDate() + 7);
    this.todayMinutes.set(
      entries.filter((e) => this.dateKey(new Date(e.startTime)) === this.dateKey(today))
      .reduce((n, e) => n + this.entryMinutes(e), 0),);
      this.weekMinutes.set(
        entries.filter((e) => {
          const d = new Date(e.startTime);
          return d >= week && d < end;
        })
        .reduce((n, e) => n + this.entryMinutes(e), 0)
      );
  }

  private entryMinutes(entry: TimeEntryResponse): number {
    const start = +new Date(entry.startTime),
    end = +new Date(entry.endTime);
    return Number.isFinite(start) && Number.isFinite(end) && end >= start ? Math.round((end - start) / 60000) : entry.durationMinutes;
  }

  private startOfDay(d: Date): Date {
    return new Date(d.getFullYear(), d.getMonth(), d.getDate());
  }

  private dateKey(d: Date): string {
    return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
  }

  private dateTimeKey(d: Date): string {
    const hours = String(d.getHours()).padStart(2, '0');
    const minutes = String(d.getMinutes()).padStart(2, '0');
    const seconds = String(d.getSeconds()).padStart(2, '0');
    return `${this.dateKey(d)}T${hours}:${minutes}:${seconds}`;
  }

  private formatWeek(d: Date): string {
    const mon = new Date(d);
    mon.setDate(d.getDate() - ((d.getDay() + 6) % 7));
    const sun = new Date(mon);
    sun.setDate(mon.getDate() + 6);
    return `${mon.toLocaleDateString('en-ZA', { day: 'numeric', month: 'short' })} - ${sun.toLocaleDateString('en-ZA', { day: 'numeric', month: 'short', year: 'numeric' })}`;
  }
}