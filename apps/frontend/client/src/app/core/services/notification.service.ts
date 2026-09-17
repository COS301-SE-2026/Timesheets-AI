import { HttpClient } from '@angular/common/http';
import { inject, Injectable, signal } from '@angular/core';
import { Observable, tap } from 'rxjs';

export interface NotificationResponse {
    id: string;
    type: string;
    title: string;
    message: string | null;
    entityType: string | null;
    entityId: string | null;
    isRead: boolean;
    createdAt: string;
}

interface NotificationConfig {
	icon: string;
	cssClass: string;
	route: string[] | null;
}

@Injectable({ providedIn: 'root',})
export class NotificationService {

  private readonly http = inject(HttpClient);
  private readonly baseUrl = '/api/notifications';

  private readonly notificationConfig: Record<string, NotificationConfig> = {

		TIMESHEET_APPROVED: {
			icon: 'fa-solid fa-circle-check',
			cssClass: 'approved',
			route: ['/timesheets'],
		},
		TIMESHEET_REJECTED: {
			icon: 'fa-solid fa-circle-xmark',
			cssClass: 'rejected',
			route: ['/timesheets'],
		},
		TIMESHEET_SUBMITTED: {
			icon: 'fa-solid fa-file-lines',
			cssClass: 'submitted',
			route: ['/timesheets'],
		},
		TIMER_LONG_RUNNING: {
			icon: 'fa-regular fa-clock',
			cssClass: 'timer',
			route: ['/log-time'],
		},
		USER_WAITING_FOR_WORKSPACE: {
			icon: 'fa-solid fa-user-plus',
			cssClass: 'new-user',
			route: ['/team'],
		},
	};

  // shared unread count used by the navbar notification badge
  public readonly unreadCount = signal<number>(0);

  // gets all notifications for the current workspace member
  getNotifications(): Observable<NotificationResponse[]> {
    return this.http.get<NotificationResponse[]>(this.baseUrl);
  }

  // gets unread notifications for the current workspace member
  getUnreadNotifications(): Observable<NotificationResponse[]> {
    return this.http.get<NotificationResponse[]>(`${this.baseUrl}/unread`);
  }

  // gets the unread count and updates the shared badge count
  loadUnreadCount(): void {
    this.http.get<number>(`${this.baseUrl}/unread/count`).subscribe({
        next: (count) => this.unreadCount.set(count),
        error: (error) => console.error('[NotificationService] Failed to load unread count:', error),
      });
  }

  // marks one notification as read
  markAsRead(notificationId: string): Observable<NotificationResponse> {

    return this.http.patch<NotificationResponse>(`${this.baseUrl}/${notificationId}/read`, {})
      .pipe(tap((notification) => {
          if (notification.isRead) {
            this.unreadCount.update(count =>Math.max(0, count - 1));
          }
        })
      );
  }

  markAllAsRead(): Observable<void> {
    return this.http.patch<void>(`${this.baseUrl}/read-all`, {}) .pipe(tap(() => this.unreadCount.set(0)));
  }

  public getNotificationIcon(type: string): string {
		return this.notificationConfig[type]?.icon ?? 'fa-solid fa-bell';
	}

	// gets the CSS class for a notification type
	public getNotificationClass(type: string): string {
		return this.notificationConfig[type]?.cssClass ?? 'default';
	}

	// gets the page that should open when a notification is clicked
	public getNotificationRoute(type: string): string[] | null {
		return this.notificationConfig[type]?.route ?? null;
	}

	// converts createdAt into values such as 5m ago or @h ago
	public getTimeAgo(createdAt: string): string {
		const created = new Date(createdAt);
		const now = new Date();

		const difference = now.getTime() - created.getTime();

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
}