/*
Thuis is the component file for the developers version of the insights

Author: Zamokuhle Zwane
Date: 03/09/2026
*/

import { Component, inject, computed, signal, OnInit } from '@angular/core';
import { ChartConfiguration } from 'chart.js';
import { ActivatedRoute, Router } from '@angular/router';

import { InsightCardComponent } from '../../../shared/components/insight-card/insight-card.component';
import { ScopeSwitcherComponent, ScopeOption } from '../../../shared/components/scope-switcher/scope-switcher.component';
import { InsightChartComponent, insightChartDefaults, tokenColor } from '../../../shared/components/insight-chart/insight-chart.component';
import { InsightsService } from '../../../core/services/insights.service';
import { InsightsAdapterService } from '../services/insights-adapter.service';
import { GithubIntegrationService } from '../../../core/services/github-integration.service';
import { DeveloperInsights } from '../models/insights.model';
import { PersonalInsightsResponse } from '../models/ai-insights.model';

type DevScope = 'overall' | 'project';

@Component({
  selector: 'app-developer-insights',
  standalone: true,
  imports: [InsightCardComponent, ScopeSwitcherComponent, InsightChartComponent],
  templateUrl: './developer-insights.component.html',
  styleUrl: './developer-insights.component.scss'
})
export class DeveloperInsightsComponent implements OnInit {
  private readonly insightsService = inject(InsightsService);
  private readonly adapter = inject(InsightsAdapterService);
  private readonly githubService = inject(GithubIntegrationService);
  readonly personalHours = signal<PersonalInsightsResponse | null>(null);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  readonly githubConnected = signal(false);
  private static readonly PERCENT = 100;

  readonly loading = signal(true);
  readonly error = signal<string | null>(null);
  readonly data = signal<DeveloperInsights | null>(null);
  readonly syncingGitHub = signal(false);

  readonly scopeOptions: ScopeOption[] = [
    { label: 'Overall', value: 'overall' },
    { label: 'By project', value: 'project' }
  ];

  ngOnInit(): void {
    this.loadInsights();
    this.githubService.status().subscribe({
      next: connected => this.githubConnected.set(connected),
      error: () => this.githubConnected.set(false)
    });

    if (this.route.snapshot.queryParamMap.get('github') === 'connected') {
      this.githubConnected.set(true);
      this.syncGitHub();
      this.router.navigate([], { queryParams: {} });
    }
  }

  loadInsights(): void {
  this.loading.set(true);
  const { from, to } = this.currentWeekRange();

  this.insightsService.getAiDashboard().subscribe({
    next: response => {
      const base = this.adapter.toDeveloperInsights(response);
      this.data.set(base);
      this.loading.set(false);

      this.insightsService.getCalendarVsTracked(from, to).subscribe({
        next: cal => this.data.update(d => d && ({
          ...d,
          overall: { ...d.overall, calendarVsTracked: cal }
        })),
        error: () => {}
      });

      this.insightsService.getJiraVsLogged().subscribe({
        next: jira => this.data.update(d => d && ({
          ...d,
          overall: { ...d.overall, jiraVsLogged: jira }
        })),
        error: () => {}
      });
    },
    error: () => {
      this.error.set('unable to load ai insights right now.');
      this.loading.set(false);
    }
  });

  this.insightsService.getInsightsSummary(from, to).subscribe({
    next: summary => this.personalHours.set(summary),
    error: () => this.personalHours.set(null)
  });
}

  private currentWeekRange(): {from: string; to: string }{
    const now = new Date();
    const day = now.getDay();
    const monday = new Date(now);
    monday.setDate(now.getDate() - ((day + 6) % 7));
    const sunday = new Date(monday);
    sunday.setDate(monday.getDate() + 6);
    const fmt = (d: Date) => d.toISOString().slice(0, 10);
    return { from: fmt(monday), to: fmt(sunday) };
  }

  connectGitHub(): void {
    this.githubService.connect().subscribe({
      next: url => window.location.href = url,
      error: () => this.error.set('unable to connect github.')
    });
  }

  syncGitHub(): void {
    this.syncingGitHub.set(true);
    this.githubService.sync().subscribe({
      next: () => {
        this.syncingGitHub.set(false);
        this.loadInsights();
      },
      error: () => {
        this.syncingGitHub.set(false);
        this.error.set('unable to sync github activity.');
      }
    });
  }

  readonly scope = signal<DevScope>('overall');

  setScope(v: string): void {
    this.scope.set(v as DevScope);
  }

  // ---- chart configs, colours resolved from the design tokens at build time ----
  // the bigger of calendar/tracked is the full bar, the other scales against it
  readonly calendarBars = computed(() => {
    const cvt = this.data()?.overall.calendarVsTracked;
    if (!cvt) return { calendar: 0, tracked: 0 };

    const max = Math.max(cvt.calendarHours, cvt.trackedHours);
    // 0 / 0 is NaN and a NaN width silently does nothing, so bail out
    if (max === 0) return { calendar: 0, tracked: 0 };

    const pct = DeveloperInsightsComponent.PERCENT;
    return {
      calendar: (cvt.calendarHours / max) * pct,
      tracked: (cvt.trackedHours / max) * pct,
    };
  });
  
    readonly trendChart = computed<ChartConfiguration>(() => ({
    type: 'line',
    data: {
      labels: this.data()?.overall.trend.labels ?? [],
      datasets: [{
        data: this.data()?.overall.trend.values ?? [],
        borderColor: tokenColor('--color-secondary'),
        backgroundColor: 'rgba(224,120,48,0.12)',
        fill: true,
        tension: 0.35,
        pointRadius: 3,
        pointBackgroundColor: tokenColor('--color-secondary')
      }]
    },
    options: insightChartDefaults()
  }));

  readonly hoursByProjectChart = computed<ChartConfiguration>(() => ({
    type: 'bar',
    data: {
      labels: this.personalHours()?.hoursPerProject.map(p => p.projectName) ?? [],
      datasets: [{
        data: this.personalHours()?.hoursPerProject.map(p => p.hours) ?? [],
        backgroundColor: [tokenColor('--color-primary'), tokenColor('--color-secondary')],
        borderRadius: 6,
        barThickness: 34
      }]
    },
    options: insightChartDefaults()
  }));

  readonly gitEffortChart = computed<ChartConfiguration>(() => {
    const muted = tokenColor('--color-text-muted');
    const border = tokenColor('--color-border');
    const gitEffort = this.data()?.overall.gitEffort;
    return {
      type: 'bar',
      data: {
        labels: gitEffort?.days ?? [],
        datasets: [
          { label: 'Hours logged', data: gitEffort?.hoursLogged ?? [], backgroundColor: tokenColor('--color-primary'), borderRadius: 5, maxBarThickness: 40, yAxisID: 'y' },
          { label: 'Commits', data: gitEffort?.commitCount ?? [], backgroundColor: tokenColor('--color-secondary'), borderRadius: 5, maxBarThickness: 40, yAxisID: 'y1' }
        ]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        plugins: {
          legend: { display: true, position: 'top', align: 'end', labels: { boxWidth: 10, font: { family: 'Inter', size: 11 }, color: muted } }
        },
        scales: {
          x: { grid: { display: false }, ticks: { font: { family: 'Inter', size: 10.5 }, color: muted } },
          y: { position: 'left', grid: { color: border }, ticks: { font: { family: 'Roboto Mono', size: 10 }, color: muted }, title: { display: true, text: 'Hours', font: { family: 'Inter', size: 10 }, color: muted } },
          y1: { position: 'right', grid: { display: false }, ticks: { font: { family: 'Roboto Mono', size: 10 }, color: muted }, title: { display: true, text: 'Commits', font: { family: 'Inter', size: 10 }, color: muted } }
        }
      }
    };
  });
}