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

@Injectable({ providedIn: 'root',})
export class NotificationService {

  private readonly http = inject(HttpClient);
  private readonly baseUrl = '/api/notifications';

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
}