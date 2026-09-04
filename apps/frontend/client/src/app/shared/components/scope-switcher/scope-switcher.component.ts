import { Component, EventEmitter, Input, Output } from '@angular/core';

export interface ScopeOption {
  label: string;
  value: string;
}

/**
 * One reusable toggle for: Overall/By-project (developer + manager insights)
 * and the project picker inside the manager's by-project view. Driven purely
 * by @Input() options so it never needs to know about insights specifically.
 */
@Component({
  selector: 'app-scope-switcher',
  standalone: true,
  templateUrl: './scope-switcher.component.html',
  styleUrl: './scope-switcher.component.scss'
})
export class ScopeSwitcherComponent {
  @Input() options: ScopeOption[] = [];
  @Input() value = '';
  @Output() valueChange = new EventEmitter<string>();

  select(v: string): void {
    if (v === this.value) return;
    this.value = v;
    this.valueChange.emit(v);
  }
}
