import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../services/auth.service';

// Limits the team management area to workspace managers and administrator

export const teamGuard: CanActivateFn = () => {
    const authService = inject(AuthService);
    const router = inject(Router);
    const roles = authService.currentUser()?.roles ?? [];

    if (roles.includes('ROLE_ADMIN') || roles.includes('ROLE_MANAGER')) {
        return true;
    }

    return router.parseUrl('/projects');
}