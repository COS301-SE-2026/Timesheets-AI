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
    explanation: string | null;
}

export interface AiDashboardResponse {
    workspaceMemberId: string;
    insights: AiInsight[];
    github: GithubActivity | null;
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
