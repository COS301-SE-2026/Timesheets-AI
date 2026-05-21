/* ============================================================
   APP ROUTES
   Momently — top-level application routing
   All feature routes are lazy loaded for performance
   ============================================================ */

import { Routes } from '@angular/router';
import { ComponentShowcaseComponent } from './pages/component-showcase/component-showcase.component';

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

  /* 404 fallback */
  {
    path: 'not-found',
    data: { layout: 'auth' },
    loadComponent: () =>
      import('./pages/not-found/not-found.component')
        .then(m => m.NotFoundComponent)
  }

];