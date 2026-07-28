/**
 * Author: Cleopatra Kwenda
 * Date: 2026-07-13
 * Purpose: 404 not found page with navigation back functionality.
 * Related Requirement: -
 */

import { Component, inject } from '@angular/core';
import { RouterModule } from '@angular/router';
import { Location } from '@angular/common';
import { HourglassComponent } from '../../shared/components/hourglass/hourglass.component';

@Component({
  selector: 'app-not-found',
  standalone: true,
  imports: [
    RouterModule,
    HourglassComponent
],
  templateUrl: './not-found.component.html',
  styleUrl: './not-found.component.scss'
})
export class NotFoundComponent {
  private readonly location = inject(Location);

  protected readonly brandLogo = '/assets/momently.png';

  goBack() {
    this.location.back();
  }
}