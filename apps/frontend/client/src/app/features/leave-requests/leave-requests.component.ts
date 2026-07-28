/**
 * Author: Lerato Sibanda
 * Date: 2026-07-27
 * Related Requirement: Leave Requests
 */

import { CommonModule } from '@angular/common';
import { Component, ChangeDetectionStrategy, computed, signal } from '@angular/core';
import { AbstractControl, FormBuilder, ReactiveFormsModule, ValidationErrors, Validators } from '@angular/forms';

export type LeaveType = 'ANNUAL' | 'SICK' | 'MATERNITY' | 'PATERNITY' | 'FAMILY_RESPONSIBILITY' | 'OTHER';
export type LeaveRequestStatus = 'PENDING' | 'APPROVED' | 'REJECTED' | 'CANCELLED';

type LeavePageView = 'LIST' | 'CREATE';
type LeaveUiState = 'idle' | 'loading' | 'error';

export interface LeaveRequest {
  id: string;
  workspace_member_id: string;
  leave_type: LeaveType;
  start_date: string;
  end_date: string;
  total_days: number;
  reason: string | null;
  attachment: unknown[] | null;
  status: LeaveRequestStatus;
  approve_by_workspace_member_id: string | null;
  approved_at: string | null;
  rejection_reason: string | null;
  availability_id: string | null;
  created_at: string;
  updated_at: string;
}

export interface CreateLeaveRequestPayload {
  leave_type: LeaveType;
  start_date: string;
  end_date: string;
  total_days: number;
  reason: string;
  attachments: null;
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
  PENDING: 'pending',
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
    description: 'Leave for pregnancy and childbirth.',
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
    workspace_member_id: '67890',
    leave_type: 'ANNUAL',
    start_date: '2025-06-24',
    end_date: '2025-06-28',
    total_days: 5,
    reason: 'Annual holiday',
    attachment: null,
    status: 'PENDING',
    approve_by_workspace_member_id: null,
    approved_at: null,
    rejection_reason: null,
    availability_id: null,
    created_at: '2025-06-10T09:00:00',
    updated_at: '2025-06-10T09:00:00'
  },
  {
    id: '11121',
    workspace_member_id: '14156',
    leave_type: 'SICK',
    start_date: '2025-05-10',
    end_date: '2025-05-12',
    total_days: 3,
    reason: 'Medical recovery.',
    attachment: null,
    status: 'APPROVED',
    approve_by_workspace_member_id: 'abcde',
    approved_at: '2025-05-09T14:30:00',
    rejection_reason: null,
    availability_id: null,
    created_at: '2025-05-09T08:00:00',
    updated_at: '2025-05-09T14:30:00'
  }
];

@Component({
  selector: 'app-leave-requests',
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './leave-requests.component.html',
  styleUrl: './leave-requests.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})

export class LeaveRequestsComponent {
  private readonly formBuilder = new FormBuilder();

  readonly currentView = signal<LeavePageView>('LIST');
  readonly uiState = signal<LeaveUiState>('idle');
  readonly errorMessage = signal<string | null>(null);
  readonly submitMessage = signal<string | null>(null);

  // INTEGRATION: Replace mock data with the response from the leave request GET endpoint
  readonly leaveRequests = signal<LeaveRequest[]>(MOCK_LEAVE_REQUESTS);

  readonly leaveTypeOptions = LEAVE_TYPES;

  readonly leaveRequestForm = this.formBuilder.nonNullable.group(
    {
      leave_type: this.formBuilder.nonNullable.control<LeaveType | ''>('', {
        validators: Validators.required
      }),
      reason: ['', [Validators.required, Validators.maxLength(500)]],
      start_date: ['', Validators.required],
      end_date: ['', Validators.required],
      total_days: [{ value: 0, disabled: true }]
    },
    {
      validators: this.dateRangeValidator
    }
  );

  readonly summaryCards = computed<SummaryCard[]>(() => [
    {
      label: 'Pending',
      value: this.countByStatus('Pending'),
      icon: 'fa-regular fa-hourglass-half',
      type: 'pending'
    },
    {
      label: 'Approved',
      value: this.countByStatus('APPROVED'),
      icon: 'fa-regular fa-calender-check',
      type: 'approved',
    },
    {
      label: 'Rejected',
      value: this.countByStatus('REJECTED'),
      icon: 'fa-solid fa-ban',
      type: 'rejected'
    },
    {
      label: 'Total requests',
      value: this.leaveRequests().length,
      icon: 'fa-solid fa-circle-notch',
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
  showView(view: LeaveRequestPageView): void {
    this.currentView.set(view);
    this.errorMessage.set(null);

    if (view === 'LIST') {
      this.resetForm();
    }
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

    if (!formValue) {
      return;
    }

    // Leave request payload
    const payload: CreateLeaveRequestPayload = {
      leave_type: formValue.leave_type,
      start_date: formValue.start_date,
      end_date: formValue.end_date,
      total_days: formValue.total_days,
      reason: formValue.reason.trim(),
      attachments: null
    };

    // INTEGRATION: Send payload to the POST leave request endpoint
    console.log('Leave request payload:', payload);

    this.addTemporaryRequest(payload);

    this.submitMessage.set(
      'Leave Request submitted successfully.'
    );

    this.showView('LIST');
  }

  // View submitted leave request
  viewRequest(request: LeaveRequest): void {
    // INTEGRATION : navigate or display the selected request
    console.log('Selected leave request:', request);
  }

  // invlaid entry
  isInvalid(controlName: keyof typeof this.leaveRequestForm.controls): void {
    const control = this.leaveRequestForm.controls[controlName];
    return control.invalid && (control.dirty || control.touched);
  }

  leaveTypeLabel(type: LeaveType): string {
    return (LEAVE_TYPES.find(option => option.value === type)?.label ?? type);
  }



}
