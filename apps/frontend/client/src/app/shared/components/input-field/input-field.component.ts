import { Component, input } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-input-field',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './input-field.component.html',
  styleUrl: './input-field.component.scss'
})
export class InputFieldComponent {

  label = input<string>('');

  placeholder = input<string>('');

  error = input<string>('');

  inputId = `input-${crypto.randomUUID()}`;
}