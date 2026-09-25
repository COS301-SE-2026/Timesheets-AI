export type ProjectForecastRiskStatus = | 'HEALTHY' | 'WARNING' | 'CRITICAL' | 'UNKNOWN';

export type ProjectForecastConfidenceLevel = | 'HIGH' | 'MEDIUM' | 'LOW';

export interface ProjectForecastBudget {
  budgetHours: number | null;
  usedHours: number;
  remainingBudgetHours: number | null;
  forecastTotalHours: number | null;
  forecastOverrunHours: number | null;
}

export interface ProjectForecastSchedule {
  startDate: string | null;
  plannedEndDate: string | null;
  forecastEndDate: string | null;
  delayDays: number | null;
  timelineProgressPercentage: number | null;
}

export interface ProjectForecastTasks {
  totalTasks: number;
  completedTasks: number;
  remainingTasks: number;
  completionPercentage: number;
  estimatedRemainingHours: number;
}

export interface ProjectForecastVelocity {
  recentHoursPerWeek: number;
  lookbackDays: number;
  hoursInPeriod: number;
  hasSufficientData: boolean;
}

export interface ProjectForecastRisk {
  budget: ProjectForecastRiskStatus;
  schedule: ProjectForecastRiskStatus;
  taskProgress: ProjectForecastRiskStatus;
  overall: ProjectForecastRiskStatus;
}

export interface ProjectForecastConfidence {
  level: ProjectForecastConfidenceLevel;
  evidenceAvailable: number;
  evidenceTotal: number;
  missingEvidence: string[];
  taskEstimateCoveragePercentage: number;
}

export interface ProjectForecastExternalActivity {
  activityType: string;
  count: number;
}

export interface ProjectForecastExternalSource {
  available: boolean;
  activityCount: number;
  latestActivity: string | null;
  activities: ProjectForecastExternalActivity[];
}

export interface ProjectForecastExternalEvidence {
  projectId: string;
  github: ProjectForecastExternalSource;
  jira: ProjectForecastExternalSource;
}

export interface ProjectForecastRecommendation {
  title: string;
  description: string;
}

export interface ProjectForecastAiExplanation {
  summary: string;
  riskExplanation: string;
  contributingFactors: string[];
  recommendations: ProjectForecastRecommendation[];
}

export interface ProjectForecast {
  projectId: string;
  projectName: string;
  budget: ProjectForecastBudget;
  schedule: ProjectForecastSchedule;
  tasks: ProjectForecastTasks;
  velocity: ProjectForecastVelocity;
  risk: ProjectForecastRisk;
  confidence: ProjectForecastConfidence;
  externalEvidence: ProjectForecastExternalEvidence;
  aiExplanation: ProjectForecastAiExplanation;
}

export interface SavedProjectForecast {
  projectId: string;
  lastSyncedAt: string;
  forecast: ProjectForecast;
}