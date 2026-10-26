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
import { Chart, ChartConfiguration, ChartOptions, ScriptableContext, registerables } from 'chart.js';

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

/*
Sleek-chart helpers shared by the Insights pages, so every line chart gets the same soft area fill and tooltip
instead of each component hand-rolling its own. Chart.js needs literal colours, so hex tokens are converted here.
*/
function withAlpha(color: string, alpha: number): string {
  const hex = color.trim().replace('#', '');
  if (/^[0-9a-f]{3}$/i.test(hex) || /^[0-9a-f]{6}$/i.test(hex)) {
    const full = hex.length === 3 ? hex.split('').map((c) => c + c).join('') : hex;
    const n = Number.parseInt(full, 16);
    return `rgba(${(n >> 16) & 255}, ${(n >> 8) & 255}, ${n & 255}, ${alpha})`;
  }
  return color;
}

// Scriptable gradient fill for line charts, fades from the line colour to transparent
export function areaGradient(color: string, topAlpha = 0.2) {
  return (context: ScriptableContext<'line'>) => {
    const { chart } = context;
    const area = chart.chartArea;
    if (!area) return withAlpha(color, topAlpha / 2);
    const gradient = chart.ctx.createLinearGradient(0, area.top, 0, area.bottom);
    gradient.addColorStop(0, withAlpha(color, topAlpha));
    gradient.addColorStop(1, withAlpha(color, 0));
    return gradient;
  };
}

export function sleekTooltip(): NonNullable<ChartOptions['plugins']>['tooltip'] {
  return {
    backgroundColor: '#102b61',
    titleFont: { family: tokenFont('--font-body'), size: 11, weight: 600 },
    bodyFont: { family: tokenFont('--font-body'), size: 11 },
    padding: 10,
    cornerRadius: 8,
    boxPadding: 4,
    displayColors: false,
  };
}
