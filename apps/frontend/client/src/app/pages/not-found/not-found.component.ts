import { Component } from '@angular/core';
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

  constructor(private readonly location: Location) {}

  goBack() {
    this.location.back();
  }
}