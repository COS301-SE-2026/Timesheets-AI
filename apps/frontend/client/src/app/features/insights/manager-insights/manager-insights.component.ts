/*
This file handles the anagers page version of insights

Author: Zamokuhle Zwane
Date: 03/09/2026
*/
import { Component, OnInit, inject, computed, signal } from '@angular/core';
import { ChartConfiguration } from 'chart.js';

import { InsightCardComponent } from '../../../shared/components/insight-card/insight-card.component';
import { ScopeSwitcherComponent, ScopeOption } from '../../../shared/components/scope-switcher/scope-switcher.component';
import { InsightChartComponent, insightChartDefaults, tokenColor } from '../../../shared/components/insight-chart/insight-chart.component';
import { InsightsService } from '../../../core/services/insights.service';
import { InsightsAdapterService } from '../services/insights-adapter.service';
import { ManagerInsights, ManagerProjectInsight } from '../models/insights.model';
import { ProjectService, ProjectDetailResponse } from '../../../core/services/project.service'; 

type MgrScope = 'overall' | 'project';

@Component({
  selector: 'app-manager-insights',
  standalone: true,
  imports: [InsightCardComponent, ScopeSwitcherComponent, InsightChartComponent],
  templateUrl: './manager-insights.component.html',
  styleUrl: './manager-insights.component.scss'
})
export class ManagerInsightsComponent implements OnInit {
  private readonly insightsService = inject(InsightsService);
  private readonly adapter = inject(InsightsAdapterService);
  private readonly projectService = inject(ProjectService);
  readonly projectDetail = signal<ProjectDetailResponse | null>(null);

  readonly loading = signal(true);
  readonly error = signal<string | null>(null);
  readonly data = signal<ManagerInsights | null>(null);

  readonly scopeOptions: ScopeOption[] = [
    { label: 'Overall', value: 'overall' },
    { label: 'By project', value: 'project' }
  ];

  // used to be built off the mock in the constructor. now it's a computed of the live signal since the project list isn't known until the response lands
  readonly projectOptions = computed<ScopeOption[]>(() =>
    Object.values(this.data()?.byProject.projects ?? {}).map(p => ({
      label: p.title,
      value: p.key
    }))
  );

  readonly scope = signal<MgrScope>('overall');
  readonly selectedProjectKey = signal<string>('');

  readonly selectedProject = computed<ManagerProjectInsight | null>(() => {
    const projects = this.data()?.byProject.projects ?? {};
    const key = this.selectedProjectKey() || Object.keys(projects)[0];
    return key ? (projects[key] ?? null) : null;
  });

  ngOnInit(): void {
    this.loadInsights();
  }

  loadInsights(): void {
    this.loading.set(true);
    this.insightsService.getAiDashboard().subscribe({
      next: response => {
        this.data.set(this.adapter.toManagerInsights(response));
        const firstKey = Object.keys(this.adapter.toManagerInsights(response).byProject.projects)[0];
        if (firstKey) this.setProject(firstKey);
        this.loading.set(false);
      },
      error: () => {
        this.error.set('unable to load ai insights right now.');
        this.loading.set(false);
      }
    });
  }

  setScope(v: string): void {
    this.scope.set(v as MgrScope);
  }

  setProject(v: string): void {
    this.selectedProjectKey.set(v);
    this.projectService.getProjectDetail(v).subscribe({
      next: (detail: ProjectDetailResponse) => this.projectDetail.set(detail),
      error: () => this.projectDetail.set(null)
    })
  }

  // ---- chart configs, colours resolved from the design tokens at build time ----

  readonly trendChart = computed<ChartConfiguration>(() => ({
    type: 'line',
    data: {
      labels: this.data()?.overall.productivityTrend.labels ?? [],
      datasets: [{
        data: this.data()?.overall.productivityTrend.values ?? [],
        borderColor: tokenColor('--color-primary'),
        backgroundColor: 'rgba(15,76,145,0.08)', // --color-primary at low alpha
        fill: true,
        tension: 0.35,
        pointRadius: 3,
        pointBackgroundColor: tokenColor('--color-primary')
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
        labels: gitEffort?.members ?? [],
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

  readonly projectHoursChart = computed<ChartConfiguration>(() => ({
    type: 'bar',
    data: {
      labels: this.projectDetail()?.members.map(m => `${m.firstName} ${m.lastName}`) ?? [],
      datasets: [{
        data: this.projectDetail()?.members.map(m => m.hoursLogged) ?? [],
        backgroundColor: tokenColor('--color-primary'),
        borderRadius: 6,
        barThickness: 30
      }]
    },
    options: insightChartDefaults()
  }));
}