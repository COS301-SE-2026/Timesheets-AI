/*
Handles project operatioms against the controller, it covers
everthing the controller exposes not just what timesheets needs, 
im just doing it in preparation for projects page

Author: Zamokuhle Zwane
Date: 25 July 2026

Patched: 28 July 2026
the  ProjectDetailResponse was Record<string, unknown>; now properly 
typed to match the real backend
*/

import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { catchError } from 'rxjs/operators';

//matches ProjectResponse field for field

export interface ProjectResponse {
  id: string;
  name: string;
  description: string | null;
  status: 'ACTIVE' | 'ON_HOLD' | 'COMPLETED' | 'ARCHIVED'; //matches the projects.status CHECK constraint
  budgetHours: number | null;
  hourlyRate: number | null;
  budgetCost: number | null;
  startDate: string | null;
  endDate: string | null;
  myRole: 'ADMIN' | 'MANAGER' | 'DEVELOPER'; //matches ProjectResponse.myRole, a WorkspaceRole enum
  createdAt: string;
  updatedAt: string;
}

export interface ProjectMemberInfo {
  workspaceMemberId: string;
  firstName: string;
  lastName: string;
  email: string;
  role: 'ADMIN' | 'MANAGER' | 'DEVELOPER';
  hoursLogged: number;
  joinedAt: string;
}

export interface ProjectDetailResponse {
  id: string;
  name: string;
  description: string | null;
  status: 'ACTIVE' | 'ON_HOLD' | 'COMPLETED' | 'ARCHIVED';
  budgetHours: number | null;
  hourlyRate: number | null;
  budgetCost: number | null;
  totalCost: number | null;
  members: ProjectMemberInfo[];
  hoursLogged: number; //team total for this project
  progressPercentage: number; //hoursLogged / budgetHours * 100, computed backend-side
  createdAt: string;
  updatedAt: string;
}
export interface CreateProjectRequest {
  name: string;
  description?: string;
  status?: string;
  budgetHours?: number;
  hourlyRate?: number;
  budgetCost?: number;
  startDate?: string;
  endDate?: string;
}

@Injectable({ providedIn: 'root' })
export class ProjectService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = '/api/projects';

  //gets projects role based filtering will happen server side
  getProjects(): Observable<ProjectResponse[]> {
    return this.http
      .get<ProjectResponse[]>(this.baseUrl)
      .pipe(catchError(this.handleError('getProjects')));
  }

  //gets the details on the projects
  getProjectDetail(projectId: string): Observable<ProjectDetailResponse> {
    return this.http
      .get<ProjectDetailResponse>(`${this.baseUrl}/${projectId}`)
      .pipe(catchError(this.handleError('getProjectDetail')));
  }

  //create a project, admin and manager only
  createProject(request: CreateProjectRequest): Observable<ProjectResponse> {
    return this.http
      .post<ProjectResponse>(this.baseUrl, request)
      .pipe(catchError(this.handleError('createProject')));
  }

  private handleError(operation: string) {
    return (error: HttpErrorResponse) => {
      console.error(`[ProjectService] ${operation} failed:`, {
        status: error.status,
        message: error.message,
        url: error.url,
      });
      return throwError(() => error);
    };
  }
}
