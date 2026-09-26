/*
This hits springs /api.insights/ai, follow the same enviroment.api.Url
pattern as the existing auth service

Author: Zamokuhle Zwane
Date: 03/09/2026
*/

import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';
import {
  AiDashboardResponse,
  DeveloperProject,
  JiraTicketsBreakdown,
  PeriodOption,
  PersonalInsightsResponse,
} from '../../features/insights/models/ai-insights.model';

@Injectable({
  providedIn: 'root',
})
export class InsightsService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/insights`;

  getAiDashboard(period: PeriodOption = '8w'): Observable<AiDashboardResponse> {
    const params = new HttpParams().set('period', period);
    return this.http.get<AiDashboardResponse>(`${this.baseUrl}/ai`, { params });
  }

  getMyProjects(): Observable<DeveloperProject[]> {
    return this.http.get<DeveloperProject[]>(`${this.baseUrl}/my-projects`);
  }

  getJiraTicketsBreakdown(): Observable<JiraTicketsBreakdown> {
    return this.http.get<JiraTicketsBreakdown>(
      `${environment.apiUrl}/integrations/jira/tickets-breakdown`,
    );
  }

  generateWeeklySummary(
  subjectId: string,
  weekStart: string,
): Observable<{ narrative: string; weekStart: string }> {
  const params = new HttpParams()
    .set('subject_type', 'USER')
    .set('week_start', weekStart);
  return this.http.post<{ narrative: string; weekStart: string }>(
    `${environment.aiServiceUrl}/insights/weekly-summary/${subjectId}/generate`,
    {},
    { params },
  );
}
  getCalendarVsTracked(
    from: string,
    to: string,
  ): Observable<{
    connected: boolean;
    calendarHours: number;
    trackedHours: number;
    unmatchedHours: number;
  }> {
    const params = new HttpParams().set('from', from).set('to', to);
    return this.http.get<any>(`${environment.apiUrl}/calendar/vs-tracked`, {
      params,
    });
  }
  getCurrentWorkspaceMemberId(): Observable<{ workspaceMemberId: string }> {
    return this.http.get<{ workspaceMemberId: string }>(`${this.baseUrl}/me`);
  }
  getJiraVsLogged(): Observable<
    { ticket: string; estimateHours: number; loggedHours: number }[]
  > {
    return this.http.get<any>(
      `${environment.apiUrl}/integrations/jira/vs-logged`,
    );
  }
  getInsightsSummary(
    from: string,
    to: string,
  ): Observable<PersonalInsightsResponse> {
    const params = new HttpParams().set('from', from).set('to', to);
    return this.http.get<PersonalInsightsResponse>(`${this.baseUrl}/summary`, {
      params,
    });
  }
}
