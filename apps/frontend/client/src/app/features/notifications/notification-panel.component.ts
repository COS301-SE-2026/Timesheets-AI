/** 
 * Author: Nyasha 15 September 2026
 * - this will be the panel that shows all the notifications for a user
 * 
 */

import { Component, EventEmitter, HostListener, OnInit, Output, computed, inject, signal, } from '@angular/core';
import { CommonModule } from '@angular/common';
import { NotificationResponse, NotificationService, } from '../../core/services/notification.service';

@Component({
  selector: 'app-notification-panel',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './notification-panel.component.html',
  styleUrl: './notification-panel.component.scss',
})

export class NotificationPanelComponent implements OnInit {

  //service for communicating with the notification backend
  private readonly notificationService = inject(NotificationService);

  @Output() closePanel = new EventEmitter<void>();

  public readonly notifications = signal<NotificationResponse[]>([]);
  public readonly isLoading = signal<boolean>(false);
  public readonly loadError = signal<boolean>(false);

  //this will help to calculate the unread notifications from the current notification list
  public readonly unreadCount = computed<number>(() => this.notifications().filter(notification => !notification.isRead).length);

  public ngOnInit(): void {
    this.loadNotifications();
  }

  // remember that backend is always going to give the newest ones first
  public loadNotifications(): void {

    this.isLoading.set(true);
    this.loadError.set(false);

    this.notificationService.getNotifications().subscribe({
      next: (notifications) => { this.notifications.set(notifications); this.isLoading.set(false); },

      error: (error) => {
        console.error( '[NotificationPanelComponent] Failed to load notifications:', error);

        this.notifications.set([]);
        this.loadError.set(true);
        this.isLoading.set(false);
      },
    });
  }

  // marks a single notification as read
  public markAsRead(notification: NotificationResponse): void {

    // don't make another request if it is already read
    if (notification.isRead) {
      return;
    }

    this.notificationService.markAsRead(notification.id).subscribe({
        next: (updatedNotification) => {

          //this will be replacing the old notification with the update
          this.notifications.update(notifications =>
            notifications.map(item => item.id === updatedNotification.id ? updatedNotification : item)
          );
        },

        error: (error) => { console.error('[NotificationPanelComponent] Failed to mark notification as read:', error);
        },
      });
  }

  // marks every notification as read
  public markAllAsRead(): void {

    //if all of them have been read, I don't need a backend request
    if (this.unreadCount() === 0) {
      return;
    }

    this.notificationService.markAllAsRead().subscribe({
        next: () => {

          this.notifications.update(notifications =>
            notifications.map(notification => ({ ...notification, isRead: true, }))
          );
        },

        error: (error) => { console.error( '[NotificationPanelComponent] Failed to mark all notifications as read:', error);
        },
      });
  }

  /*
  - I want a specific icon to be shown for the notification type
  - the type of notification should look different straight from a glance
   */
  public getNotificationIcon(type: string): string {

    switch (type) {

      case 'TIMESHEET_APPROVED':
        return 'fa-solid fa-circle-check';

      case 'TIMESHEET_REJECTED':
        return 'fa-solid fa-circle-xmark';

      case 'TIMESHEET_SUBMITTED':
        return 'fa-solid fa-file-lines';

      case 'TIMER_LONG_RUNNING':
        return 'fa-regular fa-clock';

      default:
        return 'fa-solid fa-bell';
    }
  }

  public getNotificationClass(type: string): string {

    switch (type) {

      case 'TIMESHEET_APPROVED':
        return 'approved';

      case 'TIMESHEET_REJECTED':
        return 'rejected';

      case 'TIMESHEET_SUBMITTED':
        return 'submitted';

      case 'TIMER_LONG_RUNNING':
        return 'timer';

      default:
        return 'default';
    }
  }

  // converts createdAt into things like 5m ago or 2hr ago
  public getTimeAgo(createdAt: string): string {

    const created = new Date(createdAt);
    const now = new Date();

    //calculate the diff since the notification was created so that the user has a clear understanding
    const difference = now.getTime() - created.getTime();

    // make the tome diff into minutes hours and dzaye so that I can display the times properly
    const minutes = Math.floor(difference / 60000);
    const hours = Math.floor(minutes / 60);
    const days = Math.floor(hours / 24);

    if (minutes < 1) {
      return 'Just now';
    }

    if (minutes < 60) {
      return `${minutes}m ago`;
    }

    if (hours < 24) {
      return `${hours}h ago`;
    }

    if (days === 1) {
      return 'Yesterday';
    }

    return `${days}d ago`;
  }

  //adding this for accessibility so that when a user presses the esc key or space then it closes
  @HostListener('document:keydown.escape')
  public closeOnEscape(): void {
    this.closePanel.emit();
  }
}