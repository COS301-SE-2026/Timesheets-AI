// Global Jest setup for Angular test environment.

import 'jest-preset-angular/setup-env/zone';

jest.mock('ng2-charts', () => ({
  BaseChartDirective: jest.fn(),
  ChartComponent: jest.fn(),
  ChartsModule: jest.fn(),
}));