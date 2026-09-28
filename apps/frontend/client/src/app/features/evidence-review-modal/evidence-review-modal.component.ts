/*
  This file handles the Evidence Review modal, shows the AI confidence score for a timesheet after the manager clicks "AI Review". matches the two wireframe
  states (low confidence/likely reject, high confidence/likely approve)  
  Author: Zamokuhle Zwane
  Date: 20 September 2026
 */

import { Component, computed, input, output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ManagerAssistantReview } from '../../core/services/timesheet.service';

@Component({
  selector: 'app-evidence-review-modal',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './evidence-review-modal.component.html',
  styleUrl: './evidence-review-modal.component.scss',
})
export class EvidenceReviewModalComponent {
  review = input<ManagerAssistantReview | null>(null);
  loading = input<boolean>(false);
  errorMessage = input<string | null>(null);

  closeModal = output<void>();
  approve = output<void>();
  reject = output<void>();

  readonly isLikelyReject = computed(() => this.review()?.verdict === 'LIKELY_REJECT');
  readonly isLikelyApprove = computed(() => this.review()?.verdict === 'LIKELY_APPROVE');
  readonly isNeedsReview = computed(() => this.review()?.verdict === 'NEEDS_REVIEW');

  readonly shortSummary = computed(() => {
    switch (this.review()?.verdict) {
      case 'LIKELY_REJECT':
        return 'Not enough evidence found to fully support this timesheet.';
      case 'LIKELY_APPROVE':
        return 'Based on matched evidence.';
      default:
        return 'Evidence only partially supports this timesheet.';
    }
  });

  readonly verdictLabel = computed(() => {
    switch (this.review()?.verdict) {
      case 'LIKELY_REJECT':
        return 'Likely reject';
      case 'LIKELY_APPROVE':
        return 'Likely approve';
      default:
        return 'Needs review';
    }
  });

  onClose(): void {
    this.closeModal.emit();
  }

  onApprove(): void {
    this.approve.emit();
  }

  onReject(): void {
    this.reject.emit();
  }
}