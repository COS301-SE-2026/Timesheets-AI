import { Component, input } from '@angular/core';

@Component({
  selector: 'app-input-field',
  standalone: true,
  templateUrl: './input-field.component.html',
  styleUrl: './input-field.component.scss'
})
export class InputFieldComponent {
  label = input<string>('');
  placeholder = input<string>('');
  error = input<string>('');
}
