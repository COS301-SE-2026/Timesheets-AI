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
import { AiDashboardResponse } from '../../features/insights/models/ai-insights.model';
import { PersonalInsightsResponse } from '../../features/insights/models/ai-insights.model';

@Injectable({
  providedIn: 'root',
})
export class InsightsService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/insights`;

  getAiDashboard(): Observable<AiDashboardResponse> {
    return this.http.get<AiDashboardResponse>(`${this.baseUrl}/ai`);
  }
  getCalendarVsTracked(from: string, to: string): Observable<{connected: boolean; calendarHours: number; trackedHours: number; unmatchedHours: number}> {
    const params = new HttpParams().set('from', from).set('to', to);
    return this.http.get<any>(`${environment.apiUrl}/calendar/vs-tracked`, { params });
  }

  getJiraVsLogged(): Observable<{ticket: string; estimateHours: number; loggedHours: number}[]> {
    return this.http.get<any>(`${environment.apiUrl}/integrations/jira/vs-logged`);
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
