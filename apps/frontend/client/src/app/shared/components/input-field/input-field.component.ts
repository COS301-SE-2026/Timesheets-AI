/**
 * Author: Kgaugelo Matsena
 * Date: 2026-05-15
 * Purpose: Reusable input field component with validation error support.
 * Related Requirement: -
 */

import { Component, input } from '@angular/core';


@Component({
  selector: 'app-input-field',
  standalone: true,
  imports: [],
  templateUrl: './input-field.component.html',
  styleUrl: './input-field.component.scss'
})
export class InputFieldComponent {

  label = input<string>('');

  placeholder = input<string>('');

  error = input<string>('');

  inputId = `input-${crypto.randomUUID()}`;
}