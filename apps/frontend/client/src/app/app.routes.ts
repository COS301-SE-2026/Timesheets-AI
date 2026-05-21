import { Routes } from '@angular/router';
import { ComponentShowcaseComponent } from './pages/component-showcase/component-showcase.component';

export const routes: Routes = [
  {
    path: '',
    redirectTo: 'signup',
    pathMatch: 'full'
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
    loadComponent: () => import('./features/signup/signup/signup.component').then((m) => m.SignupComponent)
  },
  
  {
    path: 'timesheets',
    loadComponent: () => import('./features/timesheets/timesheets.component').then((m) => m.TimesheetsComponent)
  }
];
