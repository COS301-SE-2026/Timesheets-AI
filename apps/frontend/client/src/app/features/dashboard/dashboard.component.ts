import { Component, inject, OnInit, signal } from '@angular/core';
import { NotificationPanelComponent } from '../notifications/notification-panel.component';
import { NotificationService } from '../../core/services/notification.service';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [
    NotificationPanelComponent
  ],
  templateUrl: './dashboard.component.html',
  styleUrl: './dashboard.component.scss'
})
export class DashboardComponent implements OnInit {

  private readonly notificationService = inject(NotificationService);

  public readonly showNotifications = signal<boolean>(false);

  public readonly unreadCount = this.notificationService.unreadCount;

  public ngOnInit(): void {
    //this will load the number displayed on the notification bell
    this.notificationService.loadUnreadCount();
  }

  public toggleNotifications(): void {
    this.showNotifications.update(show => !show);
  }

  public closeNotifications(): void {
    this.showNotifications.set(false);
  }
}