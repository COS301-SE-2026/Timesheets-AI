/*
Developer Insights page redesign 

Author: Zamokuhle Zwane
Date: 26/09/2026
*/

import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ChartConfiguration } from 'chart.js';

import { InsightCardComponent } from '../../../shared/components/insight-card/insight-card.component';
import { InsightChartComponent, insightChartDefaults, tokenColor } from '../../../shared/components/insight-chart/insight-chart.component';
import { AuthService } from '../../../core/services/auth.service';
import { InsightsService } from '../../../core/services/insights.service';
import {
  AiDashboardResponse,
  DeveloperProject,
  JiraTicketsBreakdown,
  PeriodOption,
  ScoreCard,
} from '../models/ai-insights.model';

type Tab = 'overview' | 'weekly-summary';

@Component({
  selector: 'app-developer-insights',
  standalone: true,
  imports: [CommonModule, InsightCardComponent, InsightChartComponent],
  templateUrl: './developer-insights.component.html',
  styleUrl: './developer-insights.component.scss',
})
export class DeveloperInsightsComponent implements OnInit {
  private readonly insightsService = inject(InsightsService);
  private readonly authService = inject(AuthService);

  readonly loading = signal(true);
  readonly error = signal<string | null>(null);
  readonly dashboard = signal<AiDashboardResponse | null>(null);
  readonly myProjects = signal<DeveloperProject[]>([]);
  readonly jiraBreakdown = signal<JiraTicketsBreakdown | null>(null);

  readonly period = signal<PeriodOption>('8w');
  readonly activeTab = signal<Tab>('overview');

  readonly weeklySummaryNarrative = signal<string | null>(null);
  readonly weeklySummaryLoading = signal(false);
  readonly weeklySummaryError = signal<string | null>(null);

  readonly periodOptions: { label: string; value: PeriodOption }[] = [
    { label: 'Last 4 weeks', value: '4w' },
    { label: 'Last 8 weeks', value: '8w' },
    { label: 'Last 12 weeks', value: '12w' },
  ];

  ngOnInit(): void {
    this.loadDashboard();
    this.loadMyProjects();
    this.loadJiraBreakdown();
  }

  loadDashboard(): void {
    this.loading.set(true);
    this.error.set(null);
    this.insightsService.getAiDashboard(this.period()).subscribe({
      next: (response) => {
        this.dashboard.set(response);
        this.loading.set(false);
      },
      error: () => {
        this.error.set('Unable to load insights right now.');
        this.loading.set(false);
      },
    });
  }

  loadMyProjects(): void {
    this.insightsService.getMyProjects().subscribe({
      next: (projects) => this.myProjects.set(projects),
      error: () => this.myProjects.set([]),
    });
  }

  loadJiraBreakdown(): void {
    this.insightsService.getJiraTicketsBreakdown().subscribe({
      next: (breakdown) => this.jiraBreakdown.set(breakdown),
      error: () => this.jiraBreakdown.set(null),
    });
  }

  onPeriodChange(event: Event): void {
    const select = event.target as HTMLSelectElement;
    this.period.set(select.value as PeriodOption);
    this.loadDashboard();
  }

  setTab(tab: Tab): void {
    this.activeTab.set(tab);
    if (tab === 'weekly-summary' && this.weeklySummaryNarrative() === null) {
      this.loadWeeklySummary();
    }
  }

  loadWeeklySummary(): void {
  this.weeklySummaryLoading.set(true);
  this.weeklySummaryError.set(null);

  this.insightsService.getCurrentWorkspaceMemberId().subscribe({
    next: ({ workspaceMemberId }) => {
      this.insightsService.generateWeeklySummary(workspaceMemberId, this.currentWeekStart()).subscribe({
        next: (result) => {
          this.weeklySummaryNarrative.set(result.narrative);
          this.weeklySummaryLoading.set(false);
        },
        error: () => {
          this.weeklySummaryError.set('Unable to generate the weekly summary right now.');
          this.weeklySummaryLoading.set(false);
        },
      });
    },
    error: () => {
      this.weeklySummaryError.set('Unable to determine your workspace member id.');
      this.weeklySummaryLoading.set(false);
    },
  });
}

  private currentWeekStart(): string {
    const now = new Date();
    const day = now.getDay();
    const monday = new Date(now);
    monday.setDate(now.getDate() - ((day + 6) % 7));
    return monday.toISOString().slice(0, 10);
  }

  statusBadgeVariant(status: ScoreCard['status']): 'success' | 'warning' | 'neutral' {
    return { HEALTHY: 'success', MODERATE: 'warning', AT_RISK: 'warning' }[status] as 'success' | 'warning';
  }

  statusLabel(status: ScoreCard['status']): string {
    return { HEALTHY: 'Healthy', MODERATE: 'Moderate', AT_RISK: 'At Risk' }[status];
  }

  // Productivity Score sparkline (line chart, no axes, matches the small upward-trend line in the wireframe)
  readonly currentProductivityScore = computed(() => {
    const points = this.dashboard()?.productivityTrend?.points ?? [];
    const latest = [...points].reverse().find((p) => p.userScore !== null);
    return latest?.userScore ?? null;
  });

  readonly productivityDelta = computed(() => {
    const points = (this.dashboard()?.productivityTrend?.points ?? []).filter((p) => p.userScore !== null);
    if (points.length < 2) return null;
    return Math.round((points[points.length - 1].userScore! - points[points.length - 2].userScore!) * 10) / 10;
  });

  readonly productivitySparkline = computed<ChartConfiguration>(() => {
    const points = this.dashboard()?.productivityTrend?.points ?? [];
    return {
      type: 'line',
      data: {
        labels: points.map((p) => p.weekLabel),
        datasets: [
          {
            data: points.map((p) => p.userScore ?? 0),
            borderColor: tokenColor('--color-primary'),
            backgroundColor: 'rgba(15,76,145,0.08)',
            fill: true,
            tension: 0.35,
            pointRadius: 3,
            pointBackgroundColor: tokenColor('--color-primary'),
          },
        ],
      },
      options: {
        ...insightChartDefaults(),
        scales: { x: { display: false }, y: { display: false } },
      },
    };
  });

  // Jira Tickets doughnut
  private readonly jiraStatusColors: Record<string, string> = {
    TODO: '#0F4C91',
    'TO DO': '#0F4C91',

    IN_PROGRESS: '#E07830',
    'IN PROGRESS': '#E07830',

    DONE: '#3FA34D',

    BLOCKED: '#C94F4F',
  };

  readonly jiraChart = computed<ChartConfiguration>(() => {
  const byStatus = this.jiraBreakdown()?.byStatus ?? [];

  return {
    type: 'doughnut',
    data: {
      labels: byStatus.map((s) => s.status),
      datasets: [
        {
          data: byStatus.map((s) => s.count),
          backgroundColor: byStatus.map((s) => {
            const key = s.status.trim().toUpperCase();
            return this.jiraStatusColors[key] ?? '#94A3B8';
          }),
          borderWidth: 0,
        },
      ],
    },
    options: {
      responsive: true,
      maintainAspectRatio: false,
      cutout: '68%',
      plugins: {
        legend: {
          display: false,
        },
      },
    },
  };
});

  // Time Allocation doughnut
  private readonly categoryColors = ['#0F4C91', '#E07830', '#BFD4F4', '#9B7EDE', '#6B7A99'];

  readonly timeAllocationChart = computed<ChartConfiguration>(() => {
    const categories = this.dashboard()?.timeAllocation?.categories ?? [];
    return {
      type: 'doughnut',
      data: {
        labels: categories.map((c) => c.category),
        datasets: [
          {
            data: categories.map((c) => c.hours),
            backgroundColor: categories.map((_, i) => this.categoryColors[i % this.categoryColors.length]),
            borderWidth: 0,
          },
        ],
      },
      options: { responsive: true, maintainAspectRatio: false, cutout: '68%', plugins: { legend: { display: false } } },
    };
  });

  readonly timeAllocationLegend = computed(() => {
    const categories = this.dashboard()?.timeAllocation?.categories ?? [];
    return categories.map((c, i) => ({ ...c, color: this.categoryColors[i % this.categoryColors.length] }));
  });

  // Estimate vs Actual grouped bar
  readonly estimateVsActualChart = computed<ChartConfiguration>(() => {
    const categories = this.dashboard()?.estimateVsActual?.categories ?? [];
    const muted = tokenColor('--color-text-muted');
    const border = tokenColor('--color-border');
    return {
      type: 'bar',
      data: {
        labels: categories.map((c) => c.category),
        datasets: [
          {
            label: 'Estimated',
            data: categories.map((c) => c.estimatedHours),
            backgroundColor: tokenColor('--color-primary-tint'),
            borderRadius: 5,
            maxBarThickness: 24,
          },
          {
            label: 'Actual',
            data: categories.map((c) => c.actualHours),
            backgroundColor: tokenColor('--color-primary'),
            borderRadius: 5,
            maxBarThickness: 24,
          },
        ],
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        plugins: { legend: { display: true, position: 'top', align: 'end', labels: { boxWidth: 10, font: { size: 11 }, color: muted } } },
        scales: {
          x: { grid: { display: false }, ticks: { font: { size: 10.5 }, color: muted } },
          y: { grid: { color: border }, ticks: { font: { size: 10 }, color: muted } },
        },
      },
    };
  });

  // Estimate accuracy pill text, for the ScoreCard already computed server-side
  readonly estimateAccuracyCard = computed(() => (this.dashboard()?.scoreCards ?? []).find((c) => c.insightType === 'ESTIMATE_ACCURACY'));
  readonly estimateAccuracyPillText = computed(() => {
    const score = this.estimateAccuracyCard()?.score;
    return score !== null && score !== undefined ? `${score}% accurate` : 'No data yet';
  });

  // Burnout Scale: custom gauge, not a Chart.js chart, technically a gradient slider
  readonly burnoutCard = computed(() => (this.dashboard()?.scoreCards ?? []).find((c) => c.insightType === 'BURNOUT'));
  readonly burnoutGaugePercent = computed(() => this.burnoutCard()?.score ?? 0);
  readonly burnoutRiskLabel = computed(() => {
    const status = this.burnoutCard()?.status;
    return { HEALTHY: 'Low Risk', MODERATE: 'Moderate Risk', AT_RISK: 'High Risk' }[status ?? 'HEALTHY'];
  });

  // Task Switching bar (Mon-Sun)
  readonly taskSwitchingChart = computed<ChartConfiguration>(() => {
    const days = this.dashboard()?.taskSwitchingByDay?.days ?? [];
    const border = tokenColor('--color-border');
    const muted = tokenColor('--color-text-muted');
    return {
      type: 'bar',
      data: {
        labels: days.map((d) => d.dayLabel),
        datasets: [
          {
            data: days.map((d) => d.switches),
            backgroundColor: tokenColor('--color-primary'),
            borderRadius: 5,
            maxBarThickness: 28,
          },
        ],
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        plugins: { legend: { display: false } },
        scales: {
          x: { grid: { display: false }, ticks: { font: { size: 10.5 }, color: muted } },
          y: { grid: { color: border }, ticks: { font: { size: 10 }, color: muted } },
        },
      },
    };
  });

  // Time Split by Task doughnut
  private readonly taskColors = ['#0F4C91', '#E07830', '#9B7EDE', '#6BAE75', '#D46A6A', '#6B7A99'];

  readonly timeSplitByTaskChart = computed<ChartConfiguration>(() => {
    const tasks = this.dashboard()?.timeSplitByTask?.tasks ?? [];
    return {
      type: 'doughnut',
      data: {
        labels: tasks.map((t) => t.taskTitle),
        datasets: [
          {
            data: tasks.map((t) => t.hours),
            backgroundColor: tasks.map((_, i) => this.taskColors[i % this.taskColors.length]),
            borderWidth: 0,
          },
        ],
      },
      options: { responsive: true, maintainAspectRatio: false, cutout: '68%', plugins: { legend: { display: false } } },
    };
  });

  readonly timeSplitByTaskLegend = computed(() => {
    const tasks = this.dashboard()?.timeSplitByTask?.tasks ?? [];
    return tasks.map((t, i) => ({ ...t, color: this.taskColors[i % this.taskColors.length] }));
  });

  // GitHub Activity bars use .compare-row
  githubBarPercent(value: number, target: number): number {
    return Math.min(100, (value / target) * 100);
  }
}