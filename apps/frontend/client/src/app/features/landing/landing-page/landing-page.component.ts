/*
Author: Cleopatra Kwenda
Date: 2026-07-30
Purpose: Display all the necessary components using other project models
with only mock that should be eaisly integrated.
Related Requirement: N/A
*/

import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { title } from 'node:process';
@Component({
  selector: 'app-landing-page',
  standalone:true,
  imports: [CommonModule, RouterLink],
  templateUrl: './landing-page.component.html',
  styleUrl: './landing-page.component.scss'
})
export class LandingPageComponent {
  
  features=[
    {
      title: 'Effortless Tracking',
      description: 'Track time in rea'
    }
  ]

}
