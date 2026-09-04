// core/guards/workspace.guard.ts
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '../services/auth.service';

export const workspaceGuard = () => {
  
  const authService = inject(AuthService);
  const router = inject(Router);
  
  const user = authService.currentUser();
  
  //this should check if a user has any workspace role
  const hasWorkspace = user?.roles?.some((role: string) => 
    role === 'ROLE_DEVELOPER' ||  role === 'ROLE_MANAGER' || role === 'ROLE_ADMIN'
  );
  
  if (!hasWorkspace) {
    return router.parseUrl('/waiting-for-workspace');
  }
  
  return true;
};