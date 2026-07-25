/*
This file handles the al; live timer operations such as, start, pause, resume, stop, discard
against the timer-controller endpoints as seen on the Swagger. It will keeps the timer state separate from
manual time entry CRUD.4

Author: Zamokuhle Zwane
Date: 23 July 2026
 */

import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Observable } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { throwError } from 'rxjs';

//so these mirror the schema on swagger exactly, so there's no silent mismatch later

export interface ActiveTimerResponse {
  id: string;
  project: {
    id: string;
    name: string;
  };
  task: {
    id: string;
    title: string;
  };
  startedAt: string;
  elapsedMinutes: number;
  elapsedSeconds: number;
  active: boolean;
  isPaused: boolean;
  pausedAt: string | null;
}

export interface StopTimerResponse {
  timerId: string;
  stoppedAt: string;
  durationMinutes: number;
  createdTimerEntry: {
    id: string;
    project: {
      id: string;
      name: string;
    };
    task: {
      id: string;
      title: string;
    };
    date: string;
    startTime: {
      hour: number;
      minute: number;
      second: number;
      nano: number;
    };
    endTime: {
      hour: number;
      minute: number;
      second: number;
      nano: number;
    };
    durationMinutes: number;
    status: string;
  };
}

export interface StartTimerRequest {
  projectId: string;
  taskId: string;
}

@Injectable({ providedIn: 'root' })
export class TimerService {
  private http = inject(HttpClient);

  /*the proxy.conf.json already routes /api to spring boot, so no need for
    full host here, its the same as the authservice
    */
  private readonly baseUrl = '/api/timers';
  //starts a new timer for a given project + task, backend hardcodes entryType to a TIMER
  startTimer(request: StartTimerRequest): Observable<ActiveTimerResponse> {
    return this.http
      .post<ActiveTimerResponse>(`${this.baseUrl}/start`, request)
      .pipe(catchError(this.handleError('startTimer')));
  }
  //this pauses the currently running timer, no body needed since backend tracks the active timer per user
  pauseTimer(): Observable<ActiveTimerResponse> {
    return this.http
      .post<ActiveTimerResponse>(`${this.baseUrl}/pause`, {})
      .pipe(catchError(this.handleError('pauseTimer')));
  }
  //resumes a paused timer by picking up from pauseAt
  resumeTimer(): Observable<ActiveTimerResponse> {
    return this.http
      .post<ActiveTimerResponse>(`${this.baseUrl}/resume`, {})
      .pipe(catchError(this.handleError('resumeTimer')));
  }
  //it'll stop the time and converts it to a real time entry, and will return the created entry
  stopTimer(): Observable<ActiveTimerResponse> {
    return this.http
      .post<ActiveTimerResponse>(`${this.baseUrl}/stop`, {})
      .pipe(catchError(this.handleError('stopTimer')));
  }
  //fetches whatever timer is currently active for the logged in user, used on page load refresh/load
  getActiveTimer(): Observable<ActiveTimerResponse> {
    return this.http
      .get<ActiveTimerResponse>(`${this.baseUrl}/active`)
      .pipe(catchError(this.handleError('getActiveTimer')));
  }

  //discards the active timer without creating a time entry for when someone starts by mistake
  discardTimer(): Observable<ActiveTimerResponse> {
    return this.http
      .delete<ActiveTimerResponse>(`${this.baseUrl}/discard`, {})
      .pipe(catchError(this.handleError('discardTimer')));

    /*so i create one central error handler so every call logs simlarily. this was recommended by 
    pattern angular's own docs recommend for httpclient error handling, check my draft file
    */
  }
  private handleError(operation: string) {
    return (error: HttpErrorResponse) => { //was: (error: any)
      console.error(`[TimerService] ${operation} failed:`, {
        status: error.status,
        message: error.message,
        url: error.url,
      });
      return throwError(() => error);
    };
  }
}
