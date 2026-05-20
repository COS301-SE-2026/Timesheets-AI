import { Component, input } from '@angular/core';

@Component({
  selector: 'app-button',
  imports: [],
  standalone: true,
  templateUrl: './button.component.html',
  styleUrl: './button.component.scss'
})
export class ButtonComponent {
  variant = input<'primary' | 'secondary' | 'tertiary'>('primary');
  disabled = input<boolean>(false);
}
