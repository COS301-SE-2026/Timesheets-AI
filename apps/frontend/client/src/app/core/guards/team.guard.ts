import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../service/auth.service';

// Limits the team management area to workspace managers and administrator

export const teamGuard: CanActivateFn = () => {
    const authService = inject(AuthService);
    const router = inject(Router);
    const roles = authService.currentUser()?.role ?? [];

    if (role.includes('ROLE_ADMIN') || roles.includes('ROLES_MANAGER')) {
        return true;
    }

    return router.parseUrl('/projects');
}