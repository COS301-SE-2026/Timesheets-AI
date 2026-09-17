import { CommonModule, Location } from '@angular/common';
import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { NotificationResponse, NotificationService, } from '../../../core/services/notification.service';
import { Router } from '@angular/router';

type NotificationFilter = 'all' | 'unread';

@Component({
	selector: 'app-notifications-page',
	standalone: true,
	imports: [CommonModule],
	templateUrl: './notifications-page.component.html',
	styleUrl: './notifications-page.component.scss',
})

export class NotificationsPageComponent implements OnInit {

	public readonly notificationService = inject(NotificationService);
    private readonly location = inject(Location);
    private readonly router = inject(Router);

	public readonly notifications = signal<NotificationResponse[]>([]);
	public readonly isLoading = signal<boolean>(false);
	public readonly loadError = signal<boolean>(false);

	public readonly activeFilter = signal<NotificationFilter>('all');

	public readonly unreadCount = computed<number>(() =>
		this.notifications().filter(notification => !notification.isRead).length
	);

    //not going to require another backend test instead I make a filter here, because we need to not keep making backend calls
	public readonly filteredNotifications = computed<NotificationResponse[]>(() => {
		if (this.activeFilter() === 'unread') {
			return this.notifications().filter(notification => !notification.isRead);
		}

		return this.notifications();
	});

	public ngOnInit(): void {
		this.loadNotifications();
	}

	public loadNotifications(): void {
        
		this.isLoading.set(true);
		this.loadError.set(false);

		this.notificationService.getNotifications().subscribe({

            //since this is the full notification page, I show ALL the notifications unlike the panel
			next: (notifications) => {
				this.notifications.set(notifications);
				this.isLoading.set(false);
			},

			error: (error) => {
				console.error('[NotificationsPageComponent] Failed to load notifications:', error,);

				this.notifications.set([]);
				this.loadError.set(true);
				this.isLoading.set(false);
			},
		});
	}

	public setFilter(filter: NotificationFilter): void {
		this.activeFilter.set(filter);
	}

	public markAsRead(notification: NotificationResponse): void {
		if (notification.isRead) {
			return;
		}

		this.notificationService.markAsRead(notification.id).subscribe({
            //I am only replacing the notification returned by backend instead of reloading the whole notification list, saving response time
			next: (updatedNotification) => {
				this.notifications.update(notifications => notifications.map(item => item.id === updatedNotification.id ? updatedNotification : item));
			},

			error: (error) => {
				console.error('[NotificationsPageComponent] Failed to mark notification as read:', error, );
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
		}
    }

	public markAllAsRead(): void {
		if (this.unreadCount() === 0) {
			return;
		}

		this.notificationService.markAllAsRead().subscribe({
			next: () => {
				this.notifications.update(notifications => notifications.map(notification => ({...notification, isRead: true, })));
			},

			error: (error) => {
				console.error('[NotificationsPageComponent] Failed to mark all notifications as read:', error, );
			},
		});
	}

    public goBack(): void {
        this.location.back();
    }
}