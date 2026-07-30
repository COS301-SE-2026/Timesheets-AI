/**
 * Author: Lerato Sibanda
 * Date: 2026-07-13
 * Purpose: Root application component that manages layout visibilitybased on route.
 * Related Requirement: FR-01 - Application layout
 */

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
  isFullscreenPage = false;

  private readonly hideLayoutRoutes = ['/login', '/signup', '/not-found'];

  constructor(private readonly router: Router) {
    this.updateLayout(this.router.url);
    
    this.router.events
      .pipe(filter(event => event instanceof NavigationEnd))
      .subscribe(() => {
        this.updateLayout(this.router.url); 
      });
    }


    private updateLayout(url: string) : void {
      const isLandingPage = url ==='/';

      const isFullscreenPage = 
        isLandingPage ||
        this.hideLayoutRoutes.some(r => url.startsWith(r));

      this.showSidebar = !isFullscreenPage;
      this.showHeader = !isFullscreenPage;
      this.showFooter = !isFullscreenPage;
      this.isFullscreenPage = isFullscreenPage;

  }
}