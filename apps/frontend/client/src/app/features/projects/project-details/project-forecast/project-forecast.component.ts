  /*
  - will be for showing the project forcast details

  Author: Nyasha
  Date: 25/09/2026
 */

import { CommonModule } from '@angular/common';
import {Component, inject, input, OnInit, signal, } from '@angular/core';
import { ChartConfiguration, ChartOptions } from 'chart.js';
import { BaseChartDirective } from 'ng2-charts';

import { ProjectService } from '../../../../core/services/project.service';
import { ProjectForecast, SavedProjectForecast, } from './models/project-forecast.model';

@Component({
  selector: 'app-project-forecast',
  standalone: true,
  imports: [CommonModule, BaseChartDirective],
  templateUrl: './project-forecast.component.html',
  styleUrl: './project-forecast.component.scss',
})
export class ProjectForecastComponent implements OnInit {
  private readonly projectService = inject(ProjectService);

  readonly projectId = input.required<string>();

  protected readonly forecast = signal<ProjectForecast | null>(null);
  protected readonly lastSyncedAt = signal<string | null>(null);

  protected readonly loading = signal<boolean>(true);
  protected readonly syncing = signal<boolean>(false);
  protected readonly error = signal<boolean>(false);

  ngOnInit(): void {
    this.loadForecast();
  }

  protected loadForecast(): void {
    this.loading.set(true);
    this.error.set(false);

    this.projectService.getProjectForecast(this.projectId()).subscribe({
      next: (savedForecast: SavedProjectForecast) => {
        this.forecast.set(savedForecast.forecast);
        this.lastSyncedAt.set(savedForecast.lastSyncedAt);
        this.loading.set(false);
      },
      error: () => {
        this.forecast.set(null);
        this.lastSyncedAt.set(null);
        this.loading.set(false);
      },
    });
  }

  protected syncForecast(): void {
    if (this.syncing()) {
      return;
    }

    this.syncing.set(true);
    this.error.set(false);

    this.projectService.syncProjectForecast(this.projectId()).subscribe({
      next: (forecast: ProjectForecast) => {
        this.forecast.set(forecast);
        this.syncing.set(false);

        //to reload the forecast so that the UI get the last sync from the DB
        this.loadForecast();
      },
      error: () => {
        this.syncing.set(false);
        this.error.set(true);
      },
    });
  }

  protected getBudgetChartData(): ChartConfiguration<'bar'>['data'] {
    const budget = this.forecast()?.budget;

    return {
      labels: ['Used', 'Forecast', 'Budget'],
      datasets: [
        {
          data: [
            budget?.usedHours ?? 0,
            budget?.forecastTotalHours ?? 0,
            budget?.budgetHours ?? 0,
          ],
          backgroundColor: ['#2563eb', '#E07830', '#d1d5db'],
          borderRadius: 8,
          borderSkipped: false,
          barThickness: 42,
        },
      ],
    };
  }

  protected readonly budgetChartOptions: ChartOptions<'bar'> = {
    responsive: true,
    maintainAspectRatio: false,
    plugins: {
      legend: {
        display: false,
      },
    },
    scales: {
      x: {
        grid: {
          display: false,
        },
        border: {
          display: false,
        },
      },
      y: {
        beginAtZero: true,
        border: {
          display: false,
        },
      },
    },
  };

  protected getTaskChartData(): ChartConfiguration<'doughnut'>['data'] {
    const tasks = this.forecast()?.tasks;

    return {
      labels: ['Completed', 'Remaining'],
      datasets: [
        {
          data: [
            tasks?.completedTasks ?? 0,
            tasks?.remainingTasks ?? 0,
          ],
          backgroundColor: ['#2563eb', '#e5e7eb'],
          borderWidth: 0,
        },
      ],
    };
  }

  protected readonly taskChartOptions: ChartOptions<'doughnut'> = {
    responsive: true,
    maintainAspectRatio: false,
    cutout: '72%',
    plugins: {
      legend: {
        display: false,
      },
    },
  };
}