/**
 * Author: Khaugelo Matsena
 * Date: 2026-05-15
 * Purpose: Reusable button component with variant styling support.
 * Related Requirements: -
 */

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
