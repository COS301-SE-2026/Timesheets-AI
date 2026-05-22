import { Routes } from '@angular/router';

export const routes: Routes = [
  {
    path: '',
    redirectTo: 'signup',
    pathMatch: 'full'
  },
  {
    path: 'log-time',
    loadComponent: () =>
      import('./features/users/developer/logtime/logtime.component').then(
        (m) => m.LogtimeComponent
      )
  },
  {
    path: 'signup',
    loadComponent: () =>
      import('./features/signup/signup/signup.component')
        .then((m) => m.SignupComponent)
  },
  {
    path: 'login',
    loadComponent: () =>
      import('./features/login/login.component')
        .then(m => m.LoginComponent)
  }
];