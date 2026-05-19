import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { TimesheetsComponent } from './features/timesheets/timesheets.component';
import {SidebarComponent} from './shared/components/sidebar/sidebar.component'

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, TimesheetsComponent, SidebarComponent],
  templateUrl: './app.component.html',
  styleUrl: './app.component.scss'
})
export class AppComponent {
  title = 'client';
}
