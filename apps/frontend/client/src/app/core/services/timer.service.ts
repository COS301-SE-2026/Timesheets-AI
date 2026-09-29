/*
This file handles the al; live timer operations such as, start, pause, resume, stop, discard
against the timer-controller endpoints as seen on the Swagger. It will keeps the timer state separate from
manual time entry CRUD.4

Author: Zamokuhle Zwane
Date: 23 July 2026

Patched: Zamokuhle Zwane, 03 August 2026
I fixed the problem with the timer not showing up on the log time page,
it was because the timer component was not being rendered on the log time page
so i added it to the log time page and it now shows up correctly
 

Patched: Cleopatra Kwenda, 29 Sept 2026
adding the hovering timer logic/feature*/

import { Injectable, inject, signal, computed } from '@angular/core';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { time } from 'node:console';

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
  }| null;

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
  createdTimeEntry: {
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
    startTime: string;
    endTime: string;
    durationMinutes: number;
    status: string;
  };
}

export interface StartTimerRequest {
  projectId: string;
  taskId: string| null;
}

@Injectable({ providedIn: 'root' })
export class TimerService {
  private readonly http = inject(HttpClient);

  /*the proxy.conf.json already routes /api to spring boot, so no need for
    full host here, its the same as the authservice
    */
  private readonly baseUrl = '/api/timers';

  // SHARED TIMER SHANDIES//
  readonly activeTimer= signal<ActiveTimerResponse| null>(null);
  readonly elapsedSeconds= signal(0);
  readonly isTimerPaused= signal(false);
  private timerIntervalId: ReturnType<typeof setInterval>| null= null;
  readonly openTimerPanel= signal(false);
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

  requestOpenTimerPanel(): void{
    this.openTimerPanel.set(true);
  }

  closeTimerPanel(): void{
    this.openTimerPanel.set(false);
  }
  
  //it'll stop the time and converts it to a real time entry, and will return the created entry
  stopTimer(): Observable<StopTimerResponse> {
    return this.http
      .post<StopTimerResponse>(`${this.baseUrl}/stop`, {})
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

  // STATE MANAGING
  setActiveTimer(timer: ActiveTimerResponse| null): void{
    this.activeTimer.set(timer);

    if(!timer){
      this.stopElapsedInterval();
      this.elapsedSeconds.set(0);
      this.isTimerPaused.set(false);
      return;
    }

    this.isTimerPaused.set(timer.isPaused?? false);
    if(timer.isPaused){
      this.stopElapsedInterval();
      this.elapsedSeconds.set(timer.elapsedSeconds?? 0);
    }else{
      this.startElapsedInterval(timer.elapsedSeconds?? 0);
    }
  }

  clearActiveTimer():void {
    this.activeTimer.set(null);
    this.elapsedSeconds.set(0);
    this.isTimerPaused.set(false);
    this.stopElapsedInterval();
  }

  // TIMER DISPLAY
  private startElapsedInterval(intialElapsed: number): void{
    this.stopElapsedInterval();

    this.elapsedSeconds.set(intialElapsed);

    const clientStartTime= Date.now()- intialElapsed*1000;

    this.timerIntervalId= setInterval(()=> {
      this.elapsedSeconds.set(
        Math.max(
          0, Math.floor((Date.now()- clientStartTime)/1000)
        )
      );
    }, 1000);
  }

  private stopElapsedInterval(): void{
    if(this.timerIntervalId){
      clearInterval(this.timerIntervalId);
      this.timerIntervalId= null;
    }
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
