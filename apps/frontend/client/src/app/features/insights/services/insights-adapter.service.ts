/*
This file handles converting the raw ai dashboard response into the developer insights Manager insights ui model, i kept
it conservative, we arent actually inventing calenda, jira or github numbers that arent actually there

Author: Zamokuhle Zwane
Date: 03/09/2026
*/

import { Injectable } from '@angular/core';

import { DeveloperInsights, ManagerInsights,  ManagerProjectInsight } from '../models/insights.model';
import { AiDashboardResponse, AiInsight } from '../models/ai-insights.model';


@Injectable({ providedIn: 'root' })
export class InsightsAdapterService {
  toDeveloperInsights(response: AiDashboardResponse): DeveloperInsights {
    const userInsights = response.insights.filter((i) => i.scope === 'USER');
    const weekly = userInsights.find((i) => i.insightType === 'WEEKLY_SUMMARY');
    const productivity = userInsights.find(
      (i) => i.insightType === 'PRODUCTIVITY',
    );
    const switching = userInsights.find(
      (i) => i.insightType === 'TASK_SWITCHING',
    );
    const delivery = userInsights.find(
      (i) => i.insightType === 'DELIVERY_FORECAST',
    );
    const anomalies = userInsights.filter((i) => i.insightType === 'ANOMALY');

    return {
      weeklySummary: {
        weekLabel: weekly?.createdAt
          ? new Date(weekly.createdAt).toLocaleDateString()
          : 'latest',
        generatedNote: 'generated from ai insights',
        narrative: weekly?.narrative ?? 'No weekly summary available yet.',
      },
      overall: {
        productivity: {
          score: productivity?.score ?? 0,
          previousScore: 0,
          note:
            productivity?.description ?? 'No productivity insight available.',
        },
        trend: this.toProductivityTrend(userInsights),
        taskSwitching: {
          perDay: switching?.score ?? 0,
          average: 0,
          note:
            switching?.description ?? 'No task switching insight available.',
        },
        deliveryForecast: {
          taskName: 'delivery forecast',
          forecastDate: delivery?.createdAt
            ? new Date(delivery.createdAt).toLocaleDateString()
            : '-',
          note: delivery?.description ?? 'No delivery forecast available.',
        },
        flaggedEntries: anomalies.map((i) => this.toFlaggedEntry(i)),
        calendarVsTracked: {
          calendarHours: 0,
          trackedHours: 0,
          unmatchedHours: 0,
        },
        jiraVsLogged: [],
        gitEffort: this.toDeveloperGitEffort(response),
      },
      byProject: this.toDeveloperByProject(userInsights),
    };
  }
  toManagerInsights(response: AiDashboardResponse): ManagerInsights {
    const teamInsights = response.insights.filter((i) => i.scope === 'TEAM');

    const weekly = teamInsights.find((i) => i.insightType === 'WEEKLY_SUMMARY');
    const burnout = teamInsights.filter((i) => i.insightType === 'BURNOUT');
    const delivery = teamInsights.filter(
      (i) => i.insightType === 'DELIVERY_FORECAST',
    );
    const productivity = teamInsights.find(
      (i) => i.insightType === 'PRODUCTIVITY',
    );
    //const gitEffort = teamInsights.find(i => i.insightType === 'GIT_EFFORT');

    return {
      weeklySummary: {
        weekLabel: weekly?.createdAt
          ? new Date(weekly.createdAt).toLocaleDateString()
          : 'latest week',
        generatedNote: 'generated from ai insights',
        narrative: weekly?.narrative ?? 'No weekly summary available yet.',
      },
      overall: {
        burnoutRisk: burnout.map((i) => ({
          id: i.id,
          name: i.memberName ?? 'Unknown team member',
          reason: i.description ?? 'No description available.',
          level: this.toRiskLevel(i.score),
        })),
        deliveryForecast: delivery.map((i) => ({
          id: i.id,
          project: i.projectName ?? 'Unkwon project',
          planned: '-',
          forecast: i.description ?? '-',
          onTrack: true,
        })),
        productivityTrend: this.toProductivityTrend(teamInsights),
        gitEffort: this.toManagerGitEffort(response),
      },
      byProject: this.toManagerByProject(teamInsights, response.workspaceMemberId),
    };
  }

  //helper functions
  private toDeveloperGitEffort(response: AiDashboardResponse): DeveloperInsights['overall']['gitEffort'] {
    const github = response.github;

    if (!github || !github.connected) {
      return {
        correlation: 0,
        correlationPrevWeek: 0,
        days: [],
        hoursLogged: [],
        commitCount: [],
        note: 'connect github to compare repository activity with logged time.'
      };
    }
    return {
      correlation: github.commitsPerHour,
      correlationPrevWeek: 0,
      days: ['This week'],
      hoursLogged: [github.hoursLogged],
      commitCount: [github.commitCount],
      note: github.explanation ?? 'github activity is present.'
    };
}

private toManagerGitEffort(response: AiDashboardResponse): ManagerInsights['overall']['gitEffort'] {
    const github = response.github;

    if (!github || !github.connected) {
      return {
        correlation: 0,
        correlationPrevWeek: 0,
        members: [],
        hoursLogged: [],
        commitCount: [],
        callouts: [],
        note: 'connect github to compare repository activity with logged time.'
      };
    }
    return {
      correlation: github.commitsPerHour,
      correlationPrevWeek: 0,
      members: ['This week'],
      hoursLogged: [github.hoursLogged],
      commitCount: [github.commitCount],
      callouts: [],
      note: github.explanation ?? 'github activity is present.'
    };
}

  private toFlaggedEntry(insight: AiInsight) {
    return {
      title: insight.description ?? 'anomaly detected',
      sub: insight.createdAt,
      level: this.toRiskLevel(insight.score),
    };
  }
  private toProductivityTrend(userInsights: AiInsight[]): { labels: string[]; values: number[] } {
    const weekly = userInsights
      .filter((i) => i.insightType === 'PRODUCTIVITY' && i.projectId === null)
      .filter((i) => i.score !== null)
      .sort((a, b) => new Date(a.createdAt).getTime() - new Date(b.createdAt).getTime())
      .slice(-6);
 
    return {
      labels: weekly.map((i) => new Date(i.createdAt).toLocaleDateString(undefined, { month: 'short', day: 'numeric' })),
      values: weekly.map((i) => i.score as number),
    };
  }
  private toDeveloperByProject(userInsights: AiInsight[]): DeveloperInsights['byProject'] {
    const perProject = userInsights.filter(
      (i) => i.insightType === 'PRODUCTIVITY' && i.projectId !== null,
    );
 
    const latestByProject = new Map<string, AiInsight>();
    for (const insight of perProject) {
      const existing = latestByProject.get(insight.projectId as string);
      if (!existing || new Date(insight.createdAt) > new Date(existing.createdAt)) {
        latestByProject.set(insight.projectId as string, insight);
      }
    }
 
    const projects = Array.from(latestByProject.entries()).map(([projectId, insight]) => ({
      name: insight.projectName ?? 'Unknown project',
      score: insight.score ?? 0,
      note: insight.description ?? 'No project note available.',
    }));
 
    return {
      projects,
      hoursByProject: { labels: [], hours: [] },
    };
  }

  private toManagerByProject(
    teamInsights: AiInsight[],
    currentMemberId: string,
  ): ManagerInsights['byProject'] {
    //per-project, per-member PRODUCTIVITY rows, same shape V14 section 4 seeded
    const perProjectPerMember = teamInsights.filter(
      (i) => i.insightType === 'PRODUCTIVITY' && i.projectId !== null && i.workspaceMemberId !== null,
    );
    
    const grouped = new Map<string, AiInsight[]>();
  for (const insight of perProjectPerMember) {
    const key = insight.projectId as string;
    if (!grouped.has(key)) grouped.set(key, []);
    grouped.get(key)!.push(insight);
  }
  const projects: Record<string, ManagerProjectInsight> = {};
  for (const [projectId, insights] of grouped.entries()) {
    projects[projectId] = {
      key: projectId,
      title: insights[0].projectName ?? 'Unknown project',
      compare: insights.map((i) => ({
        name: i.memberName ?? 'Unknown team member',
        score: i.score ?? 0,
        isYou: i.workspaceMemberId === currentMemberId,
      })),
      hoursLabels: [],
      hours: [],
      note: insights.find((i) => i.description)?.description ?? 'No notes yet for this project.',
    };
  }

  return { projects };
}



  private toRiskLevel(score: number | null): 'high' | 'medium' | 'low' {
    if (score === null) return 'low';
    if (score >= 70) return 'high';
    if (score >= 40) return 'medium';
    return 'low';
  }
}
