/*
This file introduces a thin wrapper around Chart.js so every line/bar chart on Insights shares one
config path (fonts, colours, no legend by default) instead of duplicatin Chart.js setup per component
Author: Zamokuhle Zwane
Date: 03/09/2026
 */

import {
  AfterViewInit,
  Component,
  ElementRef,
  Input,
  OnChanges,
  OnDestroy,
  SimpleChanges,
  ViewChild
} from '@angular/core';
import { Chart, ChartConfiguration, registerables } from 'chart.js';

Chart.register(...registerables);

@Component({
  selector: 'app-insight-chart',
  standalone: true,
  template: `<div class="chart-wrap" [style.height.px]="height"><canvas #canvas></canvas></div>`,
  styles: [`
    .chart-wrap { position: relative; width: 100%; min-width: 0; }
  `]
})
export class InsightChartComponent implements AfterViewInit, OnChanges, OnDestroy {
  @ViewChild('canvas', { static: true }) canvasRef!: ElementRef<HTMLCanvasElement>;

  /** Pass a full Chart.js config so this stays generic (incl. dual-axis bars). */
  @Input({ required: true }) config!: ChartConfiguration;
  @Input() height = 180;

  private chart?: Chart;

  ngAfterViewInit(): void {
    this.render();
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['config'] && this.chart) {
      this.chart.destroy();
      this.render();
    }
  }

  ngOnDestroy(): void {
    this.chart?.destroy();
  }

  private render(): void {
    if (!this.canvasRef || !this.config) return;
    this.chart = new Chart(this.canvasRef.nativeElement, this.config);
  }
}

/*
Shared axis/font/colour defaults, read from the Momently design tokens at call time (Chart.js needs literal colour values, it can't read CSS vars
itself, so well resolve them off <html> once here)
 */
export function insightChartDefaults(): ChartConfiguration['options'] {
  const styles = getComputedStyle(document.documentElement);
  const muted = styles.getPropertyValue('--color-text-muted').trim() || '#6B7A99';
  const border = styles.getPropertyValue('--color-border').trim() || '#DCE5F2';
  const bodyFont = tokenFont('--font-body');
  const monoFont = tokenFont('--font-mono');

  return {
    responsive: true,
    maintainAspectRatio: false,
    plugins: { legend: { display: false } },
    scales: {
      x: { grid: { display: false }, ticks: { font: { family: bodyFont, size: 10.5 }, color: muted } },
      y: { grid: { color: border }, ticks: { font: { family: monoFont, size: 10 }, color: muted } }
    }
  };
}

export function tokenColor(varName: string): string {
  return getComputedStyle(document.documentElement).getPropertyValue(varName).trim();
}

//Resolve a font-family CSS custom property (strips surrounding quotes) for Chart.js
export function tokenFont(varName: string): string {
  const raw = getComputedStyle(document.documentElement).getPropertyValue(varName).trim();
  return raw.replace(/['"]/g, '') || 'sans-serif';
}
