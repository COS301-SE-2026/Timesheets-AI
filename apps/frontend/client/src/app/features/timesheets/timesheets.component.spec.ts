/*
This handles HttpClientTesting for the four services timesheet service, projectservice, task service and auth service
we will be using a fake AuthService since the real one reads/writes localStorage
reference: https://angular.dev/guide/http/testing
*/
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { signal } from '@angular/core';
import { TimesheetsComponent } from './timesheets.component';
import { provideHttpClient } from '@angular/common/http';
import {
  HttpTestingController,
  provideHttpClientTesting,
} from '@angular/common/http/testing';
import { AuthService, AuthUser } from '../../core/services/auth.service';

describe('TimesheetsComponent', () => {
  let fixture: ComponentFixture<TimesheetsComponent>;
  let component: TimesheetsComponent;
  let httpMock: HttpTestingController;

  //real shaped ids, these are like the seed data and tests dont depend on seeded db

  const projectOneId = '00000000-0000-0000-0001-000000000040';
  const projectTwoId = '00000000-0000-0000-0001-000000000041';

  const taskOneId = '00000000-0000-0000-0002-000000000070';
  const taskTwoId = '00000000-0000-0000-0002-000000000071';

  const currentTimesheetId = 'timesheet-current';
  const pastTimesheetId = 'timesheet-past';

  const mockProjects = [
    { id: projectOneId, name: 'Mobile App Development' },
    { id: projectTwoId, name: 'Backend API' },
  ];

  const mockTasks = [
    { id: taskOneId, projectId: projectOneId, title: 'Implement Login Screen' },
    { id: taskTwoId, projectId: projectTwoId, title: 'Create Timesheet API' },
  ];

  function today(): string {
    return new Date().toISOString().slice(0, 10);
  }
  function daysAgo(n: number): string {
    const d = new Date();
    d.setDate(d.getDate() - n);
    return d.toISOString().slice(0, 10);
  }

  //the "current" one is DRAFT and spans today, the past one is APPROVED and already over
  const mockTimesheets = [
    {
      id: currentTimesheetId,
      workspaceMemberId: 'member-1',
      periodStart: today(),
      periodEnd: today(),
      status: 'DRAFT' as const,
      submittedAt: null,
      approvedAt: null,
      approvedByWorkspaceMemberId: null,
      rejectedAt: null,
      rejectionReason: null,
      isLocked: false,
      lockedAt: null,
      createdAt: today(),
      updatedAt: today(),
    },
    {
      id: pastTimesheetId,
      workspaceMemberId: 'member-1',
      periodStart: daysAgo(14),
      periodEnd: daysAgo(8),
      status: 'APPROVED' as const,
      submittedAt: daysAgo(8),
      approvedAt: daysAgo(7),
      approvedByWorkspaceMemberId: 'manager-1',
      rejectedAt: null,
      rejectionReason: null,
      isLocked: true,
      lockedAt: daysAgo(7),
      createdAt: daysAgo(14),
      updatedAt: daysAgo(7),
    },
  ];

  //had 2hrs and 1hrs on the same task/day, used to check bucketing and totals
  const mockEntries = [
    {
      id: 'entry-1',
      timesheetId: currentTimesheetId,
      workspaceMemberId: 'member-1',
      projectId: projectOneId,
      taskId: taskOneId,
      startTime: `${today()}T09:00:00`,
      endTime: `${today()}T11:00:00`,
      durationMinutes: 7200, // 2 hrs in seconds, matches the durationMinutes-is-seconds quirk
      entryType: 'MANUAL' as const,
      description: 'Login screen work',
    },
    {
      id: 'entry-2',
      timesheetId: currentTimesheetId,
      workspaceMemberId: 'member-1',
      projectId: projectOneId,
      taskId: taskOneId,
      startTime: `${today()}T14:00:00`,
      endTime: `${today()}T15:00:00`,
      durationMinutes: 3600, // 1 hour, in seconds
      entryType: 'MANUAL' as const,
      description: 'More login screen work',
    },
  ];

  const developerUser: AuthUser = {
    id: 'user-1',
    email: 'dev@momentum.co.za',
    firstName: 'Developer',
    lastName: 'Senior',
    avatarUrl: null,
    emailVerified: true,
    roles: ['DEVELOPER'],
    mfaEnabled: false,
  };

  const managerUser: AuthUser = { ...developerUser, roles: ['MANAGER'] };

  //this flushes the 3 requestss forkJoin fires from loadTimesheets()
  function flushInitialLoad(entries: typeof mockEntries = mockEntries): void {
    httpMock.expectOne('/api/timesheets/me').flush(mockTimesheets);
    httpMock.expectOne('/api/projects').flush(mockProjects);
    httpMock.expectOne('/api/tasks/my-tasks').flush(mockTasks);
    httpMock
      .expectOne(`/api/timesheets/${currentTimesheetId}/entries`)
      .flush(entries);
  }

  //this sets up TestBed with a given AuthService stand in so we can test withouth needing the real
  //authservice'es localStorage side effects
  async function setup(currentUser: AuthUser | null): Promise<void> {
    TestBed.resetTestingModule();

    await TestBed.configureTestingModule({
      imports: [TimesheetsComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        {
          provide: AuthService,
          useValue: { currentUser: signal(currentUser) },
        },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(TimesheetsComponent);
    component = fixture.componentInstance;
    httpMock = TestBed.inject(HttpTestingController);
    fixture.detectChanges(); //this will trigger the constructor, which calls the loadTimesheets()
  }
  afterEach(() => {
    httpMock.verify(); //it'll fail if any request was made but not asserted
  });

  describe('as a developer', () => {
    beforeEach(async () => {
      await setup(developerUser);
      flushInitialLoad();
      fixture.detectChanges();
    });
    it('should create', () => {
      expect(component).toBeTruthy();
    });

    it('should load timesheets, projects, and tasks, and select the first timesheet by default', () => {
      expect(component.uiState()).toBe('idle');
      expect(component.filteredTimesheets()).toHaveLength(2);
      expect(component.selectedTimesheetId()).toBe(currentTimesheetId);
      expect(component.summary()?.status).toBe('DRAFT');
    });

    it('should group entries into task rows with seconds converted to minutes', () => {
      const rows = component.tasks();
      expect(rows).toHaveLength(1); //both entries share taskOneId

      const row = rows[0];
      expect(row.id).toBe(taskOneId);
      expect(row.title).toBe('Implement Login Screen');
      expect(row.project).toBe('Mobile App Development');
      expect(row.total).toBe('3hr 0m'); //7200s -> 120min and 3600s -> 60m, so total will be 180m -> 3hr 0m
    });

    it('should compute daily totals and grand totals across all task rows', () => {
      const todayIndex = component.days().findIndex((d) => d.isToday);
      expect(component.dailyTotals()[todayIndex]).toBe('3hr 0m');
      expect(component.grandTotal()).toBe('3hr 0m');
    });

    it('should show "-" for days with no logged time', () => {
      const todayIndex = component.days().findIndex((d) => d.isToday);
      const otherIndex = todayIndex === 0 ? 1 : 0;
      expect(component.dailyTotals()[otherIndex]).toBe('-');
    });
    it('should report an empty state when the backend returns no timesheets', async () => {
      await setup(developerUser);
      httpMock.expectOne('/api/timesheets/me').flush([]);
      httpMock.expectOne('/api/projects').flush(mockProjects);
      httpMock.expectOne('/api/tasks/my-tasks').flush(mockTasks);

      expect(component.uiState()).toBe('empty');
      expect(component.summary()).toBeNull();
    });

    it('should set an error state when loading timesheets fails', async () => {
      await setup(developerUser);

      //forkjoin cancels its sibling requests (project/tasks)
      httpMock
        .expectOne('/api/timesheets/me')
        .error(new ProgressEvent('network error'));
      httpMock.match('/api/projects').forEach((req) => {
        if (!req.cancelled) req.flush(mockProjects);
      });
      httpMock.match('/api/tasks/my-tasks').forEach((req) => {
        if (!req.cancelled) req.flush(mockTasks);
      });
      expect(component.uiState()).toBe('error');
      expect(component.errorMessage()).toBe(
        'Failed to load timesheets. Please try again',
      );
    });
    it('should hit the status-filtered endpoint when a filter other than ALL is selected', () => {
      component.onFilterChange('APPROVED');
      const req = httpMock.expectOne('/api/timesheets/me/status/APPROVED');
      req.flush([mockTimesheets[1]]);
      httpMock.expectOne('/api/projects').flush(mockProjects);
      httpMock.expectOne('/api/tasks/my-tasks').flush(mockTasks);
      httpMock
        .expectOne(`/api/timesheets/${pastTimesheetId}/entries`)
        .flush([]);

      expect(component.filteredTimesheets()).toHaveLength(1);
      expect(component.summary()?.status).toBe('APPROVED');
    });
    it('should switch weeks and load that weeks entries on onWeekChange', () => {
      component.onWeekChange(pastTimesheetId);
      expect(component.selectedTimesheetId()).toBe(pastTimesheetId);

      const req = httpMock.expectOne(
        `/api/timesheets/${pastTimesheetId}/entries`,
      );
      req.flush([]);

      expect(component.summary()?.id).toBe(pastTimesheetId);
      expect(component.hasEntries()).toBe(false);
    });
    it('should allow submitting a DRAFT, unlocked timesheet', () => {
      expect(component.canSubmit()).toBe(true);
    });

    it('should open and close the submit dialog ', () => {
      component.openSubmitDialog();
      expect(component.showSubmitDialog()).toBe(true);
      component.closeSubmitDialog();
      expect(component.showSubmitDialog()).toBe(false);
    });
    it('should submit the timesheet and show the success dialog', () => {
      component.openSubmitDialog();
      component.confirmSubmitTimesheet();

      const req = httpMock.expectOne(
        `/api/timesheets/${currentTimesheetId}/submit`,
      );
      expect(req.request.method).toBe('POST');

      req.flush({
        ...mockTimesheets[0],
        status: 'SUBMITTED',
        submittedAt: today(),
        isLocked: true,
      });

      expect(component.summary()?.status).toBe('SUBMITTED');
      expect(component.showSubmitDialog()).toBe(false);
      expect(component.showSubmitSuccessDialog()).toBe(true);
      expect(component.actionPending()).toBe(false);
    });

    it('should surface an error and not open the success dialog if submit fails', () => {
      component.openSubmitDialog();
      component.confirmSubmitTimesheet();

      const req = httpMock.expectOne(
        `/api/timesheets/${currentTimesheetId}/submit`,
      );
      req.error(new ProgressEvent('network error'));

      expect(component.errorMessage()).toBe('Unable to submit the timesheet.');
      expect(component.showSubmitSuccessDialog()).toBe(false);
      expect(component.actionPending()).toBe(false);
    });

    it('should not call the backend when confirmSubmitTimesheet runs on a locked timesheet', () => {
      component.onWeekChange(pastTimesheetId);
      httpMock
        .expectOne(`/api/timesheets/${pastTimesheetId}/entries`)
        .flush([]);

      component.confirmSubmitTimesheet();

      // canSubmit() is false for a locked/approved timesheet, confirmSubmitTimesheet
      // should bail before making any HTTP call, verified by afterEach's httpMock.verify()
      expect(component.canApproveOrReject()).toBe(false);
    });

    it('should not show approve/reject actions for a developer', () => {
      expect(component.canApproveOrReject()).toBe(false);
      expect(component.isManager()).toBe(false);
    });

    it('should not open the reject reason when the user cannot approve/reject', () => {
      component.openRejectDialog();
      expect(component.showRejectReason()).toBe(false);
    });

    it('should dismiss the toast automatically after 4 seconds', () => {
      jest.useFakeTimers();

      component['showToast']('Test message');
      expect(component.toastMessage()).toBe('Test message');

      jest.advanceTimersByTime(4000);
      expect(component.toastMessage()).toBeNull();

      jest.useRealTimers();
    });

    it('should format status labels with proper casing', () => {
      expect(component.statusLabel('ALL')).toBe('ALL');
      expect(component.statusLabel('SUBMITTED')).toBe('Submitted');
    });

    it('should return "-" for null date/time values', () => {
      expect(component.formatDate(null)).toBe('-');
      expect(component.formatDateTime(null)).toBe('-');
      expect(component.displayHours(null)).toBe('-');
    });
  });

  describe('as a manager', () => {
    const teamTimesheetId = 'timesheet-team';
    const teamMemberId = 'member-dev-1';

      const submittedTeamTimesheet = {
        id: teamTimesheetId,
        workspaceMemberId: teamMemberId,
        periodStart: today(),
        periodEnd: today(),
        status: 'SUBMITTED' as const,
        submittedAt: today(),
        approvedAt: null,
        approvedByWorkspaceMemberId: null,
        rejectedAt: null,
        rejectReason: null,
        isLocked: true,
        lockedAt: today(),
        createdAt: today(),
        updatedAt: today(),
      },

      
      httpMock.expectOne('/api/projects').flush(mockProjects);
      httpMock.expectOne('/api/tasks/my-tasks').flush(mockTasks);
      httpMock
        .expectOne(`/api/timesheets/${currentTimesheetId}/entries`)
        .flush(mockEntries);

      fixture.detectChanges();
    });

    it('should allow approve/reject on a SUBMITTED timesheet', () => {
      expect(component.isManager()).toBe(true);
      expect(component.canApproveOrReject()).toBe(true);
      expect(component.canSubmit()).toBe(false); // it's already submitted, not a DRAFT
    });

    it('should approve the timesheet', () => {
      jest.spyOn(window, 'confirm').mockReturnValue(true);

      component.onApproveTimesheet();

      const req = httpMock.expectOne(
        `/api/timesheets/${currentTimesheetId}/approve`,
      );
      expect(req.request.method).toBe('POST');

      req.flush({
        ...mockTimesheets[0],
        status: 'APPROVED',
        approvedAt: today(),
        isLocked: true,
      });

      expect(component.summary()?.status).toBe('APPROVED');
      expect(component.toastMessage()).toBe('Timesheet Approved');
    });

    it('should not call the backend if the approval confirm is cancelled', () => {
      jest.spyOn(window, 'confirm').mockReturnValue(false);

      component.onApproveTimesheet();

      // confirm() returned false, onApproveTimesheet should bail before the
      // HTTP call, verified by afterEach's httpMock.verify()
      expect(component.actionPending()).toBe(false);
    });

    it('should open the reject dialog and require a non-empty reason', () => {
      component.openRejectDialog();
      expect(component.showRejectDialog()).toBe(true);

      component.onConfirmReject(); // no reason set yet
      // should bail silently, no HTTP call, verified by afterEach
      expect(component.showRejectDialog()).toBe(true);
    });

    it('should reject the timesheet with a reason', () => {
      component.openRejectDialog();
      component.rejectReason.set('Missing hours for Tuesday');
      component.onConfirmReject();

      const req = httpMock.expectOne(
        `/api/timesheets/${currentTimesheetId}/reject`,
      );
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual({ reason: 'Missing hours for Tuesday' });

      req.flush({
        ...mockTimesheets[0],
        status: 'REJECTED',
        rejectedAt: today(),
        rejectionReason: 'Missing hours for Tuesday',
        isLocked: false,
      });

      expect(component.summary()?.status).toBe('REJECTED');
      expect(component.summary()?.rejectionReason).toBe(
        'Missing hours for Tuesday',
      );
      expect(component.showRejectDialog()).toBe(false);
      expect(component.toastMessage()).toBe('Timesheet Rejected.');
    });

    it('should surface an error if reject fails', () => {
      component.openRejectDialog();
      component.rejectReason.set('Missing hours');
      component.onConfirmReject();

      const req = httpMock.expectOne(
        `/api/timesheets/${currentTimesheetId}/reject`,
      );
      req.error(new ProgressEvent('network error'));

      expect(component.errorMessage()).toBe('Unable to reject the timesheet.');
      expect(component.actionPending()).toBe(false);
    });
  });
});
