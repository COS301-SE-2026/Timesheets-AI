import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';

export type StatusType =
  | 'Approved'
  | 'Submitted'
  | 'Rejected'
  | 'Pending'
  | 'Draft'

@Component({
  selector: 'app-status-chip',
  imports: [CommonModule],
  templateUrl: './status-chip.component.html',
  styleUrl: './status-chip.component.scss'
})

export class StatusChipComponent {
  @Input() status: StatusType = 'Pending'
}
