/**
 * Author: Cleopatra Kwenda
 * Date: 2026-07-13
 * Purpose: 404 not found page with navigation back functionality.
 * Related Requirement: -
 */

import { Component, inject } from '@angular/core';
import { RouterModule } from '@angular/router';
import { Location, CommonModule } from '@angular/common';
import { HourglassComponent } from '../../shared/components/hourglass/hourglass.component';

@Component({
  selector: 'app-not-found',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    HourglassComponent
  ],
  templateUrl: './not-found.component.html',
  styleUrl: './not-found.component.scss'
})
export class NotFoundComponent {
  protected readonly brandLogo = '/assets/momently.png';

  private readonly location =inject(Location) ;

  goBack(): void {
    this.location.back();
  }
}