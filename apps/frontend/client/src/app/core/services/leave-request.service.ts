/*
Handles leave request operations against the LeaveRequestController, covers everththing controller exposes
create, update, get, approve, reject, cancel so we can drop mock data
*/

import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Observable, throwError, catchError } from 'rxjs';

//matches LeaveRequestResponse.java field for field
export interface LeaveRequestResponse {
  id: string;
  workspaceMemberId: string;
  memberName: string;
  leaveType: string; //ANNUAL|SICK|MATERNITY|PATERNITY|FAMILY_RESPONSIBILTY|OTHER
  startDate: string;
  endDate: string;
  totalDays: number;
  reason: string | null;
  attachments: string | null;
  status: string; //PENDING | APPROVED | REJECTED | CANCELLED
  approvedByName: string | null;
  approvedAt: string | null;
  rejectionReason: string | null;
  availabilityId: string | null;
  createdAt: string;
  updatedAt: string;
}

//matches leaveRequestCreate, all four fields
export interface CreateLeaveRequestRequest {
  leaveType: string;
  startDate: string;
  endDate: string;
  totalDays: number;
  reason?: string;
  attachments?: string|null;
}

//matches leave request update, everything option since its update
export interface UpdateLeaveRequestRequest {
  leaveType?: string;
  startDate?: string;
  endDate?: string;
  totalDays?: number;
  reason?: string;
  attachments?: string|null;
}

@Injectable({ providedIn: 'root' })
export class LeaveRequestService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = '/api/leave-requests';

  //gets the logged in users own leave requests
  getMyLeaveRequest(): Observable<LeaveRequestResponse[]> {
    return this.http
      .get<LeaveRequestResponse[]>(`${this.baseUrl}/my-requests`)
      .pipe(catchError(this.handleError('getMyLeaveRequests')));
  }

  /*
    get leave requests, role based filtering
    */

  getRequestsByStatus(status?: string): Observable<LeaveRequestResponse[]> {
    const url = status
      ? `${this.baseUrl}?status=${encodeURIComponent(status)}`
      : this.baseUrl;

    return this.http
      .get<LeaveRequestResponse[]>(url)
      .pipe(catchError(this.handleError('getRequestsByStatus')));
  }

  //gets a single leave request by id
  getLeaveRequestById(id: string): Observable<LeaveRequestResponse> {
    return this.http
      .get<LeaveRequestResponse>(`${this.baseUrl}/${id}`)
      .pipe(catchError(this.handleError('getLeaveRequestById')));
  }
  //gets leave request within a date range
  getRequestsByDateRange(
    from: string,
    to: string,
  ): Observable<LeaveRequestResponse[]> {
    return this.http
      .get<LeaveRequestResponse[]>(
        `${this.baseUrl}/date-range?from=${from}&to=${to}`,
      )
      .pipe(catchError(this.handleError('getRequestsByDateRange')));
  }

  //creates a new leave request
  createLeaveRequest(
    request: CreateLeaveRequestRequest,
  ): Observable<LeaveRequestResponse> {
    return this.http
      .post<LeaveRequestResponse>(this.baseUrl, request)
      .pipe(catchError(this.handleError('createLeaveRequest')));
  }

  //updates a pending leave request
  updateLeaveRequest(
    id: string,
    request: UpdateLeaveRequestRequest,
  ): Observable<LeaveRequestResponse> {
    return this.http
      .patch<LeaveRequestResponse>(`${this.baseUrl}/${id}`, request)
      .pipe(catchError(this.handleError('updateLeaveRequest')));
  }

  //approves a pending request
  approveLeaveRequest(id: string): Observable<LeaveRequestResponse> {
    return this.http
      .post<LeaveRequestResponse>(`${this.baseUrl}/${id}/approve`, {})
      .pipe(catchError(this.handleError('approveLeaveRequest')));
  }

  //rejects a pending request
  rejectLeaveRequest(
    id: string,
    reason: string,
  ): Observable<LeaveRequestResponse> {
    return this.http
      .post<LeaveRequestResponse>(`${this.baseUrl}/${id}/reject`, { reason })
      .pipe(catchError(this.handleError('rejectLeaveRequest')));
  }
  //cancels the requesters own pending request
  cancelLeaveRequest(
    id: string,
    reason: string,
  ): Observable<LeaveRequestResponse> {
    return this.http
      .post<LeaveRequestResponse>(`${this.baseUrl}/${id}/cancel`, { reason })
      .pipe(catchError(this.handleError('cancelLeaveRequest')));
  }

  private handleError(operation: string) {
    return (error: HttpErrorResponse) => {
      console.error(`[LeaveRequestService] ${operation} failed:`, {
        status: error.status,
        message: error.message,
        url: error.url,
      });
      return throwError(() => error);
    };
  }
}
