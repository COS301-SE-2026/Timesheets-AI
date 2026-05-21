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
    path: 'signup',
    data: { layout: 'auth' },
    loadComponent: () =>
      import('./features/signup/signup/signup.component').then(m => m.SignupComponent)
  },
  {
    path: 'not-found',
    data: { layout: 'auth' },
    loadComponent: () =>
      import('./pages/not-found/not-found.component').then(m => m.NotFoundComponent)
  }
];
