import { CommonModule, Location } from '@angular/common';
import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { NotificationResponse, NotificationService, } from '../../../core/services/notification.service';

type NotificationFilter = 'all' | 'unread';

@Component({
	selector: 'app-notifications-page',
	standalone: true,
	imports: [CommonModule],
	templateUrl: './notifications-page.component.html',
	styleUrl: './notifications-page.component.scss',
})

export class NotificationsPageComponent implements OnInit {

	private readonly notificationService = inject(NotificationService);
    private readonly location = inject(Location);

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

			case 'USER_WAITING_FOR_WORKSPACE':
				return 'fa-solid fa-user-plus';

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

			case 'USER_WAITING_FOR_WORKSPACE':
				return 'new-user';

			default:
				return 'default';
		}
	}

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

    public goBack(): void {
        this.location.back();
    }
}