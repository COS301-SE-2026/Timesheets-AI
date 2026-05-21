import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';

import { ButtonComponent } from '../../shared/components/button/button.component';
import { InputFieldComponent } from '../../shared/components/input-field/input-field.component';
import { DropdownComponent } from '../../shared/components/dropdown/dropdown.component';
import { LoaderComponent } from '../../shared/components/loader/loader.component';
import { ProgressBarComponent } from '../../shared/components/progress-bar/progress-bar.component';

@Component({
  selector: 'app-component-showcase',
  standalone: true,
  imports: [
    CommonModule,
    ButtonComponent,
    InputFieldComponent,
    DropdownComponent,
    LoaderComponent,
    ProgressBarComponent
  ],
  templateUrl: './component-showcase.component.html',
  styleUrls: ['./component-showcase.component.scss']
})
export class ComponentShowcaseComponent {}