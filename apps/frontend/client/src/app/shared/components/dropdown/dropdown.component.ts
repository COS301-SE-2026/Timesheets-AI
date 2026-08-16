import { Component, input, signal } from '@angular/core';


@Component({
  selector: 'app-dropdown',
  standalone: true,
  imports: [],
  templateUrl: './dropdown.component.html',
  styleUrl: './dropdown.component.scss'
})
export class DropdownComponent {

  options = input<string[]>([]);

  label = input('');

  error = input('');

  disabled = input(false);

  isOpen = signal(false);

  selectedOption = signal('');

  dropdownId = `dropdown-${crypto.randomUUID()}`;

  toggleDropDown() {
    this.isOpen.update(open => !open);
  }

  selectOption(option: string) {
    this.selectedOption.set(option);
    this.isOpen.set(false);
  }
}