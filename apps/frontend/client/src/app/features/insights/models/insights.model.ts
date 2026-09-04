// Follows the same pattern as features/projects/models — plain interfaces the
// feature's mock data (and later, an InsightsService) are typed against.

export type RiskLevel = 'high' | 'medium' | 'low';

export interface FlaggedEntry {
  title: string;
  sub: string;
  level: RiskLevel;
}

export interface WeeklySummary {
  weekLabel: string;
  generatedNote: string;
  narrative: string;
}

export interface ProjectHours {
  labels: string[];
  hours: number[];
}

export interface GitEffortMember {
  name: string;
  note: string;
  tag: 'review' | 'expected';
}

export interface DeveloperInsights {
  weeklySummary: WeeklySummary;
  overall: {
    productivity: { score: number; previousScore: number; note: string };
    trend: { labels: string[]; values: number[] };
    taskSwitching: { perDay: number; average: number; note: string };
    deliveryForecast: { taskName: string; forecastDate: string; note: string };
    flaggedEntries: FlaggedEntry[];
    calendarVsTracked: { calendarHours: number; trackedHours: number; unmatchedHours: number };
    jiraVsLogged: { ticket: string; estimateHours: number; loggedHours: number }[];
    gitEffort: {
      correlation: number;
      correlationPrevWeek: number;
      days: string[];
      hoursLogged: number[];
      commitCount: number[];
      note: string;
    };
  };
  byProject: {
    projects: { name: string; score: number; note: string }[];
    hoursByProject: ProjectHours;
  };
}

export interface ManagerProjectInsight {
  key: string;
  title: string;
  compare: { name: string; score: number; isYou: boolean }[];
  hoursLabels: string[];
  hours: number[];
  note: string;
}

export interface ManagerInsights {
  weeklySummary: WeeklySummary;
  overall: {
    burnoutRisk: { id: string; name: string; reason: string; level: RiskLevel }[];
    deliveryForecast: {id: string; project: string; planned: string; forecast: string; onTrack: boolean }[];
    productivityTrend: { labels: string[]; values: number[] };
    gitEffort: {
      correlation: number;
      correlationPrevWeek: number;
      members: string[];
      hoursLogged: number[];
      commitCount: number[];
      callouts: GitEffortMember[];
      note: string;
    };
  };
  byProject: {
    projects: Record<string, ManagerProjectInsight>;
  };
}
