import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { TimesheetsComponent } from './features/timesheets/timesheets.component';

import {SidebarComponent} from './shared/components/sidebar/sidebar.component';
import { HeaderComponent } from './shared/components/header/header.component';
import { FooterComponent } from './shared/components/footer/footer.component';
import { ButtonComponent } from './shared/components/button/button.component';
import { InputFieldComponent } from './shared/components/input-field/input-field.component';
import { DropdownComponent } from './shared/components/dropdown/dropdown.component';
import { LoaderComponent } from './shared/components/loader/loader.component';
import { ProgressBarComponent } from './shared/components/progress-bar/progress-bar.component';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet, SidebarComponent, HeaderComponent, FooterComponent],
  templateUrl: './app.component.html',
  styleUrl: './app.component.scss'
})
export class AppComponent {
  title = 'client';
}

