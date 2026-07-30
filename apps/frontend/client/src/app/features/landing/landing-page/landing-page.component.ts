/*
Author: Cleopatra Kwenda
Date: 2026-07-30
Purpose: contains all the necessary data that the website requires.
Related Requirement: N/A
*/

import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { FontAwesomModule } from '@fortawesome/angular-fontaesome'
import {
  faClock,
  faChartLine,
  faPuzzlePiece,
  faShieldHalved
} from '@fortawesome/free-solid-svg-core'

@Component({
  selector: 'app-landing-page',
  standalone:true,
  imports: [CommonModule, RouterLink, FontAwesomModule],
  templateUrl: './landing-page.component.html',
  styleUrl: './landing-page.component.scss'
})
export class LandingPageComponent {
  
  features=[
    {
      title: 'Effortless Tracking',
      description: 'Track time in real-time or log it later-quick, simple, and flexible.',
      bgColor: '#eff6ff',
      iconColor: '#2563eb',
      iconPath: faClock
    },
    {
      title: 'Seamless Integrations',
      description: 'Connect with Jira, GitHub, Outlook Calendar and more tools you already use.',
      bgColor: '#f0fdf4',
      iconColor: '#16a34a',
      iconPath: faPuzzlePiece
    },
    {
      title: 'AI-Powered Insights',
      description: 'Get intelligent analytics and predictive insights that help you work smarter.',
      bgColor: '#fff7ed',
      iconColor: '#ea580c',
      iconPath: faChartLine
    },
    {
      title: 'Accurate & Trusted Data',
      description: 'AI detects anomalies and ensures your time data is accurate and reliable.',
      bgColor: '#faf5ff',
      iconColor: '#9333ea',
      iconPath: faShieldHalved
    },
  ];

}
