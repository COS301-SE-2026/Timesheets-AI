/**
 * Author: Kgaugelo Matsena
 * Date: 2026-05-10
 * Purpose: Appplication configuration with providers for routing, HTTP, and change detection.
 * Related Requirement: -
 */

import { ApplicationConfig, provideZoneChangeDetection } from '@angular/core';
import { provideRouter } from '@angular/router';

import { routes } from './app.routes';

export const appConfig: ApplicationConfig = {
  providers: [provideZoneChangeDetection({ eventCoalescing: true }), provideRouter(routes)]
};
