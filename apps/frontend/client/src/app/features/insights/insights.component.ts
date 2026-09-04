/*
Routed at /insights (see app.routes.ts). Reads the signed-in user's roles off AuthService, same pattern as workspace.guard.ts, and renders the
matching view, ROLE_ADMIN gets the manager view, same as the guard already treats admin as having full workspace access
 */
import { Component, computed, inject } from '@angular/core';

import { AuthService } from '../../core/services/auth.service';
import { DeveloperInsightsComponent } from './developer-insights/developer-insights.component';
import { ManagerInsightsComponent } from './manager-insights/manager-insights.component';

@Component({
  selector: 'app-insights',
  standalone: true,
  imports: [DeveloperInsightsComponent, ManagerInsightsComponent],
  template: `
    @if (isManager()) {
      <app-manager-insights></app-manager-insights>
    } @else {
      <app-developer-insights></app-developer-insights>
    }
  `
})
export class InsightsComponent {
  private readonly authService = inject(AuthService);

  readonly isManager = computed(() => {
    const roles = this.authService.currentUser()?.roles ?? [];
    return roles.includes('ROLE_MANAGER') || roles.includes('ROLE_ADMIN');
  });
}
