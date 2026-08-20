/*
Handles the timesheet operations agaist timesheet controller, it fetches
the logged in users timesheets and fetching the netries that belong to a specific timesheet,
and the submit/approve/reject workflow actions

Author: Zamokuhle Zwane
Date: 25 July 2026
*/

import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { TimeEntryResponse } from './time-entry.service';

/*
matching TimesheetResponse field for field so its reusable for whatever
else would read a timesheet
*/

export interface TimesheetResponse {
  id: string;
  workspaceMemberId: string;
  periodStart: string;
  periodEnd: string;
  status: 'DRAFT' | 'SUBMITTED' | 'APPROVED' | 'REJECTED'; // matches the timesheets.status CHECK constraint
  submittedAt: string | null;
  approvedAt: string | null;
  approvedByWorkspaceMemberId: string | null;
  rejectedAt: string | null;
  rejectionReason: string | null;
  isLocked: boolean;
  lockedAt: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface RejectRequest{
  reason: string;
}

@Injectable({ providedIn: 'root' })
export class TimesheetService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = '/api/timesheets';

  //this will get every timesheet for the logged in user
  getMyTimesheets(): Observable<TimesheetResponse[]> {
    return this.http
      .get<TimesheetResponse[]>(`${this.baseUrl}/me`)
      .pipe(catchError(this.handleError('getMyTimesheets')));
  }

  //get timesheet status, useful for an "approved only"  filter view later
  getMyTimesheetsByStatus(status: string): Observable<TimesheetResponse[]> {
    return this.http
      .get<TimesheetResponse[]>(`${this.baseUrl}/me/status/${status}`)
      .pipe(catchError(this.handleError('getMyTimesheetsByStatus')));
  }
  //get timesheets by id
  getTimesheetsById(id: string): Observable<TimesheetResponse> {
    return this.http
      .get<TimesheetResponse>(`${this.baseUrl}/${id}`)
      .pipe(catchError(this.handleError('getTimesheetsById')));
  }

  //get entries for timesheet
  getEntriesForTimesheet(id: string): Observable<TimeEntryResponse[]> {
    return this.http
      .get<TimeEntryResponse[]>(`${this.baseUrl}/${id}/entries`)
      .pipe(catchError(this.handleError('getEntriesForTimesheet')));
  }

  //this is a post request to submit timesheets
  submitTimesheet(id: string): Observable<TimesheetResponse> {
    return this.http
      .post<TimesheetResponse>(`${this.baseUrl}/${id}/submit`, null)
      .pipe(catchError(this.handleError('submitTimesheet')));
  }

// all non-draft timesheets in the workspace
getWorkspaceTimesheets(): Observable<TimesheetResponse[]> {
  return this.http
  .get<TimesheetResponse[]>(`${this.baseUrl}/workspace`)
  .pipe(catchError(this.handleError('gerWorkspaceTimesheets')));
}

  // Manager/admin: submitted timesheets awaiting approval
  getPendingWorkspaceTimesheets(): Observable<TimesheetResponse[]> {
    return this.http
    .get<TimesheetResponse[]>(`${this.baseUrl}/workspace/pending`)
    .pipe(catchError(this.handleError('getPendingWorkspaceTimesheets')));
  }

  // Manager/admin: workspace timesheets filtered by status
  getWorkspaceTimesheetByStatus(status: string): Observable<TimesheetResponse[]> {
    return this.http
    .get<TimesheetResponse[]>(`${this.baseUrl}/workspace/status/${status}`)
    .pipe(catchError(this.handleError('getWorkspaceTimesheetByStatus')));
  }

  getReviewTimesheets(filter: 'ALL' | 'SUBMITTED' | 'APPROVED' | 'REJECTED'): Observable<TimesheetResponse[]> {
    if (filter === 'ALL') {
      return this.getWorkspaceTimesheets();
    }
    if( filter === 'SUBMITTED') {
      return this.getPendingWorkspaceTimesheets();
    }
    return this.getWorkspaceTimesheetByStatus(filter);
  }
  
  //a post request for managaer approvals screen, not used on this page yet
  approveTimesheet(id: string): Observable<TimesheetResponse> {
    return this.http
      .post<TimesheetResponse>(`${this.baseUrl}/${id}/approve`, null)
      .pipe(catchError(this.handleError('approveTimesheet')));
  }

  //a post request to reject timesheets by manager, not used on this page yet
  rejectTimesheet(id: string, reason: string): Observable<TimesheetResponse> {
    const request: RejectRequest = { reason };
    return this.http
      .post<TimesheetResponse>(`${this.baseUrl}/${id}/reject`, request)
      .pipe(catchError(this.handleError('rejectTimesheet')));
  }

  private handleError(operation: string) {
    return (error: HttpErrorResponse) => {
      console.error(`[TimesheetService] ${operation} failed:`, {
        status: error.status,
        message: error.message,
        url: error.url,
      });
      return throwError(() => error);
    };
  }
}
