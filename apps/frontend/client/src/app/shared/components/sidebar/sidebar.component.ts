import { Component, signal } from '@angular/core'; // UI componenet and signal store state, uodate UI changes automatically 
import { CommonModule } from '@angular/common'; //to be able use ngIf and ngFor 
import { MatIconModule } from '@angular/material/icon';
import { Router } from '@angular/router';

interface NavItem {
  label: string, //text shown in sidebar
  icon: string, // CSS class or shared icon
  route: string; // Navigation URL 
}

@Component({
  selector: 'app-sidebar', // HTML tag to show this UI component
  imports: [CommonModule, MatIconModule],
  standalone: true,
  templateUrl: './sidebar.component.html', // links the HTML for the UI component 
  styleUrl: './sidebar.component.scss' // links SCSS file 
})

export class SidebarComponent {

  constructor(private router: Router) {}

  // create reactive state variable so it stores state, update UI automatically when changed 
  navItems = signal<NavItem[]>([
    { label: 'Dashboard', icon:'dashboard', route:'/dashboard'}, 
    { label: 'Timesheets', icon: 'description', route: '/timesheets' }, 
    { label: 'Log Time', icon: 'schedule', route: '/log-time' },
    { label: 'Projects', icon: 'folder', route: '/projects' },
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

  //Update to use Angular Router to replace the manual state tracking
  //This function changes the page route when a user clicks a sidebar item.
  setActive(route: string){
    this.router.navigate([route])
  }

  activeRoute = () => this.router.url;

}
