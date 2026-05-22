import { Component } from '@angular/core';
import { RouterOutlet, Router, NavigationEnd } from '@angular/router';
import { filter } from 'rxjs/operators';
import { CommonModule } from '@angular/common';

import { SidebarComponent } from './shared/components/sidebar/sidebar.component';
import { HeaderComponent } from './shared/components/header/header.component';
import { FooterComponent } from './shared/components/footer/footer.component';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [
    RouterOutlet,
    CommonModule,
    SidebarComponent,
    HeaderComponent,
    FooterComponent
  ],
  templateUrl: './app.component.html',
  styleUrl: './app.component.scss'
})
export class AppComponent {
  showSidebar = true;
  showHeader = true;
  showFooter = true;

  private readonly hideLayoutRoutes = ['/login', '/signup', '/not-found'];

  constructor(private readonly router: Router) {
    this.router.events
      .pipe(filter(event => event instanceof NavigationEnd))
      .subscribe(() => {
        const url = this.router.url;

        const isAuthPage = this.hideLayoutRoutes.some(r =>
          url.startsWith(r)
        );

        this.showSidebar = !isAuthPage;
        this.showHeader = !isAuthPage;
        this.showFooter = !isAuthPage;
      });
  }
}