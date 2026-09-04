/* ============================================================
  APP ROUTES
  Momently (top-level application routing)
  All feature routes are lazy loaded for performance
  ============================================================ */

import { Routes } from '@angular/router';
import { LandingPageComponent } from './features/landing/landing-page/landing-page.component';
import { authGuard } from './core/guards/auth.guard';
import { workspaceGuard } from './core/guards/workspace.guard'; 
import { teamGuard } from './core/guards/team.guard'

export const routes: Routes = [
  /* Default redirect */
  {
    path: '',
    component: LandingPageComponent,
  },

  /* Log Time page */
  {
    path: 'log-time',
    canActivate: [authGuard, workspaceGuard],
    loadComponent: () =>
      import('./features/users/developer/logtime/logtime.component').then(
        (m) => m.LogtimeComponent,
      ),
  },

  /* Signup page */
  {
    path: 'signup',
    data: { layout: 'auth' },
    loadComponent: () =>
      import('./features/signup/signup/signup.component').then(
        (m) => m.SignupComponent,
      ),
  },

  /* Login page */
  {
    path: 'login',
    data: { layout: 'auth' },
    loadComponent: () =>
      import('./features/login/login.component').then((m) => m.LoginComponent),
  },

  /* Timesheets page */
  {
    path: 'timesheets',
    canActivate: [authGuard, workspaceGuard],
    loadComponent: () =>
      import('./features/timesheets/timesheets.component').then(
        (m) => m.TimesheetsComponent,
      ),
  },
  
  /* Projects page */
  {
    path: 'projects',
    canActivate: [authGuard, workspaceGuard],
    loadComponent: () =>
      import('./features/projects/projects.component')
        .then(m => m.ProjectsComponent)
  },

  /* Project Details Page */
  {
    path: 'projects/:id',
    canActivate: [authGuard, workspaceGuard],
    loadComponent: () =>
      import('./features/projects/project-details/project-details.component')
        .then(m => m.ProjectDetailsComponent)
  },
  // Tasks page

  {
    path: 'my-tasks',
    canActivate: [authGuard, workspaceGuard],
    loadComponent: () =>
      import('./features/my-tasks/my-tasks.component')
        .then(m => m.MyTasksComponent)
  },

  {
    path: 'leave-requests',
    canActivate: [authGuard, workspaceGuard],
    loadComponent: () =>
      import('./features/leave-requests/leave-requests.component')
        .then(m => m.LeaveRequestsComponent)
  },

  // Team page

  {
    path: 'team',
    canActivate: [authGuard, workspaceGuard, teamGuard ],
    loadComponent: () =>
      import('./features/teams/teams.component').then((m) => m.TeamsComponent),
  },

  //waiting-for-workspace
  {
    path: 'waiting-for-workspace',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/onboarding/waiting-for-workspace/waiting-for-workspace.component')
        .then(m => m.WaitingForWorkspaceComponent)
  },
  {
    path: 'calendar',
    loadComponent: () =>
      import('./features/calendar/calendar.component')
        .then(m => m.CalendarComponent)
  },

  /* 404 fallback */
  {
    path: 'not-found',
    data: { layout: 'auth' },
    loadComponent: () =>
      import('./pages/not-found/not-found.component').then(
        (m) => m.NotFoundComponent,
      ),
  },

  /* Catch-all wildcard (redirects to 404) */
  {
    path: '**',
    redirectTo: 'not-found',
  },
];
