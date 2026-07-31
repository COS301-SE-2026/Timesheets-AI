/*
Author: Cleopatra Kwenda
Date: 2026-07-30
Purpose: contains all the necessary data that the website requires.
Related Requirement: N/A
*/

import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-landing-page',
  standalone:true,
  imports: [CommonModule, RouterLink],
  templateUrl: './landing-page.component.html',
  styleUrl: './landing-page.component.scss'
})

export class LandingPageComponent {
  
  protected readonly brandLogo= '/assets/momently.png';

  clientLogos=[
    {
      name: 'momentum METROPOLITAN'
    },
    {
      name: ' Gaurdrisk '
    },
    {
      name: 'momentum health'
    },
    {
      name: 'ER24'
    },
    {
      name: 'momentum investments'
    },
  ];

  features=[
    {
      title: 'Effortless Tracking',
      description: 'Track time in real-time or log it later-quick, simple, and flexible.',
      bgColor: '#eff6ff',
      iconColor: '#2563eb',
      icon: 'fa-solid fa-clock'
    },
    {
      title: 'Seamless Integrations',
      description: 'Connect with Jira, GitHub, Outlook Calendar and more tools you already use.',
      bgColor: '#f0fdf4',
      iconColor: '#16a34a',
      icon: 'fa-solid fa-puzzle-piece'
    },
    {
      title: 'AI-Powered Insights',
      description: 'Get intelligent analytics and predictive insights that help you work smarter.',
      bgColor: '#fff7ed',
      iconColor: '#ea580c',
      icon: 'fa-solid fa-chart-line'
    },
    {
      title: 'Accurate & Trusted Data',
      description: 'AI detects anomalies and ensures your time data is accurate and reliable.',
      bgColor: '#faf5ff',
      iconColor: '#9333ea',
      icon: 'fa-solid fa-shield-halved'
    },
  ];

}
