import { Component, Input } from '@angular/core';

@Component({
  selector: 'app-progress-bar',
  standalone: true,
  templateUrl: './progress-bar.component.html',
  styleUrl: './progress-bar.component.scss'
})
export class ProgressBarComponent {
  // if the parent does not pass a value, it will starts at 0%
  // progress = input<number>(0);
  @Input() progress = 0;
}
