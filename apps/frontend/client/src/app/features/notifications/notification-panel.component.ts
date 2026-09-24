/** 
 * Author: Nyasha 15 September 2026
 * - this will be the panel that shows all the notifications for a user
 * 
 */

import { Component, EventEmitter, HostListener, OnInit, Output, inject, signal, ElementRef,} from '@angular/core';
import { CommonModule } from '@angular/common';
import { NotificationResponse, NotificationService, } from '../../core/services/notification.service';
import { RouterLink, Router } from '@angular/router';

@Component({
  selector: 'app-notification-panel',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './notification-panel.component.html',
  styleUrl: './notification-panel.component.scss',
})

export class NotificationPanelComponent implements OnInit {

  public readonly notificationService = inject(NotificationService);
  private readonly router = inject(Router);
  private readonly elementRef = inject(ElementRef);

  @Output() closePanel = new EventEmitter<void>();

  public readonly notifications = signal<NotificationResponse[]>([]);
  public readonly isLoading = signal<boolean>(false);
  public readonly loadError = signal<boolean>(false);

  //this will help to calculate the unread notifications from the current notification list
  public readonly unreadCount = this.notificationService.unreadCount;

  public ngOnInit(): void {
    this.loadNotifications();
  }

  // remember that backend is always going to give the newest ones first
  public loadNotifications(): void {

    this.isLoading.set(true);
    this.loadError.set(false);

    this.notificationService.getNotifications().subscribe({
      next: (notifications) => { this.notifications.set(notifications.slice(0,4)); this.isLoading.set(false); },

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

  public handleNotificationClick(notification: NotificationResponse): void {

    if (!notification.isRead) {
      this.markAsRead(notification);
    }

    const route = this.notificationService.getNotificationRoute( notification.type, );

		if (route) {
			this.router.navigate(route);
			this.closePanel.emit();
		}
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

  //adding this for accessibility so that when a user presses the esc key or space then it closes
  @HostListener('document:keydown.escape')
  public closeOnEscape(): void {
    this.closePanel.emit();
  }

  // closes the panel when the user clicks outside it
  @HostListener('document:click', ['$event'])
  public closeOnOutsideClick(event: MouseEvent): void {
    const clickedElement = event.target as Node;

    if (!this.elementRef.nativeElement.contains(clickedElement)) {
      this.closePanel.emit();
    }
  }
}