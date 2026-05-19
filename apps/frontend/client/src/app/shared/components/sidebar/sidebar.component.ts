import { Component, signal } from '@angular/core'; // UI componenet and signal store state, uodate UI changes automatically 
import { CommonModule } from '@angular/common'; //to be able use ngIf and ngFor 

interface NavItem {
  label: string, //text shown in sidebar
  icon: string, // CSS class or shared icon
  route: string; // Navigation URL 
}

@Component({
  selector: 'app-sidebar', // HTML tag to show this UI component
  imports: [CommonModule],
  standalone: true,
  templateUrl: './sidebar.component.html', // links the HTML for the UI component 
  styleUrl: './sidebar.component.scss' // links SCSS file 
})

export class SidebarComponent {
  // create reactive state variable so it stores state, update UI automatically when changed 
  navItems = signal<NavItem[]>([
    { label: 'Dashboard', icon:'icon-dashboard', route:'/dashboard'}, 
    { label: 'Timesheets', icon: 'icon-timesheets', route: '/timesheets' }, 
    { label: 'Log Time', icon: 'icon-clock', route: '/log-time' },
    { label: 'Projects', icon: 'icon-folder', route: '/projects' },
    { label: 'Calendar', icon: 'icon-calendar', route: '/calendar' },
    { label: 'Leave Requests', icon: 'icon-briefcase', route: '/leave' },
    { label: 'Reports', icon: 'icon-chart', route: '/reports' },
    { label: 'Insights', icon: 'icon-trending-up', route: '/insights' },
    { label: 'Team', icon: 'icon-users', route: '/team' },
    { label: 'Settings', icon: 'icon-settings', route: '/settings' }
  ]);

  // Temporary mock tracking for active link styling until full router links are added 
  // Stores the currently selected navigation item.
  activeItem = signal<string>('Timesheets');

  setActive(label: string){
    this.activeItem.set(label);
  }
}
