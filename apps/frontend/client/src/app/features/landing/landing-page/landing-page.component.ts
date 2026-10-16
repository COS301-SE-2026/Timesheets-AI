/*
Author: Cleopatra Kwenda
Date: 2026-07-30
Purpose: contains all the necessary data that the website requires.
Related Requirement: N/A
*/

import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { HourglassComponent } from '../../../shared/components/hourglass/hourglass.component';


@Component({
  selector: 'app-landing-page',
  standalone: true,
  imports: [CommonModule, RouterLink, HourglassComponent],
  templateUrl: './landing-page.component.html',
  styleUrl: './landing-page.component.scss'
})

export class LandingPageComponent {

  protected readonly brandLogo = '/assets/momently.png';
  activeTab: 'home' | 'help' = 'home';

  clientLogos = [
    { name: 'momentum METROPOLITAN' },
    { name: ' Gaurdrisk ' },
    { name: 'momentum health' },
    { name: 'ER24' },
    { name: 'momentum investments' },
  ];

  features = [
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

  // Capability tags that scroll across the marquee
  featureTags = [
    { label: 'Real-time tracking',   color: '#2563eb', icon: 'fa-solid fa-clock' },
    { label: 'Jira & GitHub sync',   color: '#16a34a', icon: 'fa-solid fa-puzzle-piece' },
    { label: 'AI-powered insights',  color: '#ea580c', icon: 'fa-solid fa-chart-line' },
    { label: 'Predictive forecasts', color: '#9333ea', icon: 'fa-solid fa-telescope' },
    { label: 'Anomaly detection',    color: '#0d9488', icon: 'fa-solid fa-shield-halved' },
    { label: 'Outlook Calendar',     color: '#2563eb', icon: 'fa-solid fa-calendar-days' },
    { label: 'Team capacity',        color: '#ea580c', icon: 'fa-solid fa-users' },
    { label: 'One-click timesheets', color: '#16a34a', icon: 'fa-solid fa-paper-plane' },
  ];

  // Project forecast section — supporting points
  forecastPoints = [
    {
      title: 'Confidence, not guesswork',
      description: 'Every sprint gets a live on-time confidence score based on actual logged hours.',
      icon: 'fa-solid fa-gauge-high'
    },
    {
      title: 'Slip detected early',
      description: 'Momently flags at-risk tickets days before they become a delivery problem.',
      icon: 'fa-solid fa-triangle-exclamation'
    },
    {
      title: 'Act on AI suggestions',
      description: 'Reassignment and scope recommendations show exactly what would move the date.',
      icon: 'fa-solid fa-wand-magic-sparkles'
    },
  ];

  // Project forecast section — sprint effort distribution
  forecastBars = [
    { label: 'Backend',  value: 82 },
    { label: 'Frontend', value: 64 },
    { label: 'QA',       value: 91 },
    { label: 'Design',   value: 45 },
  ];

  // Impact stats band
  impactStats = [
    { value: '18%',   label: 'more focus time per sprint' },
    { value: '3d',    label: 'average slip caught early' },
    { value: '5 min', label: 'to onboard your team' },
    { value: '100%',  label: 'of logged hours accounted for' },
  ];

  // Platform-level FAQs shown on the landing page
  landingFaqs = [
    {
      question: 'What exactly is Momently?',
      answer: 'Momently is an AI-driven time analytics platform built for software development teams. It tracks time against projects and tasks, then turns that data into insight — forecasts, anomaly alerts, and capacity views that help teams deliver more predictably.'
    },
    {
      question: 'Who is Momently built for?',
      answer: 'Development teams, engineering managers and delivery leads who want visibility into where their hours actually go. If your team already lives in Jira, GitHub or Outlook Calendar, Momently slots in without changing how they work.'
    },
    {
      question: 'How is Momently different from a basic timer?',
      answer: 'A timer tells you how long something took. Momently tells you what that means — how confident you should be in a delivery date, which tickets are quietly slipping, and where your team\'s capacity is really being spent.'
    },
    {
      question: 'Do I need to install anything?',
      answer: 'No. Momently runs in the browser and connects to the tools your team already uses. Most teams are logging time within five minutes of creating a workspace.'
    },
    {
      question: 'Is my data safe?',
      answer: 'Yes. Your time data belongs to you and your workspace. We do not sell it, share it with third parties, or use it to train models outside your own workspace analytics.'
    },
    {
      question: 'How much does it cost?',
      answer: 'Momently is free to start — create a workspace, invite your team and see your first forecast without entering payment details. Paid plans for larger teams are available when you need them.'
    },
  ];

  toggleTab(): void {
    this.activeTab = this.activeTab === 'home' ? 'help' : 'home';
  }

  goHome(): void {
    this.activeTab = 'home';
  }

  setActiveTab(tab: 'home' | 'help'): void {
    this.activeTab = tab;
  }

  downloadManual(): void {
    window.open('https://drive.google.com/file/d/18TvwQfwzIs9q5z6C6n2ud_Ffpb0jL1fa/view?usp=drive_link', '_blank', 'noopener, noreferrer');
  }

  watchDemo(): void {
    window.open('https://youtu.be/NED-IekCKc8?si=0AQHgPD3XTn4RCeM', '_blank', 'noopener, noreferrer');
  }
}