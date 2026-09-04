/*
this file handles tests for LeaveRequestService. leave-requests.component.spec.ts already exercises getMyLeaveRequest, createLeaveRequest, updateLeaveRequest, and
cancelLeaveRequest indirectly. This file covers the manager-facing endpoints the component doesn't touch yet (getRequestsByStatus, getLeaveRequestById,
getRequestsByDateRange, approveLeaveRequest, rejectLeaveRequest) plus the shared error handler. once again, tryna get coverage up :(

Author: Zamokuhle Zwane
Date: 06 August 2026
*/
import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import {
  HttpTestingController,
  provideHttpClientTesting,
} from '@angular/common/http/testing';
import { LeaveRequestService, LeaveRequestResponse } from './leave-request.service';

describe('LeaveRequestService', () => {
  let service: LeaveRequestService;
  let httpMock: HttpTestingController;
  
  //field for field match
  const mockResponse: LeaveRequestResponse = {
    id: 'leave-1',
    workspaceMemberId: 'member-1',
    memberName: 'Enzokuhle Khumalo',
    leaveType: 'ANNUAL',
    startDate: '2026-08-10',
    endDate: '2026-08-12',
    totalDays: 3,
    reason: 'Family trip',
    attachments: null,
    status: 'PENDING',
    approvedByName: null,
    approvedAt: null,
    rejectionReason: null,
    availabilityId: null,
    createdAt: '2026-08-01T09:00:00',
    updatedAt: '2026-08-01T09:00:00',
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(LeaveRequestService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('getRequestsByStatus should GET /api/leave-requests with no query string when status is omitted', () => {
    service.getRequestsByStatus().subscribe();

    const req = httpMock.expectOne('/api/leave-requests');
    expect(req.request.method).toBe('GET');
    req.flush([mockResponse]);
  });

  it('getRequestsByStatus should GET /api/leave-requests?status={status} when a status is given', () => {
    service.getRequestsByStatus('PENDING').subscribe();

    const req = httpMock.expectOne('/api/leave-requests?status=PENDING');
    expect(req.request.method).toBe('GET');
    req.flush([mockResponse]);
  });

  it('getLeaveRequestById should GET /api/leave-requests/{id}', () => {
    let result: LeaveRequestResponse | undefined;
    service.getLeaveRequestById('leave-1').subscribe((res) => (result = res));

    const req = httpMock.expectOne('/api/leave-requests/leave-1');
    expect(req.request.method).toBe('GET');
    req.flush(mockResponse);

    expect(result).toEqual(mockResponse);
  });

  it('getRequestsByDateRange should GET with from/to query params', () => {
    service.getRequestsByDateRange('2026-08-01', '2026-08-31').subscribe();

    const req = httpMock.expectOne(
      '/api/leave-requests/date-range?from=2026-08-01&to=2026-08-31',
    );
    expect(req.request.method).toBe('GET');
    req.flush([mockResponse]);
  });

  it('approveLeaveRequest should POST an empty body to /api/leave-requests/{id}/approve', () => {
    service.approveLeaveRequest('leave-1').subscribe();

    const req = httpMock.expectOne('/api/leave-requests/leave-1/approve');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({});
    req.flush({ ...mockResponse, status: 'APPROVED' });
  });

  it('rejectLeaveRequest should POST the rejection reason to /api/leave-requests/{id}/reject', () => {
    service.rejectLeaveRequest('leave-1', 'Team is short-staffed').subscribe();

    const req = httpMock.expectOne('/api/leave-requests/leave-1/reject');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ reason: 'Team is short-staffed' });
    req.flush({ ...mockResponse, status: 'REJECTED' });
  });

  it('should log and rethrow via the shared error handler when a request fails', () => {
    const consoleSpy = jest.spyOn(console, 'error').mockImplementation(() => {});
    let caught: unknown;

    service.getLeaveRequestById('missing-id').subscribe({ error: (err) => (caught = err) });

    const req = httpMock.expectOne('/api/leave-requests/missing-id');
    req.flush('not found', { status: 404, statusText: 'Not Found' });

    expect(caught).toBeTruthy();
    expect(consoleSpy).toHaveBeenCalledWith(
      '[LeaveRequestService] getLeaveRequestById failed:',
      expect.objectContaining({ status: 404 }),
    );

    consoleSpy.mockRestore();
  });
});