// import { Component, input, signal } from '@angular/core';
// import { CommonModule } from '@angular/common';

// @Component({
//   selector: 'app-dropdown',
//   standalone: true,
//   imports: [CommonModule],
//   templateUrl: './dropdown.component.html',
//   styleUrl: './dropdown.component.scss'
// })
// export class DropdownComponent {
//   //The dropdown receives a list of strings from outside.
//   options = input<string[]>([]);
//   /*
//   dropdown starts CLOSED (false)
//   becomes true when opened
//   */
//   isOpen = signal(false);

//   //What the user selected
//   selectedOption = signal('');

//   // read the current state and flips it 
//   toggleDropDown(){
//     this.isOpen.update(open => !open);
//   }

//   // closes menu after value is set
//   selectOption(option: string){
//     this.selectedOption.set(option);
//     this.isOpen.set(false);

//   }
// }

import { Component, input, signal } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-dropdown',
  standalone: true,
  imports: [CommonModule],
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

  toggleDropDown() {
    this.isOpen.update(open => !open);
  }

  selectOption(option: string) {
    this.selectedOption.set(option);
    this.isOpen.set(false);
  }

}