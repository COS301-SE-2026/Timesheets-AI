/**
 * Author: Lerato Sibanda
 * Date: 2026-07-25
 * Purpose: Display and manage tasks assigned to the current user with filtering and status updates
 * Related Requirement: FR-05 : Task management
 */

import { Component, OnInit, OnDestroy, signal, computed} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Subject } from 'rxjs';
import { RouterLink } from '@angular/router';

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
  assignedWorkspaceMemberId: string;
  dueDate?: string;
  completedAt?: string;
  createdAt: string;
  updatedAt: string;
  parentTaskId?: string;
  isDeleted: boolean;
  deletedAt?: string;
}

// status and priority options as defined in backend

export type TaskStatus = 'TO_DO' | 'IN_PROGRESS' | 'DONE' | 'ARCHIVED';
export type TaskPriority = 'LOW' | 'MEDIUM' | 'HIGH';

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

// Display labels for task statuses

const STATUS_LABELS: Record<TaskStatus, string> = {
  TO_DO: 'To Do',
  IN_PROGRESS: 'In Progress',
  DONE: 'Done',
  ARCHIVED: 'Archived'
} as const;

//Display labels for task priorities 

const PRIORITY_LABELS: Record<TaskPriority, string> = {
    LOW: 'Low',
    MEDIUM: 'Medium',
    HIGH: 'High'
} as const;

const STATUS_CLASSES: Record<TaskStatus, string> = {
  TO_DO : 'status-to-do',
  IN_PROGRESS: 'status-in-progress',
  DONE: 'status-done',
  ARCHIVED: 'status-archived'
} as const;

const PRIORITY_CLASSES: Record<TaskPriority, string> = {
    LOW: 'priority-low',
    MEDIUM: 'priority-medium',
    HIGH: 'priority-high'
};

const PRIORITY_ICONS: Record<TaskPriority, string> = {
  LOW: 'fa-arrow-down',
  MEDIUM: 'fa-minus',
  HIGH: 'fa-arrow-up'
}as const;

@Component({
  selector: 'app-my-tasks',
  imports: [CommonModule, FormsModule],
  templateUrl: './my-tasks.component.html',
  styleUrl: './my-tasks.component.scss',
  standalone: true
})

export class MyTasksComponent implements OnInit, OnDestroy {

  // Component state signal

  public readonly tasks = signal<Task[]>([]);
  public readonly filteredTasks = signal<Task[]>([]);
  public readonly isLoading = signal<boolean>(false);
  public readonly selectedStatus = signal<string>('ALL');
  public readonly showCompleted = signal<boolean>(false);
  public readonly showArchived = signal<boolean>(false);
  public readonly searchQuery = signal<string>('');

  //Computed signals

  // Total number of active tasks

  public readonly activeCount = computed<number>(() => {
    return this.tasks().filter((task: Task) => {
      return task.status === 'TO_DO' || task.status === 'IN_PROGRESS';
    }).length;
  });

  // Total number of completed tasks
  public readonly completedCount = computed<number>(() => {
    return this.tasks().filter((task: Task) => task.status === 'DONE').length;
  });

  // Total number of archived charts
  public readonly archivedCount = computed<number>(() => {
    return this.tasks().filter((task: Task) => task.status === 'ARCHIVED').length;
  });

  // Total number of archived tasks

  public readonly totalCount = computed<number>(() => {
    return this.tasks().length;
  });

  // public constants
  // Status filter options for the filter down

  public readonly statusFilterOptions: StatusFilterOption[] = [
    {value: 'ALL', label: 'All'},
    {value: 'TO_DO', label: 'To Do'},
    {value: 'IN_PROGRESS', label: 'In Progress'},
    {value: 'DONE', label: 'Done'},
    {value: 'ARCHIVED', label: 'Archived'}
  ];

  // Status options for the task status breakdown
  public readonly statusDropdownOptions: StatusDropdownOption[] = [
    {value: 'TO_DO', label: 'To Do'},
    {value: 'IN_PROGRESS', label: 'In Progress'},
    {value: 'DONE', label: 'Done'},
    {value: 'ARCHIVED', label: 'Archived'}
  ];

  public readonly statusLabels = STATUS_LABELS;
  public readonly priorityLabels = PRIORITY_LABELS;
  public readonly priorityIcons = PRIORITY_ICONS;
  private readonly destroy$ = new Subject<void>();

  // INTEGRATION: Inject tASKservices when integrating with backend

  // Initialize component and load tasks

  public ngOnInit(): void {
    this.loadTasks();
  }

  // cleanup subscription when the component is destroyed

  public ngOnDestroy(): void { 
    this.destroy$.next();
    this.destroy$.complete();
  }

  public loadTasks(): void {
    this.isLoading.set(true);

    // INTEGRATION: REPLACE WITH ACTUAL API CALL
    // ADDED MOCK DATA BELOW FOR DEMONSTRATION
    this.loadMockData();
  }

  // apply all active filtes to the task list

  public applyFilters(): void {
    const currentTasks = this.tasks();
    let filtered = [...currentTasks];

    const searchQuery = this.searchQuery().toLowerCase().trim();
      if (searchQuery) {
        filtered = filtered.filter((task: Task) => {
          return task.title.toLowerCase().includes(searchQuery) || task.projectName.toLowerCase().includes(searchQuery) || (task.jiraTicketKey && task.jiraTicketKey.toLowerCase().includes(searchQuery));
        });
      }

    const selectedStatus = this.selectedStatus();
    if(selectedStatus !== 'ALL') {
      filtered = filtered.filter((task: Task) => task.status === selectedStatus);
    }

    if(!this.showCompleted()) {
      filtered = filtered.filter((task: Task) => task.status !== 'DONE');
    }

    if(!this.showArchived()) {
      filtered = filtered.filter((task: Task) => task.status !== 'ARCHIVED');
    }

    this.filteredTasks.set(filtered);
  }

  // Handles search input change

  public onSearchChange(event: Event) : void {
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
// get css class for a task status badge

public getStatusClass(status: TaskStatus):string {
  return `status-badge ${STATUS_CLASSES[status]}`;
}

public getPriorityClass(priority: TaskPriority): string {
  return `priority-badge ${PRIORITY_CLASSES[priority]}`;
}

public onStatusChange(task: Task, newStatus: string) : void {
  const status = newStatus as TaskStatus;

  // INTEGRATION: REPLACE WITH ACTUAL API WHEN INTEGRATING
  // MOCK UPDATE FOR DEMMONSTRATION
  this.updateTaskInTaskList({...task, status});
}

// Navigate to project

public navigateToProject(projectId: string) : void {
  // INTEGRSTION: Implement navigation to project detail
  console.log('Navigate to project:', projectId);
}

// Format date string for display

public formatDate(dateString?: string): string {
  if(!dateString) {
    return '-';
  }
  const date = new Date(dateString);
  return date.toLocaleDateString('en-ZA', {
    day: '2-digit',
    month: 'short',
    year: 'numeric'
  });
}

// Track tasks by their id

public trackByTaskId(index: number, task: Task): string {
  return task.id;
}

public getStatusIcon(status: TaskStatus): string {
  const icons: Record<TaskStatus, string> = {
    TO_DO: 'fa-regular fa-circle',
    IN_PROGRESS:'fa-regular fa-circle-check',
    DONE:'fa-solid fa-check-circle',
    ARCHIVED: 'fa-regular fa-file-zipper'
  };
  return icons[status];
}

// Update task in the task list after status change

private updateTaskInTaskList(updatedTask: Task): void {
  const currentTasks = this.tasks();
  const index = currentTasks.findIndex((task: Task) => task.id === updatedTask.id);
  if (index !== -1){
    const newTasks = [...currentTasks];
    newTasks[index] = updatedTask;
    this.tasks.set(newTasks);
    this.applyFilters();
  }
}

private loadMockData(): void {
  const mockTasks: Task[] = [{
    id: '1',
    title: 'Design login flow',
    description: 'Design the login flow for the application',
    projectName: 'Project Alpha',
    projectId: 'proj-1',
    status: 'IN_PROGRESS',
    priority: 'LOW',
    estimatedHours: 8,
    actualHours: 6,
    jiraTicketKey: 'ALPHA-101',
    assignedToName: 'Thabang Siduke',
    assignedWorkspaceMemberId: 'user-1',
    dueDate: '2026-07-28',
    createdAt: '2026-07-20T10:00:00Z',
    updatedAt: '2026-07-22T14:30:00Z',
    isDeleted: false,
    completedAt: '',
    parentTaskId: ''
  },
  {
    id: '2',
    title: 'Implement OAuth callback',
    description: 'Implement OAuth callback handler',
    projectName: 'Project Beta',
    projectId: 'proj-1',
    status: 'TO_DO',
    priority: 'MEDIUM',
    estimatedHours: 5,
    actualHours: 0,
    jiraTicketKey: 'ALPHA-102',
    assignedToName: 'Thabang Siduke',
    assignedWorkspaceMemberId: 'user-1',
    dueDate: '2026-07-26',
    createdAt: '2026-07-20T10:00:00Z',
    updatedAt: '2026-07-20T10:30:00Z',
    isDeleted: false,
    completedAt: '',
    parentTaskId: ''
  }

];

this.tasks.set(mockTasks);
this.applyFilters();
this.isLoading.set(false);
}

}
