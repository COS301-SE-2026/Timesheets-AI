/*
This file mirrors timesheets.dto.response.PersonalInsightsResponse.java field for field,
same convention as ai-insights.model.ts mirroring the ai-service dashboard schema

Author: Zamokuhle Zwane
Date: 04/09/2026
*/

export interface ProjectHours {
  projectId: string;
  projectName: string;
  hours: number;
  entryCount: number;
}

export interface TaskHours {
  taskId: string;
  taskTitle: string;
  hours: number;
  status: string;
}

export interface DailyTrend {
  date: string;
  hours: number;
  entryCount: number;
}

export interface PersonalInsightsResponse {
  totalHoursLogged: number;
  averageHoursPerDay: number;
  totalDaysLogged: number;
  hoursPerProject: ProjectHours[];
  hoursPerTask: TaskHours[];
  dailyTrend: DailyTrend[];
}