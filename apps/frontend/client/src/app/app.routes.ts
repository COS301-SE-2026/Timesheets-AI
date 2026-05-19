import { Routes } from '@angular/router';

export const routes: Routes = [
  {
    path: '',
    loadComponent: () => import('./features/signup/signup/signup.component').then((m) => m.SignupComponent)
  }
];
