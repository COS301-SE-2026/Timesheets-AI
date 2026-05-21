import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';

import {SidebarComponent} from './shared/components/sidebar/sidebar.component';
import { HeaderComponent } from './shared/components/header/header.component';


@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet, SidebarComponent, HeaderComponent],
  templateUrl: './app.component.html',
  styleUrl: './app.component.scss'
})
export class AppComponent {
  title = 'client';
}

