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