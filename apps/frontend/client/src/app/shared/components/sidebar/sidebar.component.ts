/**
 * Author: Kgaugelo Matsena & Lerato Sibanda
 * Date: 2026-05-15
 * Purpose: Navigation sidebar component with route-based active state tracking.
 * Related Requirement: -
 * 
 * patched: Zamokuhle Zwane, 26 July 2026
 * user-profile is hardcoded to JD, John Doe / software developer
 * 
 * patched: Zamokuhle Zwane,  03 August 2026
 * I fixed the problem with my tasks page not showing up on the sidebar. I added a new route for my tasks and updated the navItems array to include it. I also added a new icon for my tasks.
 * 
 * added: Lerato Sibanda
 * I added routing to the  teams page
 */

import { Component, signal, inject, computed} from '@angular/core'; // UI componenet and signal store state, uodate UI changes automatically 
 //to be able use ngIf and ngFor 
import { MatIconModule } from '@angular/material/icon';
import { Router, RouterModule } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';

interface NavItem {
  label: string, //text shown in sidebar
  icon: string, // CSS class or shared icon
  route: string; // Navigation URL 
  requiresWorkspace?: boolean;
  allowedRoles?: string[];
}

@Component({
  selector: 'app-sidebar', // HTML tag to show this UI component
  imports: [MatIconModule, RouterModule],
  standalone: true,
  templateUrl: './sidebar.component.html', // links the HTML for the UI component 
  styleUrl: './sidebar.component.scss' // links SCSS file 
})

export class SidebarComponent {
  private readonly router = inject(Router);
  private readonly authService = inject(AuthService);

  // create reactive state variable so it stores state, update UI automatically when changed 
  navItems = signal<NavItem[]>([
    { label: 'Dashboard', icon:'dashboard', route:'/dashboard', requiresWorkspace: true}, 
    { label: 'Timesheets', icon: 'description', route: '/timesheets', requiresWorkspace: true}, 
    { label: 'Log Time', icon: 'schedule', route: '/log-time', requiresWorkspace: true},
    { label: 'Projects', icon: 'folder', route: '/projects', requiresWorkspace: true},
    { label: 'My Tasks', icon: 'task', route: '/my-tasks', requiresWorkspace: true},
    { label: 'Calendar', icon: 'calendar_month', route: '/calendar', requiresWorkspace: true},
    { label: 'Leave Requests', icon: 'business_center', route: '/leave-requests', requiresWorkspace: true},
    { label: 'Reports', icon: 'bar_chart', route: '/reports', requiresWorkspace: true},
    { label: 'Insights', icon: 'trending_up', route: '/insights', requiresWorkspace: true},
    { label: 'Team', icon: 'groups', route: '/team', requiresWorkspace: true, allowedRoles: ['ROLE_ADMIN', 'ROLE_MANAGER']},
    { label: 'Settings', icon: 'settings', route: '/settings' }
  ]);

  readonly visibleNavItems = computed(() => {
    const roles = this.authService.currentUser()?.roles ?? [];
    return this.navItems().filter((item) =>
    !item.allowedRoles || item.allowedRoles.some((role) => roles.includes(role)),
  );
  });
  // Temporary mock tracking for active link styling until full router links are added 
  // Stores the currently selected navigation item.
  // activeRoute = signal<string>('Timesheets');

  // setActive(label: string){
  //   this.activeRoute.set(label);
  // }

  private hasWorkspace(): boolean {
      const user = this.authService.currentUser();
      return user?.roles?.some((role: string) => 
        role === 'ROLE_DEVELOPER' || role === 'ROLE_MANAGER' || role === 'ROLE_ADMIN') || false;
    }

    //if the user does not have access to a workspace, take them to the waiting page
    navigateTo(route: string, requiresWorkspace = false): void {

    if (requiresWorkspace && !this.hasWorkspace()) {
      this.router.navigate(['/waiting-for-workspace']);
      return;
    }
    
    // otherwise navigate normally
    this.router.navigate([route]);
  }

  readonly currentUser = this.authService.currentUser;
  readonly displayName = computed(() => {
    const user = this.currentUser();
    if(!user) return 'Guest';

    return `${user.firstName} ${user.lastName}`.trim();
  });

  readonly displayRole = computed(() =>{
    const role = this.currentUser()?.roles?.[0];
    if(!role) return ''
    const withoutPrefix = role.startsWith('ROLE_') ? role.slice(5):role;
    return withoutPrefix.charAt(0) +withoutPrefix.slice(1).toLowerCase();
  })

  //first letter of first+last name
  readonly initials = computed(() =>{
    const user = this.currentUser();
    if(!user) return "?";
    const first = user.firstName?.charAt(0)??'';
    const last = user.lastName?.charAt(0)??'';
    return (first+last).toUpperCase() || '?';
  });

  //Update to use Angular Router to replace the manual state tracking
  //This function changes the page route when a user clicks a sidebar item.
  setActive(route: string){
    this.router.navigate([route])
  }

  //adding logout button on the dropdown
  readonly isMenuOpen = signal<boolean>(false);

  toggleMenu(): void{
    this.isMenuOpen.update((open) => !open);
  }
  onLogout(): void{
    this.authService.logout();
  }
  public onProfileClick(event: MouseEvent): void {
    const clickedInsideMenu = (event.target as HTMLElement).closest('.user-menu');
    if (!clickedInsideMenu) {
      this.toggleMenu();
    }
  }
  activeRoute = () => this.router.url;

}
