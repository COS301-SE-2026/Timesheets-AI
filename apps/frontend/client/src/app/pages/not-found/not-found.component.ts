import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
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
  
  goBack() {
    window.history.back();
  }
}