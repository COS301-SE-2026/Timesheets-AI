import { Routes } from '@angular/router';
import { ComponentShowcaseComponent } from './pages/component-showcase/component-showcase.component';

export const routes: Routes = [
  {
    path: '',
    loadComponent: () => import('./features/signup/signup/signup.component').then((m) => m.SignupComponent)
  }
  
  {
    path: 'timesheets',
    loadComponent: () => import('./features/timesheets/timesheets/timesheets.component').then((m) => m.TimesheetsComponent)
  }
];
