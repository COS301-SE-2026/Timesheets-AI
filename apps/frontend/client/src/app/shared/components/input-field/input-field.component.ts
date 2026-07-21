/**
 * Author: Kgaugelo Matsena
 * Date: 2026-05-15
 * Purpose: Reusable input field component with validation error support.
 * Related Requirement: -
 */

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