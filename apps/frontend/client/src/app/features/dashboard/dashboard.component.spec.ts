import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { signal, WritableSignal } from '@angular/core';
import { of } from 'rxjs';
import { DashboardComponent } from './dashboard.component';
import { AuthService, AuthUser } from '../../core/services/auth.service';
import { NotificationService } from '../../core/services/notification.service';
import { ProjectDetailResponse, ProjectService } from '../../core/services/project.service';
import { TaskService } from '../../core/services/task.service';
import { TimerService } from '../../core/services/timer.service';
import { TimeEntryService } from '../../core/services/time-entry.service';
import { TimesheetService } from '../../core/services/timesheet.service';
import { TeamService } from '../../core/services/team.service';
import { CalendarService } from '../calendar/calendar.services';

const adminUser: AuthUser = {
    id: 'admin-1',
    email: 'admin@example.com',
    firstName: 'Faith',
    lastName: 'Solomons',
    avatarUrl: null,
    emailVerified: true,
    roles: ['ROLE_ADMIN'],
    mfaEnabled: false,
};

const developerUser: AuthUser = {
    ...adminUser,
    id: 'developer-1',
    roles: ['ROLE_DEVELOPER'],
};

const activeProject = {
    id: 'project-1',
    name: 'Mobile app developemt',
    description: null,
    status: 'ACTIVE' as const,
    budgetHours: 40,
    hourlyRate: null,
    budgetCost: null,
    startDate: null,
    endDate: null,
    myRole: 'ADMIN' as const,
    createdAt: '2026-09-01T00:00:00',
    updatedAt: '2026-09-01T00:00:00',
};

const projectDetail: ProjectDetailResponse = {
    ...activeProject,
    totalCost: null,
    hoursLogged: 600,
    progressPercentage: 0,
    members: [
        {
            workspaceMemberId: 'member-1',
            firstName: 'Alex',
            lastName: 'Johnson',
            email: 'alex@example.com',
            role: 'MANAGER',
            isProjectManager: true,
            hoursLogged: 300,
            joinedAt: '2026-09-01T00:00:00',
        },
        {
            workspaceMemberId: 'member-2',
            firstName: 'Terry',
            lastName: 'White',
            email: 'terry@example.com',
            role: 'DEVELOPER',
            isProjectManager: false,
            hoursLogged: 300,
            joinedAt: '2026-09-01T00:00:00',
        },
    ],
};
