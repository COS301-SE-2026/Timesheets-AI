/*
This file handles the model, the types match backends aiDashboard response and mirrpors the 
Ai service schema 1:1

Author: Zamokuhle Zwane
Date: 03/09/2026
*/


export interface AiInsight {
    id: string;
    insightType: string;
    scope: 'USER' | 'TEAM';
    score: number | null;
    confidence: number | null;
    description: string | null;
    recommendation: string | null;
    narrative : string | null;
    projectId: string | null;
    projectName: string | null;
    workspaceMemberId: string | null;
    memberName: string | null;
    workspaceId: string | null;
    createdAt: string;
}

export interface GithubActivity {
    connected: boolean;
    hoursLogged: number;
    commitCount: number;
    commitsPerHour: number;
    additions: number;
    deletions: number;
    activeRepositories: number;
    activeDays: number;
    alignment: string | null;
    alignmentScore: number | null;
    explanation: string | null;
}

export interface AiDashboardResponse {
    workspaceMemberId: string;
    insights: AiInsight[];
    github: GithubActivity | null;
    period?: string;
    productivityTrend?: ProductivityTrend;
    timeAllocation?: TimeAllocation;
    estimateVsActual?: EstimateVsActual;
    scoreCards?: ScoreCard[];
    timeSplitByTask?: TimeSplitByTask;
    taskSwitchingByDay?: TaskSwitchingByDay;
}

export interface ProjectHours {
  projectId: string;
  projectName: string;
  hours: number;
  entryCount: number;
}

export interface PersonalInsightsResponse {
  totalHoursLogged: number;
  averageHoursPerDay: number;
  totalDaysLogged: number;
  hoursPerProject: ProjectHours[];
  hoursPerTask: unknown[];
  dailyTrend: unknown[];
}

export interface ProductivityTrendPoint {
  weekLabel: string;
  userScore: number | null;
  teamAverage: number | null;
}

export interface ProductivityTrend {
  points: ProductivityTrendPoint[];
  isSynthetic: boolean;
}

export interface TimeAllocationCategory {
  category: string;
  hours: number;
  percentage: number;
}

export interface TimeAllocation {
  totalHours: number;
  categories: TimeAllocationCategory[];
}

export interface EstimateVsActualCategory {
  category: string;
  estimatedHours: number;
  actualHours: number;
}

export interface EstimateVsActual {
  categories: EstimateVsActualCategory[];
}

export type StatusBand = 'HEALTHY' | 'MODERATE' | 'AT_RISK';

export interface ScoreCard {
  insightType: string;
  score: number | null;
  status: StatusBand;
  deltaVsPrevious: number | null;
  recommendation: string;
  sparkline: number[];
}

export interface TimeSplitByTaskItem {
  taskTitle: string;
  hours: number;
  percentage: number;
}

export interface TimeSplitByTask {
  totalHours: number;
  tasks: TimeSplitByTaskItem[];
}

export interface JiraStatusCount {
  status: string;
  count: number;
  percentage: number;
}

export interface JiraTicketsBreakdown {
  totalTickets: number;
  byStatus: JiraStatusCount[];
}

export interface DeveloperProject {
  projectId: string;
  projectName: string;
  hoursLogged: number;
  active: boolean;
}

export type PeriodOption = '4w' | '8w' | '12w';

export interface TaskSwitchingByDayItem {
  dayLabel: string;
  switches: number;
}

export interface TaskSwitchingByDay {
  days: TaskSwitchingByDayItem[];
}