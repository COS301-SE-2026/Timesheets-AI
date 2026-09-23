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

