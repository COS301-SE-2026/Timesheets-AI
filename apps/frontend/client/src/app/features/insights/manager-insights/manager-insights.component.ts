/*
Manager Insights page. Full rebuild to match the approved wireframe:
weekly summary hero, project pill row, completion forecast, team hours,
burnout risk, task overview, velocity, project health, github activity,
jira tickets.

Replaces the old overall/by-project scope-switcher version.

Author: Zamokuhle Zwane
Date: 03/09/2026
*/

import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ChartConfiguration } from 'chart.js';

import { InsightCardComponent } from '../../../shared/components/insight-card/insight-card.component';
import { InsightChartComponent, insightChartDefaults, tokenColor } from '../../../shared/components/insight-chart/insight-chart.component';
import { InsightsService } from '../../../core/services/insights.service';
import {
  FlaggedBurnoutMember,
  JiraTypeCount,
  ManagerDashboardResponse,
} from '../models/manager-dashboard.model';

@Component({
  selector: 'app-manager-insights',
  standalone: true,
  imports: [CommonModule, InsightCardComponent, InsightChartComponent],
  templateUrl: './manager-insights.component.html',
  styleUrl: './manager-insights.component.scss',
})
export class ManagerInsightsComponent implements OnInit {
  private readonly insightsService = inject(InsightsService);

  readonly loading = signal(true);
  readonly error = signal<string | null>(null);
  readonly data = signal<ManagerDashboardResponse | null>(null);

  readonly selectedProjectId = signal<string | null>(null); // null = "All My Projects"
  readonly generating = signal(false);
  readonly lastSyncedAt = signal<Date | null>(null);

  readonly weeklySummaryLines = signal<string[]>([]);
  readonly weeklySummaryGenerated = signal(false);
  readonly weeklySummaryLoading = signal(false);
  readonly weeklySummaryError = signal<string | null>(null);

  ngOnInit(): void {
    this.loadDashboard();
  }

  loadDashboard(): void {
    this.loading.set(true);
    this.error.set(null);
    this.insightsService.getManagerDashboard(this.selectedProjectId()).subscribe({
      next: (response) => {
        this.data.set(response);
        this.loading.set(false);
      },
      error: () => {
        this.error.set('Unable to load manager insights right now.');
        this.loading.set(false);
      },
    });
  }

  selectProject(projectId: string | null): void {
    this.selectedProjectId.set(projectId);
    this.loadDashboard();
  }

  onGenerateInsights(): void {
    const currentProject = this.selectedProjectId() ?? this.data()?.managedProjects[0]?.projectId;
    if (!currentProject) return;

    this.generating.set(true);
    this.insightsService.generateInsights(currentProject).subscribe({
      next: (result) => {
        this.lastSyncedAt.set(new Date(result.github.lastSyncedAt));
        this.generating.set(false);
        this.loadDashboard();
      },
      error: () => {
        this.generating.set(false);
      },
    });
  }

  loadWeeklySummary(): void {
    this.weeklySummaryLoading.set(true);
    this.weeklySummaryError.set(null);
    this.insightsService.getTeamWeeklySummary().subscribe({
      next: (result) => {
        this.weeklySummaryLines.set(result.narrative.split('\n').filter((line) => line.trim().length > 0));
        this.weeklySummaryGenerated.set(true);
        this.weeklySummaryLoading.set(false);
      },
      error: () => {
        this.weeklySummaryError.set('Unable to generate the weekly summary right now.');
        this.weeklySummaryLoading.set(false);
      },
    });
  }

  resolveBurnout(member: FlaggedBurnoutMember): void {
    this.insightsService.resolveBurnoutInsight(member.insightId).subscribe({
      next: () => {
        const current = this.data();
        if (!current) return;
        this.data.set({
          ...current,
          flaggedBurnoutMembers: current.flaggedBurnoutMembers.filter(
            (m) => m.insightId !== member.insightId,
          ),
        });
      },
    });
  }

  initialsFor(name: string): string {
    const parts = name.trim().split(/\s+/);
    const first = parts[0]?.[0] ?? '';
    const last = parts.length > 1 ? parts[parts.length - 1][0] : '';
    return (first + last).toUpperCase();
  }

  private readonly avatarPalette = ['#0F4C91', '#E07830', '#3FA34D', '#9B7EDE', '#C94F4F', '#3B8FC9'];
  avatarColorFor(name: string): string {
    let hash = 0;
    for (let i = 0; i < name.length; i++) {
      hash = name.charCodeAt(i) + ((hash << 5) - hash);
    }
    return this.avatarPalette[Math.abs(hash) % this.avatarPalette.length];
  }

  timeAgo(date: Date | null): string {
    if (!date) return 'Not yet synced';
    const minutes = Math.round((Date.now() - date.getTime()) / 60000);
    if (minutes < 1) return 'Just now';
    if (minutes < 60) return `${minutes} min ago`;
    const hours = Math.round(minutes / 60);
    if (hours < 24) return `${hours} hour${hours === 1 ? '' : 's'} ago`;
    return `${Math.round(hours / 24)} day(s) ago`;
  }

  // Project Completion Forecast

  readonly completionChart = computed<ChartConfiguration>(() => {
    const forecast = this.data()?.projectCompletionForecast;
    const points = forecast?.weeklyProgress ?? [];
    const labels = points.map((p) => p.weekLabel);
    const actualValues = points.map((p) => p.percent);

    const forecastLabel = forecast?.forecastCompletionDate
      ? new Date(forecast.forecastCompletionDate).toLocaleDateString('en-US', { month: 'short', day: 'numeric' })
      : null;

    const allLabels = forecastLabel ? [...labels, forecastLabel] : labels;

    return {
      type: 'line',
      data: {
        labels: allLabels,
        datasets: [
          {
            label: 'Actual progress',
            data: [...actualValues, null],
            borderColor: tokenColor('--color-primary'),
            backgroundColor: 'rgba(15,76,145,0.12)',
            fill: true,
            tension: 0.3,
            pointRadius: 3,
          },
          {
            label: 'Forecast',
            data: forecastLabel
              ? [...new Array(allLabels.length - 2).fill(null), actualValues[actualValues.length - 1] ?? 0, 100]
              : [],
            borderColor: tokenColor('--color-secondary'),
            borderDash: [6, 4],
            fill: false,
            tension: 0,
            pointRadius: (ctx: any) => (ctx.dataIndex === allLabels.length - 1 ? 5 : 0),
          },
        ],
      },
      options: {
        ...insightChartDefaults(),
        scales: {
          x: { grid: { display: false }, ticks: { font: { size: 10.5 }, color: tokenColor('--color-text-muted') } },
          y: {
            min: 0,
            max: 100,
            grid: { color: tokenColor('--color-border') },
            ticks: { callback: (v: any) => `${v}%`, font: { size: 10 }, color: tokenColor('--color-text-muted') },
          },
        },
        plugins: { legend: { display: true, position: 'top', align: 'end', labels: { boxWidth: 10, font: { size: 10.5 } } } },
      },
    };
  });

  // Team Logged Hours

  readonly maxLoggedHours = computed(() => {
    const members = this.data()?.teamLoggedHours.members ?? [];
    return Math.max(...members.map((m) => m.hours), 1);
  });

  hoursBarPercent(hours: number): number {
    return (hours / this.maxLoggedHours()) * 100;
  }

  // Task Overview donut

  private readonly taskStatusColors: Record<string, string> = {
    done: '#0F4C91',
    inProgress: '#E07830',
    todo: '#BFD4F4',
    blocked: '#C94F4F',
  };

  readonly taskOverviewChart = computed<ChartConfiguration>(() => {
    const overview = this.data()?.taskOverview;
    if (!overview) return { type: 'doughnut', data: { labels: [], datasets: [] } };

    return {
      type: 'doughnut',
      data: {
        labels: ['Completed', 'In Progress', 'Open', 'Blocked'],
        datasets: [
          {
            data: [overview.done, overview.inProgress, overview.todo, overview.blocked],
            backgroundColor: [
              this.taskStatusColors['done'],
              this.taskStatusColors['inProgress'],
              this.taskStatusColors['todo'],
              this.taskStatusColors['blocked'],
            ],
            borderWidth: 0,
          },
        ],
      },
      options: { responsive: true, maintainAspectRatio: false, cutout: '70%', plugins: { legend: { display: false } } },
    };
  });

  readonly taskOverviewLegend = computed(() => {
    const overview = this.data()?.taskOverview;
    if (!overview) return [];
    const total = overview.total || 1;
    return [
      { label: 'Completed', count: overview.done, percent: Math.round((overview.done / total) * 100), color: this.taskStatusColors['done'] },
      { label: 'In Progress', count: overview.inProgress, percent: Math.round((overview.inProgress / total) * 100), color: this.taskStatusColors['inProgress'] },
      { label: 'Open', count: overview.todo, percent: Math.round((overview.todo / total) * 100), color: this.taskStatusColors['todo'] },
      { label: 'Blocked', count: overview.blocked, percent: Math.round((overview.blocked / total) * 100), color: this.taskStatusColors['blocked'] },
    ];
  });

  // Velocity

  readonly velocityChart = computed<ChartConfiguration>(() => {
    const velocity = this.data()?.velocity;
    const weeks = velocity?.weeks ?? [];
    const average = velocity?.rollingAverage ?? 0;
    const muted = tokenColor('--color-text-muted');
    const border = tokenColor('--color-border');

    return {
      type: 'line',
      data: {
        labels: weeks.map((w) => w.weekLabel),
        datasets: [
          {
            label: 'Tasks completed',
            data: weeks.map((w) => w.completed),
            borderColor: tokenColor('--color-primary'),
            backgroundColor: 'transparent',
            tension: 0.35,
            pointRadius: 3,
          },
          {
            label: 'Average',
            data: weeks.map(() => average),
            borderColor: tokenColor('--color-secondary'),
            borderDash: [6, 4],
            pointRadius: 0,
            fill: false,
          },
        ],
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        plugins: { legend: { display: true, position: 'top', align: 'end', labels: { boxWidth: 10, font: { size: 10.5 }, color: muted } } },
        scales: {
          x: { grid: { display: false }, ticks: { font: { size: 10.5 }, color: muted } },
          y: { grid: { color: border }, ticks: { font: { size: 10 }, color: muted } },
        },
      },
    };
  });

  // Project Health ring

  readonly healthRingCircumference = 2 * Math.PI * 54;

  healthRingOffset(score: number): number {
    return this.healthRingCircumference * (1 - score / 100);
  }

  healthRingColor(status: string): string {
    return { 'On Track': '#3FA34D', 'At Risk': '#E0A930', Behind: '#C94F4F', 'No data': '#9CB9DD' }[status] ?? '#9CB9DD';
  }

  // GitHub Activity — grouped horizontal bar chart, dual x-axis (hours vs commits differ in magnitude)

  readonly githubActivityChart = computed<ChartConfiguration>(() => {
    const members = this.data()?.githubActivityByMember ?? [];
    const border = tokenColor('--color-border');
    const muted = tokenColor('--color-text-muted');

    return {
      type: 'bar',
      data: {
        labels: members.map((m) => m.memberName),
        datasets: [
          {
            label: 'Hours logged',
            data: members.map((m) => m.hoursLogged),
            backgroundColor: tokenColor('--color-primary'),
            xAxisID: 'xHours',
          },
          {
            label: 'Commits',
            data: members.map((m) => m.commitCount),
            backgroundColor: tokenColor('--color-secondary'),
            xAxisID: 'xCommits',
          },
        ],
      },
      options: {
        indexAxis: 'y',
        responsive: true,
        maintainAspectRatio: false,
        plugins: {
          legend: { display: true, position: 'top', align: 'end', labels: { boxWidth: 10, font: { size: 10.5 } } },
        },
        scales: {
          xHours: {
            position: 'bottom',
            grid: { color: border },
            ticks: { font: { size: 10.5 }, color: muted },
            title: { display: true, text: 'Hours', font: { size: 10 }, color: muted },
          },
          xCommits: {
            position: 'top',
            grid: { display: false },
            ticks: { font: { size: 10.5 }, color: muted },
            title: { display: true, text: 'Commits', font: { size: 10 }, color: muted },
          },
          y: {
            grid: { display: false },
            ticks: { font: { size: 11 }, color: muted },
          },
        },
      },
    };
  });

  // Jira Tickets donut — fixed color map (backend returns UPPERCASE issue types) + fixed Bug/Story/Task order

  private readonly jiraTypeColors: Record<string, string> = {
    BUG: '#C94F4F',
    STORY: '#0F4C91',
    TASK: '#E07830',
  };

  private readonly jiraTypeOrder = ['BUG', 'STORY', 'TASK'];

  private orderedJiraTypes(byType: JiraTypeCount[]): JiraTypeCount[] {
    return [...byType].sort(
      (a, b) => this.jiraTypeOrder.indexOf(a.issueType.toUpperCase()) - this.jiraTypeOrder.indexOf(b.issueType.toUpperCase())
    );
  }

  readonly jiraLegend = computed(() => {
    const breakdown = this.data()?.jiraTicketBreakdown;
    return this.orderedJiraTypes(breakdown?.byType ?? []);
  });

  readonly jiraChart = computed<ChartConfiguration>(() => {
    const byType = this.jiraLegend();
    return {
      type: 'doughnut',
      data: {
        labels: byType.map((t) => t.issueType),
        datasets: [
          {
            data: byType.map((t) => t.count),
            backgroundColor: byType.map((t) => this.jiraTypeColors[t.issueType.toUpperCase()] ?? '#9CB9DD'),
            borderWidth: 0,
          },
        ],
      },
      options: { responsive: true, maintainAspectRatio: false, cutout: '70%', plugins: { legend: { display: false } } },
    };
  });

  jiraLegendColor(issueType: string): string {
    return this.jiraTypeColors[issueType?.toUpperCase()] ?? '#9CB9DD';
  }
}