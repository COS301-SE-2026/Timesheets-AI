/**
 * Author: Lerato Sibanda
 * Date: 2026-07-27
 * Related Requirement: Leave Requests
 */

import { Component, ChangeDetectionStrategy, computed, signal } from '@angular/core';
import { AbstractControl, FormBuilder, ValidationErrors, Validator } from '@angular/forms';

export type LeaveType = 'ANNUAL' | 'SICK' | 'MATERNITY' | 'PATERNITY' | 'FAMILY_RESPONSIBILITY' | 'OTHER' ;
export type LeaveRequestStatus = 'PENDING' | 'APPROVED' | 'REJECTED' | 'CANCELLED' ;

type LeavePageView = 'LIST' | 'CREATE';
type LeaveUiStatus = 'idle' | 'loading' | 'error';

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
  attachment: null;
}

interface LeaveTypeOption {
  value: LeaveType;
  label: string;
  description: string;
  icon: string;
  iconClass: string;
}

interface SummmaryCard {
  label: string;
  value: number;
  icon: string;
  className: string;
}

const LEAVE_TYPE_LABELS: Record<LeaveType, string> = {
  ANNUAL: 'Annual Leave',
  SICK: 'Sick Leave',
  MATERNITY: 'Maternity Leave',
  PATERNITY: 'Paternity Leave',
  FAMILY_RESPONSIBILITY: 'Family Responsibility',
  OTHER: 'Other'
}

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
  imports: [],
  templateUrl: './leave-requests.component.html',
  styleUrl: './leave-requests.component.scss'
})
export class LeaveRequestsComponent {

}
