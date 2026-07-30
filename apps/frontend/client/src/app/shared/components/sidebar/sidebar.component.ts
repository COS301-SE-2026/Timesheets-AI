/**
 * Author: Kgaugelo Matsena & Lerato Sibanda
 * Date: 2026-05-15
 * Purpose: Navigation sidebar component with route-based active state tracking.
 * Related Requirement: -
 * 
 * patched: Zamokuhle Zwane, 26 July 2026
 * user-profile is hardcoded to JD, John Doe / software developer
 */

import { Component, signal, inject, computed} from '@angular/core'; // UI componenet and signal store state, uodate UI changes automatically 
 //to be able use ngIf and ngFor 
import { MatIconModule } from '@angular/material/icon';
import { Router } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';

interface NavItem {
  label: string, //text shown in sidebar
  icon: string, // CSS class or shared icon
  route: string; // Navigation URL 
}

@Component({
  selector: 'app-sidebar', // HTML tag to show this UI component
  imports: [MatIconModule],
  standalone: true,
  templateUrl: './sidebar.component.html', // links the HTML for the UI component 
  styleUrl: './sidebar.component.scss' // links SCSS file 
})

export class SidebarComponent {
  private readonly router = inject(Router);
  private readonly authService = inject(AuthService);

  // create reactive state variable so it stores state, update UI automatically when changed 
  navItems = signal<NavItem[]>([
    { label: 'Dashboard', icon:'dashboard', route:'/dashboard'}, 
    { label: 'Timesheets', icon: 'description', route: '/timesheets' }, 
    { label: 'Log Time', icon: 'schedule', route: '/log-time' },
    { label: 'Projects', icon: 'folder', route: '/projects' },
    { label: 'My Tasks', icon: 'assignment', route: '/my-tasks' },
    { label: 'Calendar', icon: 'calendar_month', route: '/calendar' },
    { label: 'Leave Requests', icon: 'business_center', route: '/leave' },
    { label: 'Reports', icon: 'bar_chart', route: '/reports' },
    { label: 'Insights', icon: 'trending_up', route: '/insights' },
    { label: 'Team', icon: 'groups', route: '/team' },
    { label: 'Settings', icon: 'settings', route: '/settings' }
  ]);

  // Temporary mock tracking for active link styling until full router links are added 
  // Stores the currently selected navigation item.
  // activeRoute = signal<string>('Timesheets');

  // setActive(label: string){
  //   this.activeRoute.set(label);
  // }

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
