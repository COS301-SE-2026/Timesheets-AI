import { Component, signal } from '@angular/core';
import { CommonModule } from '@angular/common';

import { HeaderComponent } from '../../shared/components/header/header.component';
import { ButtonComponent } from '../../shared/components/button/button.component';
import { DropdownComponent } from '../../shared/components/dropdown/dropdown.component';

import { StatsCardComponent } from '../../shared/components/stats-card/stats-card.component';
import { StatusChipComponent } from '../../shared/components/status-chip/status-chip.component';
import { TableCardComponent } from '../../shared/components/table-card/table-card.component';

export type TimesheetStatus =
  | 'SUBMITTED'
  | 'APPROVED'
  | 'REJECTED'
  | 'PENDING'
  | 'DRAFT';

interface Developer {
  id: string;
  name: string;
}

interface Project {
  id: string;
  name: string;
}

interface Task {
  id: string;
  title: string;
}

export interface Timesheet {
  id: string;
  developer: Developer;
  project: Project;
  task: Task;
  date: string;
  durationMinutes: number;
  notes: string;
  status: TimesheetStatus;
  submittedAt: string | null;
}

@Component({
  selector: 'app-timesheets',
  standalone: true,
  imports: [ CommonModule,HeaderComponent,ButtonComponent,DropdownComponent,StatsCardComponent,StatusChipComponent,TableCardComponent],
  templateUrl: './timesheets.component.html',
  styleUrl: './timesheets.component.scss'
})

//mock data used 
export class TimesheetsComponent {
timesheets = signal<Timesheet[]>([
  {
    id: '7b9f8d1c-4a3b-4c5d-9e2f-1a2b3c4d5e6f',
    developer: {
      id: 'd3b07384-d113-4c4e-9c8e-cf7b978d1234',
      name: 'Lerato Sibanda'
    },
    project: {
      id: 'f81d4fae-7dec-11d0-a765-00a0c91e6bf6',
      name: 'Momently Core App'
    },
    task: {
      id: 'a4f3b2c1-d5e6-4a7b-8c9d-0e1f2a3b4c5d',
      title: 'Build time entry form'
    },
    date: '2026-05-13',
    durationMinutes: 150,
    notes: 'Completed validation logic',
    status: 'SUBMITTED',
    submittedAt: '2026-05-13T12:00:00Z'
  },
  {
    id: '9c8b7a6f-5e4d-3c2b-1a0f-9e8d7c6b5a4f',
    developer: {
      id: 'e2a1b0c9-d8e7-f6a5-b4c3-d2e1f0a9b8c7',
      name: 'John Doe'
    },
    project: {
      id: 'a12b3c4d-5e6f-7a8b-9c0d-1e2f3a4b5c6d',
      name: 'Nexus Dashboard'
    },
    task: {
      id: 'b5e4d3c2-a1f0-9e8d-7c6b-5a4f3e2d1c0b',
      title: 'Sidebar redesign'
    },
    date: '2026-05-12',
    durationMinutes: 240,
    notes: 'Updated layout and spacing',
    status: 'APPROVED',
    submittedAt: '2026-05-12T10:00:00Z'
  },
  {
    id: '1a2b3c4d-5e6f-7a8b-9c0d-1e2f3a4b5c6d',
    developer: {
      id: 'e2a1b0c9-d8e7-f6a5-b4c3-d2e1f0a9b8c7',
      name: 'John Doe'
    },
    project: {
      id: 'b23c4d5e-6f7a-8b9c-0d1e-2f3a4b5c6d7e',
      name: 'Apex Analytics Portal'
    },
    task: {
      id: 'c6b5a4f3-e2d1-0b9a-8f7e-6d5c4b3a2f1e',
      title: 'Implement state management'
    },
    date: '2026-05-20',
    durationMinutes: 360,
    notes: 'Configured global slice for timesheet aggregates',
    status: 'APPROVED',
    submittedAt: '2026-05-20T16:30:00Z'
  },
  {
    id: '2f3e4d5c-6b7a-8f9e-0d1c-2b3a4f5e6d7c',
    developer: {
      id: 'e2a1b0c9-d8e7-f6a5-b4c3-d2e1f0a9b8c7',
      name: 'John Doe'
    },
    project: {
      id: 'c34d5e6f-7a8b-9c0d-1e2f-3a4b5c6d7e8f',
      name: 'Quantum CRM'
    },
    task: {
      id: 'd7c6b5a4-f3e2-1d0b-9a8f-7e6d5c4b3a2f',
      title: 'Setup chart configurations'
    },
    date: '2026-05-21',
    durationMinutes: 495,
    notes: 'Integrated donut and weekly trend line graphics',
    status: 'APPROVED',
    submittedAt: '2026-05-21T17:15:00Z'
  },
  {
    id: '3a4b5c6d-7e8f-9a0b-1c2d-3e4f5a6b7c8d',
    developer: {
      id: 'd3b07384-d113-4c4e-9c8e-cf7b978d1234',
      name: 'Lerato Sibanda'
    },
    project: {
      id: 'f81d4fae-7dec-11d0-a765-00a0c91e6bf6',
      name: 'Momently Core App'
    },
    task: {
      id: 'a4f3b2c1-d5e6-4a7b-8c9d-0e1f2a3b4c5d',
      title: 'Build time entry form'
    },
    date: '2026-05-22',
    durationMinutes: 360,
    notes: 'Connected dropdown utility inputs to mock endpoints',
    status: 'APPROVED',
    submittedAt: '2026-05-22T15:00:00Z'
  },
  {
    id: '4f5e6d7c-8b9a-0f1e-2d3c-4b5a6f7e8d9c',
    developer: {
      id: 'd3b07384-d113-4c4e-9c8e-cf7b978d1234',
      name: 'Lerato Sibanda'
    },
    project: {
      id: '0192e384-7bc3-4d4e-aa5c-df7c891e4321',
      name: 'Internal Dev Systems'
    },
    task: {
      id: 'e8d7c6b5-a4f3-2e1d-0b9a-8f7e6d5c4b3a',
      title: 'Sprint planning and alignment'
    },
    date: '2026-05-23',
    durationMinutes: 300,
    notes: 'Aligned timeline schedules for upcoming features',
    status: 'APPROVED',
    submittedAt: '2026-05-23T11:00:00Z'
  },
  {
    id: '5b6a7f8e-9d0c-1b2a-3f4e-5d6c7b8a9f0e',
    developer: {
      id: 'e2a1b0c9-d8e7-f6a5-b4c3-d2e1f0a9b8c7',
      name: 'John Doe'
    },
    project: {
      id: 'd45e6f7a-8b9c-0d1e-2f3a-4b5c6d7e8f9a',
      name: 'Vanguard API Gateway'
    },
    task: {
      id: 'b5e4d3c2-a1f0-9e8d-7c6b-5a4f3e2d1c0b',
      title: 'Sidebar redesign'
    },
    date: '2026-05-24',
    durationMinutes: 360,
    notes: 'Refactored active states to match revised style standards',
    status: 'PENDING',
    submittedAt: '2026-05-24T14:20:00Z'
  },
  {
    id: '6c7b8a9f-0e1d-2b3a-4f5e-6d7c8b9a0f1e',
    developer: {
      id: 'd3b07384-d113-4c4e-9c8e-cf7b978d1234',
      name: 'Lerato Sibanda'
    },
    project: {
      id: 'f81d4fae-7dec-11d0-a765-00a0c91e6bf6',
      name: 'Momently Core App'
    },
    task: {
      id: 'f9e8d7c6-b5a4-3f2e-1d0b-9a8f7e6d5c4b',
      title: 'Fix responsive grid rendering issues'
    },
    date: '2026-05-25',
    durationMinutes: 60,
    notes: 'Adjusted overflow properties on container sub-wrappers',
    status: 'DRAFT',
    submittedAt: null
  }
]);


  createTimesheet(): void {
    console.log('Create timesheet clicked');
  }

  getHours(minutes: number): number {
    return +(minutes / 60).toFixed(2);
  }

  trackById(index: number, item: Timesheet): string {
    return item.id;
  }

getStatusType(
  status: TimesheetStatus
): 'Approved' | 'Submitted' | 'Rejected' | 'Pending' | 'Draft' {

  switch (status) {
    case 'APPROVED':
      return 'Approved';

    case 'SUBMITTED':
      return 'Submitted';

    case 'REJECTED':
      return 'Rejected';

    case 'PENDING':
      return 'Pending'; 

    case 'DRAFT':
      return 'Draft';
  }
}

}