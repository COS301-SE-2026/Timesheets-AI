/**
 * Author: Lerato Sibanda
 * Date: 2026-07-27
 * Related Requirement: Leave Requests
 */

import { CommonModule } from '@angular/common';
import { Component, ChangeDetectionStrategy, computed, signal } from '@angular/core';
import { AbstractControl, FormBuilder, ReactiveFormsModule, ValidationErrors, Validators } from '@angular/forms';
import { BidiModule } from "@angular/cdk/bidi";

export type LeaveType = 'ANNUAL' | 'SICK' | 'MATERNITY' | 'PATERNITY' | 'FAMILY_RESPONSIBILITY' | 'OTHER';
export type LeaveRequestStatus = 'PENDING' | 'APPROVED' | 'REJECTED' | 'CANCELLED';

type LeavePageView = 'LIST' | 'CREATE';
type LeaveUiState = 'idle' | 'loading' | 'error';

export interface LeaveRequest {
  id: string;
  workspaceMemberId: string;
  leaveType: LeaveType;
  startDate: string;
  endDate: string;
  totalDays: number;
  reason: string | null;
  attachments: string | null;
  status: LeaveRequestStatus;
  approvedByName: string | null;
  memberName: string;
  approvedAt: string | null;
  rejectionReason: string | null;
  availabilityId: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface CreateLeaveRequestPayload {
  leaveType: LeaveType;
  startDate: string;
  endDate: string;
  totalDays: number;
  reason: string;
  attachments: string | null;
}

export interface UpdateLeaveRequestPayload {
  leaveType?: LeaveType,
  startDate?: string,
  endDate?: string,
  totalDays?: number,
  reason?: string,
  attachments?: string | null;
}

export interface CancelLeaveRequestPayload {
  reason: string;
}

interface LeaveTypeOption {
  value: LeaveType;
  label: string;
  description: string;
  icon: string;
}

interface SummaryCard {
  label: string;
  value: number;
  icon: string;
  type: 'pending' | 'approved' | 'rejected' | 'total';
}

const LEAVE_TYPE_LABELS: Record<LeaveType, string> = {
  ANNUAL: 'Annual Leave',
  SICK: 'Sick Leave',
  MATERNITY: 'Maternity Leave',
  PATERNITY: 'Paternity Leave',
  FAMILY_RESPONSIBILITY: 'Family Responsibility',
  OTHER: 'Other'
}

// Status labels
const STATUS_LABELS: Record<LeaveRequestStatus, string> = {
  PENDING: 'Pending',
  APPROVED: 'Approved',
  REJECTED: 'Rejected',
  CANCELLED: 'Cancelled'
};

// Leave types options
const LEAVE_TYPES: LeaveTypeOption[] = [
  {
    value: 'ANNUAL',
    label: 'Annual',
    description: 'Planned time off for rest and recreation.',
    icon: 'fa-solid fa-umbrella-beach',
  },
  {
    value: 'SICK',
    label: 'Sick',
    description: 'Time off due to illness or medical treatment.',
    icon: 'fa-solid fa-briefcase-medical',
  },
  {
    value: 'MATERNITY',
    label: 'Maternity',
    description: 'Leave for pregnancy and child birth.',
    icon: 'fa-solid fa-person-pregnant',
  },
  {
    value: 'PATERNITY',
    label: 'Paternity',
    description: 'Leave for a father during or after child birth.',
    icon: 'fa-solid fa-person',
  },
  {
    value: 'FAMILY_RESPONSIBILITY',
    label: 'Family Responsibility',
    description: 'Time off for family related responsibilities.',
    icon: 'fa-solid fa-people-group',
  },
  {
    value: 'OTHER',
    label: 'Other',
    description: 'Other types of leave not listed above.',
    icon: 'fa-solid fa-ellipsis',
  }
];

// MOCK data for UI
const MOCK_LEAVE_REQUESTS: LeaveRequest[] = [
  {
    id: '12345',
    workspaceMemberId: '67890',
    memberName: 'John Doe',
    leaveType: 'ANNUAL',
    startDate: '2025-06-24',
    endDate: '2025-06-28',
    totalDays: 5,
    reason: 'Annual holiday',
    attachments: null,
    status: 'PENDING',
    approvedByName: null,
    approvedAt: null,
    rejectionReason: null,
    availabilityId: null,
    createdAt: '2025-06-10T09:00:00Z',
    updatedAt: '2025-06-10T09:00:00Z'
  },
  {
    id: '11121',
    workspaceMemberId: '14156',
    memberName: 'John Doe',
    leaveType: 'SICK',
    startDate: '2025-05-10',
    endDate: '2025-05-12',
    totalDays: 3,
    reason: 'Medical recovery.',
    attachments: null,
    status: 'APPROVED',
    approvedByName: 'Jane Smith',
    approvedAt: '2025-05-09T14:30:00Z',
    rejectionReason: null,
    availabilityId: null,
    createdAt: '2025-05-09T08:00:00Z',
    updatedAt: '2025-05-09T14:30:00Z'
  }
];

@Component({
  selector: 'app-leave-requests',
  imports: [CommonModule, ReactiveFormsModule, BidiModule],
  templateUrl: './leave-requests.component.html',
  styleUrl: './leave-requests.component.scss',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush
})

export class LeaveRequestsComponent {
  private readonly formBuilder = new FormBuilder();

  readonly currentView = signal<LeavePageView>('LIST');
  readonly uiState = signal<LeaveUiState>('idle');
  readonly errorMessage = signal<string | null>(null);
  readonly submitMessage = signal<string | null>(null);
  readonly selectedRequest = signal<LeaveRequest | null>(null);
  readonly isDetailsOpen = signal(false);
  readonly editingRequestId = signal<string | null>(null);

  // INTEGRATION: Replace mock data with the response from the leave request GET endpoint
  readonly leaveRequests = signal<LeaveRequest[]>(MOCK_LEAVE_REQUESTS);

  readonly leaveTypeOptions = LEAVE_TYPES;

  readonly leaveRequestForm = this.formBuilder.nonNullable.group(
    {
      leave_type: this.formBuilder.nonNullable.control<LeaveType | ''>('', {
        validators: Validators.required
      }),
      reason: this.formBuilder.nonNullable.control('', {validators: [ Validators.required, Validators.maxLength(500) ] }),
      start_date: this.formBuilder.nonNullable.control('', { validators: [Validators.required]}) ,
      end_date: this.formBuilder.nonNullable.control('', { validators: [Validators.required]}) ,
      total_days:this.formBuilder.nonNullable.control({ value: 0, disabled: true }) 
    },
    {
      validators: [this.dateRangeValidator.bind(this)]
    }
  );

  readonly summaryCards = computed<SummaryCard[]>(() => [
    {
      label: 'Pending',
      value: this.countByStatus('PENDING'),
      icon: 'fa-regular fa-hourglass',
      type: 'pending'
    },
    {
      label: 'Approved',
      value: this.countByStatus('APPROVED'),
      icon: 'fa-regular fa-calendar-check',
      type: 'approved',
    },
    {
      label: 'Rejected',
      value: this.countByStatus('REJECTED'),
      icon: 'fa-regular fa-circle-xmark',
      type: 'rejected'
    },
    {
      label: 'Total requests',
      value: this.leaveRequests().length,
      icon: 'fa-regular fa-circle-check',
      type: 'total'
    }
  ]);

  readonly reasonCount = computed(() =>
    this.leaveRequestForm.controls.reason.value.length);

  constructor() {
    const startDateControl = this.leaveRequestForm.controls.start_date;
    const endDateControl = this.leaveRequestForm.controls.end_date;
    startDateControl.valueChanges.subscribe(() => {
      this.calculateTotalDays();
    });

    endDateControl.valueChanges.subscribe(() => {
      this.calculateTotalDays();
    });
  }

  // View leave request
  showView(view: LeavePageView): void {
    this.currentView.set(view);
    this.errorMessage.set(null);

    if (view === 'CREATE') {
      this.submitMessage.set(null);
      return;
    }
    this.resetForm();
  }

  // Submit leave request function
  submitLeaveRequest(): void {
    this.errorMessage.set(null);
    this.submitMessage.set(null);

    if (this.leaveRequestForm.invalid) {
      this.leaveRequestForm.markAllAsTouched();
      return;
    }

    const formValue = this.leaveRequestForm.getRawValue();

    if (!formValue.leave_type) {
      this.leaveRequestForm.controls.leave_type.markAsTouched();
      return;
    }

    // Leave request payload
    const payload: CreateLeaveRequestPayload = {
      leaveType: formValue.leave_type,
      startDate: formValue.start_date,
      endDate: formValue.end_date,
      totalDays: formValue.total_days,
      reason: formValue.reason.trim(),
      attachments: null
    };

    // INTEGRATION: Send payload to the POST leave request endpoint
    const requestId = this.editingRequestId();

    if(requestId) {
     
      // INTEGRATION:  PATCH /api/leave-request/{id}

      console.log(`patch /api/leave-request/${requestId}`, payload);

      // temporary frontend update
      this.updateTemporaryRequest(requestId, payload);
      this.submitMessage.set('Leave request updated successfully');
    } else {

      // INTEGRATION POST /api/leave-requests
      console.log('POST /api/leave-request', payload);
    }

    this.editingRequestId.set(null);
    this.showView('LIST');
    this.resetForm();
  }

private updateTemporaryRequest(id: string, payload: CreateLeaveRequestPayload): void {
  const updatedAt = new Date().toISOString();

  this.leaveRequests.update(requests => 
    requests.map( request => request.id === id ? {
    ...request,
    leaveType: payload.leaveType,
    startDate: payload.startDate,
    endDate: payload.endDate,
    totalDays: payload.totalDays,
    reason: payload.reason,
    attachments: payload.attachments,
    updatedAt
  } : request));
}

  // View submitted leave request
  viewRequest(request: LeaveRequest): void {
    // INTEGRATION : navigate or display the selected request

    this.selectedRequest.set(request);
    this.isDetailsOpen.set(true);
    // console.log('Selected leave request:', request);
  }

  closeRequestDetails(): void {
    this.isDetailsOpen.set(false);
    this.selectedRequest.set(null);
  }
  canEdit(request: LeaveRequest): boolean {
    return request.status === 'PENDING';
  }

  canCancel(request: LeaveRequest): boolean {
    return request.status === 'PENDING'
  }

  editRequest(request: LeaveRequest): void {

    // INTEGRATION: PATCH /api/leave-request/{id}\

    if (!this.canEdit(request)){
      return;
    }

    this.closeRequestDetails();
    this.editingRequestId.set(request.id);
    
    this.leaveRequestForm.reset({
      leave_type: request.leaveType,
      reason: request.reason ?? '',
      start_date: request.startDate,
      end_date: request.endDate,
      total_days: request.totalDays
    });

    this.currentView.set('CREATE');
    this.errorMessage.set(null);
    this.submitMessage.set(null);

  }

  cancelRequest(request: LeaveRequest): void {

    // integration: POST/API/LEAVE-REQUEST/{ID}/CANCEL

    if(!this.canCancel(request)){
      return;
    }

     this.leaveRequests.update(requests =>
      requests.map(item => item.id === request.id ? {
        ...item,
        status: 'CANCELLED',
        updatedAt: new Date().toISOString()
      }
    :item)
     );
     this.submitMessage.set('Leave request cancelled successfully.')
  }
  
  // invlaid entry
  isInvalid(controlName: keyof typeof this.leaveRequestForm.controls): boolean {
    const control = this.leaveRequestForm.controls[controlName];
    return control.invalid && (control.dirty || control.touched);
  }

  leaveTypeLabel(type: LeaveType): string {
    return LEAVE_TYPE_LABELS[type];
  }

  // returns the display label for leave request status

  statusLabel(status: LeaveRequestStatus): string {
    return STATUS_LABELS[status];
  }

  // Returns the css classes used to style a leave request statys badge
  statusClass(status: LeaveRequestStatus ): string {
    return `status status-${status.toLowerCase()}`;
  }

  // Returns the css class used to display the icon for a leave type
  iconClass(type: LeaveType): string {
    return `leave-icon leave-icon-${type.toLowerCase()}`;
  }

  // Formats a date string into a user friendly display format
  formatDate(date: string): string {
    return new Intl.DateTimeFormat('en-ZA', {
      day: '2-digit',
      month: 'short',
      year: 'numeric'
    }).format(this.toDate(date));
  }

  // Count number of leave requests with the specific status
  private countByStatus(status: LeaveRequestStatus): number {
    return this.leaveRequests().filter(request => request.status === status).length;
  }

  //Calculates and updates the total number of leave days
  private calculateTotalDays(): void {
    const { start_date, end_date} = this.leaveRequestForm.getRawValue();

    if(!start_date || !end_date) {
      this.setTotalDays(0);
      return;
    }

    const startDate = this.toDate(start_date);
    const endDate = this.toDate(end_date);

    const millisecondsPerDay = 86_400_000;

    const totalDays = Math.floor( (endDate.getTime() - startDate.getTime()) / millisecondsPerDay) + 1;

    this.setTotalDays(Math.max(0, totalDays));

    this.leaveRequestForm.updateValueAndValidity({
      emitEvent: false
    });
  }

  // Validates that the end date is not earlier than the start date
  private dateRangeValidator( control: AbstractControl) : ValidationErrors | null {
    const startDate = control.get('start_date')?.value as string;
    const endDate = control.get('end_date')?.value as string;

    if(!startDate || !endDate){
      return null;
    }

    return this.toDate(endDate) >= this.toDate(startDate) ? null : {invalidDateRange: true};
  };

  // Updates the total days filled without triggering additional form events
  private setTotalDays(days: number) : void {
    this.leaveRequestForm.controls.total_days.setValue(days, {
      emitEvent: false
    });
  }

  // Converts a date string into a date object
  private toDate(date: string) : Date {
    return new Date(`${date}T00:00:00`);
  }

  // resets the leave request form to it's default values
  private resetForm(): void {
    this.leaveRequestForm.reset({
      leave_type: '',
      reason: '',
      start_date: '',
      end_date: '',
      total_days: 0
    });

    this.editingRequestId.set(null);
  }

  //  INTEGRATION: remove this and use the respons from the POST request
  // Adds temporary leave request to local list unti backend integration is compplete
  private addTemporaryRequest(payload: CreateLeaveRequestPayload) : void {
    const now = new Date().toISOString();

    const request: LeaveRequest = {
    id: crypto.randomUUID(),
    workspaceMemberId: 'current-member-id',
    memberName: 'Current User',
    leaveType: payload.leaveType,
    startDate: payload.startDate,
    endDate: payload.endDate,
    totalDays: payload.totalDays,
    reason: payload.reason,
    attachments: payload.attachments,
    status: 'PENDING',
    approvedByName: null,
    approvedAt: null,
    rejectionReason: null,
    availabilityId: null,
    createdAt: now,
    updatedAt: now
  };

  this.leaveRequests.update(requests => [
    request, ...requests
  ]);
}
  
}
