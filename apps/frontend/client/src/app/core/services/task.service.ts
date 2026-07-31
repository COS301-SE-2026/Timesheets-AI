/*
Handles task operations against a task controller

Author: Zamokuhle Zwane
Date: 26 July 2026

patched: Zamokuhle Zwane, 29 July 2026 made a slight change
*/

import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { catchError } from 'rxjs/operators';

//matches the TaskResponse.java fieled for field
export interface TaskResponse {
  id: string;
  projectId: string;
  projectName: string | null;
  jiraTicketKey: string | null;
  parentTaskId: string | null;
  title: string;
  description: string | null;
  status: 'TODO' | 'IN_PROGRESS' | 'DONE' | 'BLOCKED'; // matches the tasks.status CHECK constraint
  priority: 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL'; // matches the tasks.priority CHECK constraint
  estimatedHours: number | null;
  actualHours: number | null;
  assignedToName: string | null;
  assignedWorkspaceMemberId: string | null;
  dueDate: string | null;
  completedAt: string | null;
  isDeleted: boolean;
  deletedAt: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface CreateTaskRequest {
  projectId: string;
  title: string;
  description?: string;
  priority?: string;
  estimatedHours?: number;
  assignedWorkspaceMemberId?: string;
  dueDate?: string;
}

@Injectable({ providedIn: 'root' })
export class TaskService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = '/api/tasks';

  //gets a projects task
  getTasksForProject(projectId: string): Observable<TaskResponse[]> {
    return this.http
      .get<TaskResponse[]>(`${this.baseUrl}/project/${projectId}`)
      .pipe(catchError(this.handleError('getTaskForProject')));
  }

  //gets tasks by id
  getTaskById(taskId: string): Observable<TaskResponse> {
    return this.http
      .get<TaskResponse>(`${this.baseUrl}/${taskId}`)
      .pipe(catchError(this.handleError('getTaskId')));
  }

  //get tasks of logged in use
  getMyTasks(): Observable<TaskResponse[]> {
    return this.http
      .get<TaskResponse[]>(`${this.baseUrl}/my-tasks`)
      .pipe(catchError(this.handleError('getMyTasks')));
  }

  //post request to create a task
  createTask(request: CreateTaskRequest): Observable<TaskResponse> {
    return this.http
      .post<TaskResponse>(this.baseUrl, request)
      .pipe(catchError(this.handleError('createTask')));
  }

  private handleError(operation: string) {
    return (error: HttpErrorResponse) => {
      console.error(`[TaskService] ${operation} failed:`, {
        status: error.status,
        message: error.message,
        url: error.url,
      });
      return throwError(() => error);
    };
  }
}
