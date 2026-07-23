/*
This file handles the al; live timer operations such as, start, pause, resume, stop, discard
against the timer-controller endpoints as seen on the Swagger. It will keeps the timer state separate from
manual time entry CRUD.4

Author: Zamokuhle Zwane
Date: 23 July 2026
 */

import { Injectable } from '@angular/core'; 
import { HttpClient } from '@angular/common/http';
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
        id:string; 
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

@Injectable ({ providedIn: 'root'})
export class TimerService {
    /*the proxy.conf.json already routes /api to spring boot, so no need for
    full host here, its the same as the authservice
    */
   private readonly baseUrl = '/api/timers';

   constructor(private http: HttpClient){}

   startTimer(request: StartTimerRequest): Observable <ActiveTimerResponse> {
    return this.http.post<ActiveTimerResponse>(`${this.baseUrl}/start`, request).pipe(
        catchError(this.handleError('startTimer'))
    );
   }
   pauseTimer(): Observable<ActiveTimerResponse>{
    return this.http.post<ActiveTimerResponse>(`${this.baseUrl}/pause`, {}).pipe(
        catchError(this.handleError('pauseTimer'))
    );
   }
   resumeTimer(): Observable<ActiveTimerResponse>{
    return this.http.post<ActiveTimerResponse>(`${this.baseUrl}/resume`, {}).pipe(
        catchError(this.handleError('resumeTimer'))
    );
   }
   stopTimer(): Observable<ActiveTimerResponse>{
    return this.http.post<ActiveTimerResponse>(`${this.baseUrl}/stop`, {}).pipe(
        catchError(this.handleError('stopTimer'))
    );
   }
   discardTimer(): Observable<ActiveTimerResponse>{
    return this.http.post<ActiveTimerResponse>(`${this.baseUrl}/discard`, {}).pipe(
        catchError(this.handleError('discardTimer'))
    );

    /*so i create one central error handler so every call logs simlarily. this was recommended by 
    pattern angular's own docs recommend for httpclient error handling, check my draft file
    */
}
   private handleError(operation: string) {
    return (error:any) => {
        console.error(`[TimerService] ${operation} failed:`, {
            status: error.status,
            message: error.message,
            url: error.url,
        });
        return throwError(() => error);
        
    };
   }
}
