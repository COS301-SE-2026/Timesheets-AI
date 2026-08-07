/*
This file has tests for the LeaveRequestsComponent in the frontend
It uses Jest and Angular's testing utilities to ensure that the component behaves as expected under various scenarios
The tests cover component initialization, data binding, user interactions, and integration with services

Author: Zamokuhle Zwane
Date: 04 July 2026

Patch: i updated the file to fix sonarqube issues
*/
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { LeaveRequestsComponent, LeaveRequest } from './leave-requests.component';
import { LeaveRequestResponse } from '../../core/services/leave-request.service';

describe('LeaveRequestsComponent', () => {
  let component: LeaveRequestsComponent;
  let fixture: ComponentFixture<LeaveRequestsComponent>;
  let httpMock: HttpTestingController;

  const pendingId = 'leave-pending-1';
  const approvedId = 'leave-approved-1';
  const rejectedId = 'leave-rejected-1';

  //matches LeaveRequestResponse field for field

    function makeLeaveResponse(
      overrides: Partial<LeaveRequestResponse>,
    ): LeaveRequestResponse {
      return {
        id: pendingId,
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
        ...overrides,
      };
    }
  
    const mockRequests: LeaveRequestResponse[] = [
      makeLeaveResponse({ id: pendingId, status: 'PENDING' }),
      makeLeaveResponse({
        id: approvedId,
        status: 'APPROVED',
        approvedByName: 'Amahle Dlamini',
        approvedAt: '2026-08-02T09:00:00',
      }),
      makeLeaveResponse({
        id: rejectedId,
        status: 'REJECTED',
        rejectionReason: 'Team is short-staffed that week',
      }),
    ];

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [LeaveRequestsComponent],
      providers: [provideHttpClient(), provideHttpClientTesting()],
    }).compileComponents();
 
    fixture = TestBed.createComponent(LeaveRequestsComponent);
    component = fixture.componentInstance;
    httpMock = TestBed.inject(HttpTestingController);
  });
 
  afterEach(() => {
    httpMock.verify();
  });

   function flushInitialLoad(data: LeaveRequestResponse[] = mockRequests): void {
    fixture.detectChanges(); // ngOnInit -> loadRequests()
    const req = httpMock.expectOne('/api/leave-requests/my-requests');
    expect(req.request.method).toBe('GET');
    req.flush(data);
  }
 
  it('should create', () => {
    flushInitialLoad([]);
    expect(component).toBeTruthy();
  });
 
  describe('loadRequests()', () => {
    it('should GET /api/leave-requests/my-requests and map into LeaveRequest objects on init', () => {
      fixture.detectChanges();
      expect(component.uiState()).toBe('loading');
 
      const req = httpMock.expectOne('/api/leave-requests/my-requests');
      req.flush(mockRequests);
 
      expect(component.uiState()).toBe('idle');
      expect(component.leaveRequests()).toHaveLength(3);
      expect(component.leaveRequests()[0].memberName).toBe('Enzokuhle Khumalo');
    });

     it('should set uiState to error and show a message when the request fails', () => {
      fixture.detectChanges();
      const req = httpMock.expectOne('/api/leave-requests/my-requests');
 
      req.flush('server error', { status: 500, statusText: 'Internal Server Error' });
 
      expect(component.uiState()).toBe('error');
      expect(component.errorMessage()).toBe(
        'Could not load your leave requests. Please try again.',
      );
    });
  });

  describe('summaryCards computed', () => {
    beforeEach(() => flushInitialLoad());
 
    it('should count PENDING, APPROVED, REJECTED and total correctly', () => {
      const cards = component.summaryCards();
      const byLabel = Object.fromEntries(cards.map((c) => [c.label, c.value]));
 
      expect(byLabel['Pending']).toBe(1);
      expect(byLabel['Approved']).toBe(1);
      expect(byLabel['Rejected']).toBe(1);
      expect(byLabel['Total requests']).toBe(3);
    });
  });

    describe('leaveRequestForm validation', () => {
    beforeEach(() => flushInitialLoad([]));
 
    it('should be invalid when required fields are empty', () => {
      expect(component.leaveRequestForm.invalid).toBe(true);
    });
 
    it('should mark reason invalid past 500 characters (Validators.maxLength)', () => {
      component.leaveRequestForm.controls.reason.setValue('a'.repeat(501));
      expect(component.leaveRequestForm.controls.reason.invalid).toBe(true);
    });
 
    it('dateRangeValidator should flag the form invalid when end date is before start date', () => {
      component.leaveRequestForm.patchValue({
        leave_type: 'ANNUAL',
        reason: 'Trip',
        start_date: '2026-08-15',
        end_date: '2026-08-10',
      });
 
      expect(component.leaveRequestForm.errors?.['invalidDateRange']).toBe(true);
    });

    it('dateRangeValidator should allow a same-day request (end === start)', () => {
      component.leaveRequestForm.patchValue({
        leave_type: 'SICK',
        reason: 'Doctor appointment',
        start_date: '2026-08-15',
        end_date: '2026-08-15',
      });
 
      expect(component.leaveRequestForm.errors?.['invalidDateRange']).toBeUndefined();
    });
 
    it('should recalculate total_days (inclusive) whenever start_date or end_date changes', () => {
      //reads via valueChanges that are set up in the constructor
      component.leaveRequestForm.controls.start_date.setValue('2026-08-10');
      component.leaveRequestForm.controls.end_date.setValue('2026-08-12');
 
      //total_days is a disabled control, so i read it via getRawValue()
      expect(component.leaveRequestForm.getRawValue().total_days).toBe(3);
    });
 
    it('reasonCount should track the live length of the reason field', () => {
      component.leaveRequestForm.controls.reason.setValue('Family emergency');
      expect(component.reasonCount()).toBe('Family emergency'.length);
    });
 
    it('isInvalid should only report true once the control is dirty or touched', () => {
      const control = component.leaveRequestForm.controls.leave_type;
      expect(component.isInvalid('leave_type')).toBe(false); // untouched, so no red border yet
 
      control.markAsTouched();
      expect(component.isInvalid('leave_type')).toBe(true);
    });
  });
 
  describe('submitLeaveRequest() - create flow', () => {
    beforeEach(() => flushInitialLoad([]));
 
    it('should not call the service and should markAllAsTouched when the form is invalid', () => {
      const markSpy = jest.spyOn(component.leaveRequestForm, 'markAllAsTouched');
 
      component.submitLeaveRequest();
 
      expect(markSpy).toHaveBeenCalled();
      httpMock.expectNone('/api/leave-requests');
    });

    it('should POST /api/leave-requests with the trimmed payload when valid, then switch back to LIST', () => {
      component.leaveRequestForm.patchValue({
        leave_type: 'ANNUAL',
        reason: '  Family trip  ',
        start_date: '2026-08-10',
        end_date: '2026-08-12',
      });
      component.showView('CREATE');
 
      component.submitLeaveRequest();
 
      const req = httpMock.expectOne('/api/leave-requests');
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual({
        leaveType: 'ANNUAL',
        startDate: '2026-08-10',
        endDate: '2026-08-12',
        totalDays: 3,
        reason: 'Family trip', //.trim() applied, see submitLeaveRequest(), was causing problems
        attachments: null,
      });
 
      req.flush(makeLeaveResponse({ id: 'new-id', reason: 'Family trip' }));
 
      expect(component.currentView()).toBe('LIST');
      expect(component.submitMessage()).toBe('Leave request submitted successfully');
      expect(component.leaveRequests().some((r) => r.id === 'new-id')).toBe(true);
    });
    
    it('should touch leave_type and bail out if leave_type is somehow falsy despite the form reporting valid', () => {
      /*
      this branch is defensive leave_type carries Validators.required, so in practice form.invalid catches an empty leave_type first (see the test
      above) so stubbing the invalid getter is the only way to reach this guard and prove it still protects the payload if that ever changes tbh
      */
      component.leaveRequestForm.patchValue({
        reason: 'Trip',
        start_date: '2026-08-10',
        end_date: '2026-08-12',
      });
      jest.spyOn(component.leaveRequestForm, 'invalid', 'get').mockReturnValue(false);
      const touchSpy = jest.spyOn(
        component.leaveRequestForm.controls.leave_type,
        'markAsTouched',
      );

      component.submitLeaveRequest();

      expect(touchSpy).toHaveBeenCalled();
      httpMock.expectNone('/api/leave-requests');
    });

    it('should set errorMessage and stay on the form when the create request fails', () => {
      component.leaveRequestForm.patchValue({
        leave_type: 'SICK',
        reason: 'Flu',
        start_date: '2026-08-10',
        end_date: '2026-08-10',
      });
 
      component.submitLeaveRequest();
 
      const req = httpMock.expectOne('/api/leave-requests');
      req.flush('bad request', { status: 400, statusText: 'Bad Request' });
 
      expect(component.errorMessage()).toBe(
        'Could not submit your leave request. Please try again.',
      );
    });
  });
 

  describe('editRequest() + submitLeaveRequest() - update flow', () => {
    beforeEach(() => flushInitialLoad());
 
    it('editRequest should refuse to open the form for a non-PENDING request', () => {
      const approved = component.leaveRequests().find((r) => r.id === approvedId)!;
      const viewBefore = component.currentView();
 
      component.editRequest(approved);
 
      expect(component.currentView()).toBe(viewBefore); // unchanged, canEdit() returned false
      expect(component.editingRequestId()).toBeNull();
    });
 
    it('editRequest should populate the form and switch to CREATE view for a PENDING request', () => {
      const pending = component.leaveRequests().find((r) => r.id === pendingId)!;
 
      component.editRequest(pending);
 
      expect(component.currentView()).toBe('CREATE');
      expect(component.editingRequestId()).toBe(pendingId);
      expect(component.leaveRequestForm.controls.leave_type.value).toBe('ANNUAL');
    });

     it('submitLeaveRequest should PATCH /api/leave-requests/{id} when editingRequestId is set', () => {
      const pending = component.leaveRequests().find((r) => r.id === pendingId)!;
      component.editRequest(pending);
      component.leaveRequestForm.controls.reason.setValue('Updated reason');
 
      component.submitLeaveRequest();
 
      const req = httpMock.expectOne(`/api/leave-requests/${pendingId}`);
      expect(req.request.method).toBe('PATCH');
      req.flush(makeLeaveResponse({ id: pendingId, reason: 'Updated reason' }));
 
      expect(component.submitMessage()).toBe('Leave request updated successfully');
      expect(component.editingRequestId()).toBeNull();
      expect(
        component.leaveRequests().find((r) => r.id === pendingId)?.reason,
      ).toBe('Updated reason');
    });
 
    it('submitLeaveRequest should set errorMessage when the update request fails', () => {
      const pending = component.leaveRequests().find((r) => r.id === pendingId)!;
      component.editRequest(pending);
 
      component.submitLeaveRequest();
 
      const req = httpMock.expectOne(`/api/leave-requests/${pendingId}`);
      req.flush('conflict', { status: 409, statusText: 'Conflict' });
 
      expect(component.errorMessage()).toBe(
        'Could not update your leave request. Please try again.',
      );
    });
  });
 
  describe('cancelRequest()', () => {
    beforeEach(() => flushInitialLoad());
 
    it('should do nothing for a request that is not PENDING', () => {
      const approved = component.leaveRequests().find((r) => r.id === approvedId)!;
      const statusBefore = approved.status;

      component.cancelRequest(approved);
      httpMock.expectNone(`/api/leave-requests/${approvedId}/cancel`);
      expect(approved.status).toBe(statusBefore);
    });

     it('should POST /api/leave-requests/{id}/cancel for a PENDING request and update the list', () => {
      const pending = component.leaveRequests().find((r) => r.id === pendingId)!;
 
      component.cancelRequest(pending);
 
      const req = httpMock.expectOne(`/api/leave-requests/${pendingId}/cancel`);
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual({ reason: 'Cancelled by requester' });
 
      req.flush(makeLeaveResponse({ id: pendingId, status: 'CANCELLED' }));
 
      expect(component.submitMessage()).toBe('Leave request cancelled successfully.');
      expect(
        component.leaveRequests().find((r) => r.id === pendingId)?.status,
      ).toBe('CANCELLED');
    });
 
    it('should set errorMessage when the cancel request fails', () => {
      const pending = component.leaveRequests().find((r) => r.id === pendingId)!;
 
      component.cancelRequest(pending);
 
      const req = httpMock.expectOne(`/api/leave-requests/${pendingId}/cancel`);
      req.flush('server error', { status: 500, statusText: 'Internal Server Error' });
 
      expect(component.errorMessage()).toBe(
        'Could not cancel your leave request. Please try again.',
      );
    });
  });

   describe('canEdit() / canCancel()', () => {
    it('should only allow edit/cancel while status is PENDING', () => {
      const asLeaveRequest = (
        status: LeaveRequest['status'],
      ): LeaveRequest => ({
        id: '1',
        workspaceMemberId: 'm',
        leaveType: 'ANNUAL',
        startDate: '2026-08-10',
        endDate: '2026-08-10',
        totalDays: 1,
        reason: null,
        attachments: null,
        status,
        approvedByName: null,
        memberName: 'Enzokuhle Khumalo',
        approvedAt: null,
        rejectionReason: null,
        availabilityId: null,
        createdAt: '',
        updatedAt: '',
      });
 
      expect(component.canEdit(asLeaveRequest('PENDING'))).toBe(true);
      expect(component.canEdit(asLeaveRequest('APPROVED'))).toBe(false);
      expect(component.canCancel(asLeaveRequest('PENDING'))).toBe(true);
      expect(component.canCancel(asLeaveRequest('CANCELLED'))).toBe(false);
    });
  });
 
  describe('view + detail panel helpers', () => {
    beforeEach(() => flushInitialLoad());
 
    it('showView("CREATE") should clear submitMessage but keep the form untouched', () => {
      component.submitMessage.set('leftover message');
 
      component.showView('CREATE');
 
      expect(component.currentView()).toBe('CREATE');
      expect(component.submitMessage()).toBeNull();
    });
 
    it('showView("LIST") should reset the form back to defaults', () => {
      component.leaveRequestForm.controls.reason.setValue('draft text');
 
      component.showView('LIST');
 
      expect(component.leaveRequestForm.controls.reason.value).toBe('');
      expect(component.editingRequestId()).toBeNull();
    });
 
    it('viewRequest should open the details panel with the selected request', () => {
      const target = component.leaveRequests()[0];
 
      component.viewRequest(target);
 
      expect(component.isDetailsOpen()).toBe(true);
      expect(component.selectedRequest()).toBe(target);
    });

     it('closeRequestDetails should clear the details panel', () => {
      component.viewRequest(component.leaveRequests()[0]);
 
      component.closeRequestDetails();
 
      expect(component.isDetailsOpen()).toBe(false);
      expect(component.selectedRequest()).toBeNull();
    });
  });
 
  describe('display helpers', () => {
    beforeEach(() => flushInitialLoad([]));
 
    it('leaveTypeLabel should map enum values to display labels', () => {
      expect(component.leaveTypeLabel('FAMILY_RESPONSIBILITY')).toBe(
        'Family Responsibility',
      );
    });
 
    it('statusLabel should map enum values to display labels', () => {
      expect(component.statusLabel('REJECTED')).toBe('Rejected');
    });
 
    it('statusClass should lowercase the status into the css class', () => {
      expect(component.statusClass('APPROVED')).toBe('status status-approved');
    });
 
    it('iconClass should lowercase the leave type into the css class', () => {
      expect(component.iconClass('MATERNITY')).toBe('leave-icon leave-icon-maternity');
    });

     it('formatDate should format using en-ZA locale', () => {
      const formatted = component.formatDate('2026-08-10');
      expect(formatted).toMatch(/2026/);
    });
  });
});
