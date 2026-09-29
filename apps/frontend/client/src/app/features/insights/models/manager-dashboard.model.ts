/*
tHIS FILE HANDLES THE types for the Manager Insights page. Kept in its own file instead of
dumping this into ai-insights.model.ts since that one's shaped around
the developer-side dashboard

Author: Zamokuhle Zwane
Date: 25/09/2026
*/

export interface WeeklyProgressPoint {
  weekLabel: string;
  percent: number;
}

export interface ProjectCompletionForecast {
  weeklyProgress: WeeklyProgressPoint[];
  currentProgressPercent: number;
  forecastCompletionDate: string | null;
}

export interface TeamLoggedHoursMember {
  memberName: string;
  hours: number;
}

export interface TeamLoggedHours {
  members: TeamLoggedHoursMember[];
}

export type BurnoutStatusBand = 'HIGH_RISK' | 'AT_RISK';

export interface FlaggedBurnoutMember {
  workspaceMemberId: string;
  riskScore: number;
  statusBand: BurnoutStatusBand;
  reason: string | null;
  insightId: string;
  memberName: string;
  memberRole: string;
}

export interface TaskOverview {
  total: number;
  todo: number;
  inProgress: number;
  done: number;
  blocked: number;
  narrative: string;
}

export interface VelocityWeek {
  weekLabel: string;
  completed: number;
}

export interface Velocity {
  weeks: VelocityWeek[];
  rollingAverage: number;
}

export interface ProjectHealth {
  score: number;
  statusLabel: 'On Track' | 'At Risk' | 'Behind' | 'No data';
  onTrackPercent: number;
  atRiskPercent: number;
  behindPercent: number;
}

export interface GithubActivityByMember {
  workspaceMemberId: string;
  memberName: string;
  hoursLogged: number;
  commitCount: number;
}

export interface JiraTypeCount {
  issueType: string;
  count: number;
  percentage: number;
}

export interface JiraTicketBreakdown {
  total: number;
  byType: JiraTypeCount[];
}

export interface ManagedProject {
  projectId: string;
  projectName: string;
}

export interface ManagerDashboardResponse {
  projectCompletionForecast: ProjectCompletionForecast;
  teamLoggedHours: TeamLoggedHours;
  flaggedBurnoutMembers: FlaggedBurnoutMember[];
  taskOverview: TaskOverview;
  velocity: Velocity;
  projectHealth: ProjectHealth;
  githubActivityByMember: GithubActivityByMember[];
  jiraTicketBreakdown: JiraTicketBreakdown;
  managedProjects: ManagedProject[];
  scopedProjectId: string | null;
  period: string;
}

export interface GenerateInsightsSyncResult {
  source: 'LIVE' | 'CACHED';
  lastSyncedAt: string;
}

export interface GenerateInsightsResponse {
  github: GenerateInsightsSyncResult;
  jira: GenerateInsightsSyncResult;
}