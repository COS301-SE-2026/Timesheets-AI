/**
 * Autthor: Lerato Sibanda
 * Date: 2026-07-25
 * Purpose: Display and manage tasks assigned to the current user with filtering and status updates
 * Related Requirement: FR-05 : Task management
 */

import { Component, OnInit, OnDestroy, signal, computed, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Subject, takeUntil } from 'rxjs';

/**
 * Represents a task assigned to a user
 * Matches the ackend API response structure
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
  jiraTicketKet?: string;
  assignedToName: string;
  assignedWorkspaceMemberId: string;
  dueDate?: string;
  completedAt: string;
  createdAt: string;
  updatedAt: string;
  parentTaskId: string;
  isDeleted: boolean;
  deletedAt?: string;
}

// status and priority options as defined in backend

export type TaskStatus = 'TO_DO' | 'IN_PROGRESS' | 'DONE' | 'ARCHIVED';
export type TaskPriority = 'LOW' | 'MEDIUM' | 'HIGH';

// reuest payload for updating task status

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

// Dispaly labels for task statuses

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

const PRIORITY_CLASSES: Record<TaskPriority, string> = {
    LOW: 'priority-low',
    MEDIUM: 'priority-medium',
    HIGH: 'priority-high'
} as const;

@Component({
  selector: 'app-my-tasks',
  imports: [CommonModule, FormsModule],
  templateUrl: './my-tasks.component.html',
  styleUrl: './my-tasks.component.scss',
  standalone: true
})

export class MyTasksComponent {

  // Component state signal

  public readonly tasks = signal<Task[]>([]);
  public readonly filteredTasks = signal<Task[]>([]);
  public readonly  isLoading = signal<boolean>(false);
  public readonly selectedStatus = signal<string>('ALL');
  public readonly showCompleted = signal<boolean>(false);
  public readonly showArchived = signal<boolean>(false);

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
  private readonly destroy$ = new Subject<void>();

  // Initialize component and load tasks

  public ngOnInit(): void {
    this.loadTasks();
  }

  // cleanup subscription when the component is destroyed

  public ngOnDestro(): void { 
    this.destroy$.next();
    this.destroy$.complete();
  }

  public loadTasks(): void {
    this.isLoading.set(true);

    // TODO: REPLACE WITH ACTUAL API CALL
    // ADDED MOCK DATA BELOW FOR DEMONSTRATION
    this.loadMockData();
  }

  // apply all active filtes to the task list

  public applyFilters(): void {
    const currentTasks = this.tasks();
    let filtered = [...currentTasks];

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

  // Handles status filter dropdown change

  public onStatusFilterChange(event: Event): void {
    const selectedElement = event.target as HTMLSelectElement;
    this.selectedStatus.set(selectedElement.value);
    this.applyFilters(); 
  }

  // Handles show completed checkbox change

  public onToogleCompleted(event: Event): void {
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




}
