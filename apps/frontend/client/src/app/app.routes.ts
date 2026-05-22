/* ============================================================
  APP ROUTES
  Momently (top-level application routing)
  All feature routes are lazy loaded for performance
  ============================================================ */

import { Routes } from '@angular/router';

export const routes: Routes = [

  /* Default redirect */
  {
    path: '',
    redirectTo: 'signup',
    pathMatch: 'full'
  },

  /* Log Time page */
  {
    path: 'log-time',
    loadComponent: () =>
      import('./features/users/developer/logtime/logtime.component')
        .then(m => m.LogtimeComponent)
  },

  /* Signup page */
  {
    path: 'signup',
    data: { layout: 'auth' },
    loadComponent: () =>
      import('./features/signup/signup/signup.component')
        .then(m => m.SignupComponent)
  },

  /* Login page */
  {
    path: 'login',
    data: { layout: 'auth' },
    loadComponent: () =>
      import('./features/login/login.component')
        .then(m => m.LoginComponent)
  },

  /* Log time page (placeholder for Demo 1) */
  {
    path: 'log-time',
    loadComponent: () =>
      import('./pages/log-time-placeholder/log-time-placeholder.component')
        .then(m => m.LogTimePlaceholderComponent)
  },
  /* Timesheets page */
  {
    path: 'timesheets',
    loadComponent: () =>
      import('./features/timesheets/timesheets.component')
        .then(m => m.TimesheetsComponent)
  },

  /* 404 fallback */
  {
    path: 'not-found',
    data: { layout: 'auth' },
    loadComponent: () =>
      import('./pages/not-found/not-found.component')
        .then(m => m.NotFoundComponent)
  },

  /* Catch-all wildcard (redirects to 404) */
  {
    path: '**',
    redirectTo: 'not-found'
  }

];