/**
 This file handles the shared card shell for the Insights page (and its also reusable anywhere a raw-data-vs-AI-output distinction needs to show, e.g. Dashboard later)
 variant="insight" -> secondary/amber left border, used for AI/model output
 variant="raw"      -> primary/navy left border, used for raw or comparison data
 badge="ai"         -> renders the "AI" pill (secondary colour, sparkle icon)
 Follows the same @Input + standalone pattern as StatsCardComponent

 Author: Zamokuhle Zwane
 Date: 03/09/2026
 */
import { Component, Input } from '@angular/core';

export type CardVariant = 'raw' | 'insight' | 'plain' | 'summary';
export type BadgeKind = 'ai' | 'raw' | 'comparison' | 'none';
export type CardSpan = 4 | 5 | 6 | 7 | 8 | 12;

@Component({
  selector: 'app-insight-card',
  standalone: true,
  templateUrl: './insight-card.component.html',
  styleUrl: './insight-card.component.scss'
})
export class InsightCardComponent {
  @Input() title = '';
  @Input() subtitle = '';
  @Input() variant: CardVariant = 'plain';
  @Input() badge: BadgeKind = 'none';
  @Input() span: CardSpan = 4;
}
