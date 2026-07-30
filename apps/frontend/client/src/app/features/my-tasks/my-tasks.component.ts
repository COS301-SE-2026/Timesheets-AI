/**
 * Author: Lerato Sibanda
 * Date: 2026-07-25
 * Purpose: Display and manage tasks assigned to the current user with filtering and status updates
 * Related Requirement: FR-05 : Task management
 *
 * Patched: Zamokuhle Zwane 29 July 2026
 * Integrated the tasks page with backend and ensured everything matches.
 */

import {
  Component,
  OnInit,
  OnDestroy,
  signal,
  computed,
  inject,
  HostListener,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Subject } from 'rxjs';
import { Router } from '@angular/router';
import { TaskService, TaskResponse } from '../../core/services/task.service';

/**
 * Represents a task assigned to a user
 * Matches the backend API response structure
 */

export interface Task {
  id: string;
  title: string;
  description?: string;
  projectName: string;
  projectId: string;
  status: TaskStatus;
  priority: TaskPriority;
  estimatedHours: number;
  actualHours: number;
  jiraTicketKey?: string;
  assignedToName: string;
  assignedWorkspaceMemberId?: string;
  dueDate?: string;
  completedAt?: string;
  createdAt: string;
  updatedAt: string;
  parentTaskId?: string;
  isDeleted: boolean;
  deletedAt?: string;
}

// status and priority options, matches tasks.status and tasks.priority CHECK constraints
export type TaskStatus = 'TODO' | 'IN_PROGRESS' | 'DONE' | 'BLOCKED';
export type TaskPriority = 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';

// request payload for updating task status

export interface UpdateStatusRequest {
  status: TaskStatus;
}

interface StatusFilterOption {
  value: string;
  label: string;
}

interface StatusDropdownOption {
  value: TaskStatus;
  label: string;
}

//display labels for task statuses
const STATUS_LABELS: Record<TaskStatus, string> = {
  TODO: 'To Do',
  IN_PROGRESS: 'In Progress',
  DONE: 'Done',
  BLOCKED: 'Blocked',
} as const;

//display label for task priorities
const PRIORITY_LABELS: Record<TaskPriority, string> = {
  LOW: 'Low',
  MEDIUM: 'Medium',
  HIGH: 'High',
  CRITICAL: 'Critical',
} as const;

const STATUS_CLASSES: Record<TaskStatus, string> = {
  TODO: 'status-to-do',
  IN_PROGRESS: 'status-in-progress',
  DONE: 'status-done',
  BLOCKED: 'status-archived',
} as const;

const PRIORITY_CLASSES: Record<TaskPriority, string> = {
  LOW: 'priority-low',
  MEDIUM: 'priority-medium',
  HIGH: 'priority-high',
  CRITICAL: 'priority-critical',
};

// font awesome icons, using the free solid/regular sets already loaded for the app
const PRIORITY_ICONS: Record<TaskPriority, string> = {
  LOW: 'fa-arrow-down',
  MEDIUM: 'fa-minus',
  HIGH: 'fa-arrow-up',
  CRITICAL: 'fa-triangle-exclamation',
} as const;

@Component({
  selector: 'app-my-tasks',
  imports: [CommonModule, FormsModule],
  templateUrl: './my-tasks.component.html',
  styleUrl: './my-tasks.component.scss',
  standalone: true,
})
export class MyTasksComponent implements OnInit, OnDestroy {
  //Component state signal
  public readonly tasks = signal<Task[]>([]);
  public readonly filteredTasks = signal<Task[]>([]);
  public readonly isLoading = signal<boolean>(false);
  public readonly loadError = signal<boolean>(false);
  public readonly selectedStatus = signal<string>('ALL');
  public readonly showCompleted = signal<boolean>(false);
  public readonly showArchived = signal<boolean>(false);
  public readonly searchQuery = signal<string>('');
  public readonly selectedTask = signal<Task | null>(null);
  public readonly isDetailOpen = signal<boolean>(false);
  public readonly isDetailLoading = signal<boolean>(false);
  public readonly detailError = signal<string | null>(null);
  private readonly router = inject(Router);
  private readonly taskService = inject(TaskService);

  //Computed signals

  // Total number of active tasks

  public readonly activeCount = computed<number>(() => {
    return this.tasks().filter((task: Task) => {
      return (
        !task.isDeleted &&
        (task.status === 'TODO' || task.status === 'IN_PROGRESS')
      );
    }).length;
  });

  // Total number of completed tasks
  public readonly completedCount = computed<number>(() => {
    return this.tasks().filter(
      (task: Task) => !task.isDeleted && task.status === 'DONE',
    ).length;
  });

  //total number of blocked tasks (was "archived" in the mock, schema only has BLOCKED)
  public readonly archivedCount = computed<number>(() => {
    return this.tasks().filter(
      (task: Task) => !task.isDeleted && task.status === 'BLOCKED',
    ).length;
  });

  //total number of tasks

  public readonly totalCount = computed<number>(() => {
    return this.tasks().filter((task: Task) => !task.isDeleted).length;
  });

  //public constants tsatus filter options for the filter down

  public readonly statusFilterOptions: StatusFilterOption[] = [
    { value: 'ALL', label: 'All' },
    { value: 'TODO', label: 'To Do' },
    { value: 'IN_PROGRESS', label: 'In Progress' },
    { value: 'DONE', label: 'Done' },
    { value: 'BLOCKED', label: 'Blocked' },
  ];

  //status options for the task status breakdown
  public readonly statusDropdownOptions: StatusDropdownOption[] = [
    { value: 'TODO', label: 'To Do' },
    { value: 'IN_PROGRESS', label: 'In Progress' },
    { value: 'DONE', label: 'Done' },
    { value: 'BLOCKED', label: 'Blocked' },
  ];

  public readonly statusLabels = STATUS_LABELS;
  public readonly priorityLabels = PRIORITY_LABELS;
  public readonly priorityIcons = PRIORITY_ICONS;
  private readonly destroy$ = new Subject<void>();

  //Initialise component and load tasks
  public ngOnInit(): void {
    this.loadTasks();
  }

  //cleanup subscription when the component is destroyed
  public ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  // loads the logged in user's tasks from GET /api/tasks/my-tasks
  // TaskService already handles the HttpClient call and logs errors on failure
  public loadTasks(): void {
    this.isLoading.set(true);
    this.loadError.set(false);

    this.taskService.getMyTasks().subscribe({
      next: (responses: TaskResponse[]) => {
        this.tasks.set(responses.map((response) => this.mapToTask(response)));
        this.applyFilters();
        this.isLoading.set(false);
      },
      error: (error) => {
        //task.service.ts already logs the raw HttpErrorResponse (status, message, url) for its own operation label, this log is just the component-level context
        console.error('[MyTasksComponent] failed to load my-tasks:', error);
        this.isLoading.set(false);
        this.loadError.set(true);
      },
    });
  }

  /*
  converts a raw TaskResponse (backend DTO shape) into the Task shape this component
  */
  private mapToTask(response: TaskResponse): Task {
    return {
      id: response.id,
      title: response.title,
      description: response.description ?? undefined,
      projectName: response.projectName ?? 'Unknown Project',
      projectId: response.projectId,
      status: response.status as TaskStatus,
      priority: response.priority as TaskPriority,
      estimatedHours: response.estimatedHours ?? 0,
      actualHours: response.actualHours ?? 0,
      jiraTicketKey: response.jiraTicketKey ?? undefined,
      assignedToName: response.assignedToName ?? 'Unassigned',
      assignedWorkspaceMemberId:
        response.assignedWorkspaceMemberId ?? undefined,
      dueDate: response.dueDate ?? undefined,
      completedAt: response.completedAt ?? undefined,
      createdAt: response.createdAt,
      updatedAt: response.updatedAt,
      parentTaskId: response.parentTaskId ?? undefined,
      isDeleted: response.isDeleted ?? false,
      deletedAt: response.deletedAt ?? undefined,
    };
  }

  // apply all active filters to the task list

  public applyFilters(): void {
    const currentTasks = this.tasks();
    let filtered = [...currentTasks];

    // exclude deleted filters from list
    filtered = filtered.filter((task: Task) => !task.isDeleted);

    const searchQuery = this.searchQuery().toLowerCase().trim();
    if (searchQuery) {
      filtered = filtered.filter((task: Task) => {
        return (
          task.title.toLowerCase().includes(searchQuery) ||
          task.projectName.toLowerCase().includes(searchQuery) ||
          task.jiraTicketKey?.toLowerCase().includes(searchQuery)
        );
      });
    }

    const selectedStatus = this.selectedStatus();
    if (selectedStatus !== 'ALL') {
      filtered = filtered.filter(
        (task: Task) => task.status === selectedStatus,
      );
    } else {
      if (!this.showCompleted()) {
        filtered = filtered.filter((task: Task) => task.status !== 'DONE');
      }

      if (!this.showArchived()) {
        filtered = filtered.filter((task: Task) => task.status !== 'BLOCKED');
      }
    }
    this.filteredTasks.set(filtered);
  }

  // Handles search input change

  public onSearchChange(event: Event): void {
    const inputElement = event.target as HTMLInputElement;
    this.searchQuery.set(inputElement.value);
    this.applyFilters();
  }

  // Handles status filter dropdown change

  public onStatusFilterChange(event: Event): void {
    const selectElement = event.target as HTMLSelectElement;
    this.selectedStatus.set(selectElement.value);
    this.applyFilters();
  }

  // Handles show completed checkbox change

  public onToggleCompleted(event: Event): void {
    const checkbox = event.target as HTMLInputElement;
    this.showCompleted.set(checkbox.checked);
    this.applyFilters();
  }

  // Handles show archived checkbox changes

  public onToggleArchived(event: Event): void {
    const checkbox = event.target as HTMLInputElement;
    this.showArchived.set(checkbox.checked);
    this.applyFilters();
  }
  public onOverlayClick(event: MouseEvent): void {
    if (event.target === event.currentTarget) {
      this.closeTaskDetail();
    }
  }
  // get css class for a task status badge

  public getStatusClass(status: TaskStatus): string {
    return `status-badge ${STATUS_CLASSES[status]}`;
  }

  public getPriorityClass(priority: TaskPriority): string {
    return `priority-badge ${PRIORITY_CLASSES[priority]}`;
  }

  //there's no status field on the GET, i've flagged this with backend, so for now its local
  public onStatusChange(task: Task, newStatus: string): void {
    const status = newStatus as TaskStatus;
    this.updateTaskInTaskList({ ...task, status });
  }

  //navigate to project

  public navigateToProject(projectId: string): void {
    void this.router.navigate(['/projects', projectId]);
  }

  public onOverlayKeydown(event: KeyboardEvent): void {
    if (event.key === 'Enter' || event.key === ' ') {
      event.preventDefault();
      this.closeTaskDetail();
    }
  }

  //format date string for display

  public formatDate(dateString?: string): string {
    if (!dateString) {
      return '-';
    }
    const date = new Date(dateString);

    if (Number.isNaN(date.getTime())) {
      return '-';
    }
    return date.toLocaleDateString('en-ZA', {
      day: '2-digit',
      month: 'short',
      year: 'numeric',
    });
  }

  public formatDateTime(value?: string): string {
    if (!value) {
      return '-';
    }
    const date = new Date(value);
    return date.toLocaleString('en-ZA', {
      day: '2-digit',
      month: 'short',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
    });
  }

  //track tasks by id

  public trackByTaskId(index: number, task: Task): string {
    return task.id;
  }

  public getStatusIcon(status: TaskStatus): string {
    const icons: Record<TaskStatus, string> = {
      TODO: 'fa-regular fa-circle',
      IN_PROGRESS: 'fa-regular fa-circle-check',
      DONE: 'fa-solid fa-check-circle',
      BLOCKED: 'fa-solid fa-ban',
    };
    return icons[status];
  }

  //update tasks in the task list after a status change

  private updateTaskInTaskList(updatedTask: Task): void {
    const currentTasks = this.tasks();
    const index = currentTasks.findIndex(
      (task: Task) => task.id === updatedTask.id,
    );
    if (index !== -1) {
      const newTasks = [...currentTasks];
      newTasks[index] = updatedTask;
      this.tasks.set(newTasks);
      this.applyFilters();
    }
  }

  //opens the task detail modal, wired to GET /api/tasks/{taskId} for the full record
  public onViewTask(task: Task): void {
    this.detailError.set(null);
    this.isDetailOpen.set(true);
    this.isDetailLoading.set(true);

    this.taskService.getTaskById(task.id).subscribe({
      next: (response: TaskResponse) => {
        this.selectedTask.set(this.mapToTask(response));
        this.isDetailLoading.set(false);
      },
      error: (error) => {
        console.error('[MyTasksComponent] failed to load task detail:', error);
        this.detailError.set('Could not load task details, please try again.');
        this.isDetailLoading.set(false);
      },
    });
  }

  public closeTaskDetail(): void {
    this.isDetailOpen.set(false);
    this.selectedTask.set(null);
    this.detailError.set(null);
  }

  //this will close modal on escape
  @HostListener('document:keydown.escape')
  public onEscapeKey(): void {
    if (this.isDetailOpen()) {
      this.closeTaskDetail();
    }
  }
}
