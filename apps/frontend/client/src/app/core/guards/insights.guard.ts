// core/guards/insights.guard.ts
import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../services/auth.service';

export const insightsGaurd: CanActivateFn = () => {
  const authService = inject(AuthService);
  const router = inject(Router);

  const roles = authService.currentUser()?.roles?? [];


    if (roles.includes('ROLE_DEVELOPER') || roles.includes('ROLE_MANAGER')) {
        return true;
    }

    return router.parseUrl('/dashboard');
};
