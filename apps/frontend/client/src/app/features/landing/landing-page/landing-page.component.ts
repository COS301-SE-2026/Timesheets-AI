/*
Author: Cleopatra Kwenda
Date: 2026-07-30
Purpose: Contains the data and interactions required by the landing page.
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

  // Keep these names only if the corresponding approved brand assets are available.
  clientLogos = [
    { name: 'Momentum Metropolitan' },
    { name: 'Guardrisk' },
    { name: 'Momentum Health' },
    { name: 'ER24' },
    { name: 'Momentum Investments' },
  ];

  features = [
    {
      title: 'Effortless Time Tracking',
      description: 'Track time with a timer or add a manual entry when you need to record work afterwards.',
      bgColor: '#eff6ff',
      iconColor: '#2563eb',
      icon: 'fa-solid fa-clock'
    },
    {
      title: 'Connected Workflows',
      description: 'Bring work context together with integrations such as Jira, GitHub and calendar data where connected.',
      bgColor: '#f0fdf4',
      iconColor: '#16a34a',
      icon: 'fa-solid fa-puzzle-piece'
    },
    {
      title: 'AI-Powered Insights',
      description: 'Use AI-assisted analysis to understand time entries, work patterns and potential delivery risks.',
      bgColor: '#fff7ed',
      iconColor: '#ea580c',
      icon: 'fa-solid fa-chart-line'
    },
    {
      title: 'Timesheet Review',
      description: 'Give managers context to review submitted timesheets and investigate entries that may need attention.',
      bgColor: '#faf5ff',
      iconColor: '#9333ea',
      icon: 'fa-solid fa-shield-halved'
    },
  ];

  // Capability tags that scroll across the marquee.
  featureTags = [
    { label: 'Timer and manual entries', color: '#2563eb', icon: 'fa-solid fa-clock' },
    { label: 'Jira and GitHub context', color: '#16a34a', icon: 'fa-solid fa-puzzle-piece' },
    { label: 'AI-assisted insights', color: '#ea580c', icon: 'fa-solid fa-chart-line' },
    { label: 'Project delivery forecasts', color: '#9333ea', icon: 'fa-solid fa-telescope' },
    { label: 'Timesheet review signals', color: '#0d9488', icon: 'fa-solid fa-shield-halved' },
    { label: 'Calendar context', color: '#2563eb', icon: 'fa-solid fa-calendar-days' },
    { label: 'Team effort visibility', color: '#ea580c', icon: 'fa-solid fa-users' },
    { label: 'Manager approvals', color: '#16a34a', icon: 'fa-solid fa-clipboard-check' },
  ];

  // Project forecast section — supporting points.
  forecastPoints = [
    {
      title: 'Plan with better context',
      description: 'Use logged time and remaining work to understand the current state of a project.',
      icon: 'fa-solid fa-gauge-high'
    },
    {
      title: 'Identify potential delivery risks',
      description: 'Surface work that may need attention before it affects delivery.',
      icon: 'fa-solid fa-triangle-exclamation'
    },
    {
      title: 'Turn insights into action',
      description: 'Use AI-generated guidance to support planning and workload discussions.',
      icon: 'fa-solid fa-wand-magic-sparkles'
    },
  ];

  // Illustrative values used only to style the landing-page forecast preview.
  forecastBars = [
    { label: 'Backend', value: 82 },
    { label: 'Frontend', value: 64 },
    { label: 'QA', value: 91 },
    { label: 'Design', value: 45 },
  ];

  // Describe product capabilities instead of publishing unsupported performance claims.
  impactStats = [
    { value: 'Timer + manual', label: 'ways to record work' },
    { value: 'Jira + GitHub', label: 'connected development context' },
    { value: 'AI-assisted', label: 'timesheet review insights' },
    { value: 'Project-level', label: 'delivery forecasting' },
  ];

  // Platform-level FAQs shown on the landing page.
  landingFaqs = [
    {
      question: 'What exactly is Momently?',
      answer: 'Momently is a time-tracking and time-analytics platform for software development teams. It brings together time entries, project and task context, timesheet review, and AI-assisted insights to help teams understand effort and delivery.'
    },
    {
      question: 'Who is Momently built for?',
      answer: 'Momently is designed for developers who record time against projects and tasks, and managers who review submitted timesheets and need better visibility into team effort and project progress.'
    },
    {
      question: 'How is Momently different from a basic timer?',
      answer: 'A timer records duration. Momently also connects time entries with project and task context, supports manager timesheet review, and provides AI-assisted insights and project delivery forecasting.'
    },
    {
      question: 'Can I record time if I forgot to start the timer?',
      answer: 'Yes. Alongside timer-based tracking, users can add manual time entries to record work after it has happened.'
    },
    {
      question: 'How does timesheet review work?',
      answer: 'Developers can submit their timesheets for review. Managers can examine submitted entries and use available AI-generated signals as supporting context before approving a timesheet or requesting changes.'
    },
    {
      question: 'What do the AI insights and project forecast do?',
      answer: 'AI insights help identify patterns or entries that may need attention. Project forecasting uses available project and time-tracking information to help teams assess delivery progress and potential risks. These insights support decisions rather than replacing human judgement.'
    },
    {
      question: 'Which tools can Momently connect to?',
      answer: 'Momently has integration workflows for tools such as Jira and GitHub, with calendar context also part of the product. The information available depends on which integrations are configured and connected.'
    },
    {
      question: 'Is the landing-page forecast live data?',
      answer: 'No. The forecast and timesheet review panels on this landing page are illustrative previews, not live workspace data or guaranteed outcomes.'
    },
  ];

  setActiveTab(tab: 'home' | 'help'): void {
    this.activeTab = tab;
    window.scrollTo({ top: 0, behavior: 'smooth' });
  }

  goHome(): void {
    this.activeTab = 'home';
    window.scrollTo({ top: 0, behavior: 'smooth' });
  }

  downloadManual(): void {
    window.open(
      'https://drive.google.com/file/d/18TvwQfwzIs9q5z6C6n2ud_Ffpb0jL1fa/view?usp=drive_link',
      '_blank',
      'noopener,noreferrer'
    );
  }

  watchDemo(): void {
    window.open(
      'https://youtu.be/NED-IekCKc8?si=0AQHgPD3XTn4RCeM',
      '_blank',
      'noopener,noreferrer'
    );
  }
}
