import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ProgressBarComponent } from '../progress-bar/progress-bar.component';
@Component({
  selector: 'app-stats-card',
  standalone: true, 
  imports: [CommonModule, ProgressBarComponent],
  templateUrl: './stats-card.component.html',
  styleUrl: './stats-card.component.scss'
})
export class StatsCardComponent {
  @Input() label = '';
  @Input() value = '';
  @Input() description = '';
  @Input() progress = 0;
} 
