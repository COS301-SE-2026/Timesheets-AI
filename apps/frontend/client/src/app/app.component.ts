import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { TimesheetsComponent } from './features/timesheets/timesheets.component';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, TimesheetsComponent],
  templateUrl: './app.component.html',
  styleUrl: './app.component.scss'
})
export class AppComponent {
  title = 'client';
}
