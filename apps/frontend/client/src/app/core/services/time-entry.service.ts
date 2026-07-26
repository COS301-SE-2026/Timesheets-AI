/*
This handles the CRUD operations for manual time entries against time-entry-controller
entryType is typed as a union ('MANUAL' | 'TIMER') to match the 
entryType CHECK constraint on the time_entries table, this catches a 
bad value at compile time intead of a 500 at runtime

Author: Zamokuhle Zwane
Date: 23 July 2026

Patched: Zamokuhle Zwane, 25 July 2026
fixed a typo, was entryTime and not entryType.
*/

import { Injectable, inject } from "@angular/core";
import { HttpClient, HttpErrorResponse } from "@angular/common/http";
import { Observable, throwError } from "rxjs";
import { catchError } from "rxjs/operators";

export interface TimeEntryResponse {
    id: string;
    timesheetId: string;
    workspaceMemberId: string;
    projectId: string;
    taskId: string;
    startTime: string;
    endTime: string;
    durationMinutes: number;
    entryType: 'MANUAL' | 'TIMER'; //matches the CHECK constraint, was entryTime
    description: string;
    isDeleted: boolean;
    createdAt: string;
    updatedAt: string;
}

export interface TimeEntryRequest {
    projectId: string;
    taskId: string;
    startTime:string;
    endTime: string;
    durationSeconds: number;
    entryType: 'MANUAL' | 'TIMER';
    description: string;
}

@Injectable ({providedIn: 'root'})
export class TimeEntryService {
    private readonly http = inject(HttpClient);

    private readonly baseUrl = '/api/time-entries';
    //gets every entry thst belongs to the logged in user
    getMyEntries(): Observable<TimeEntryResponse[]>{
        return this.http.get<TimeEntryResponse[]>(`${this.baseUrl}/me`).pipe(
            catchError(this.handleError('getMyEntries'))
        );
    }

    //gets a single entry by id, its use when opening the edit modal
    getEntry(id: string): Observable<TimeEntryResponse>{
        return this.http.get<TimeEntryResponse>(`${this.baseUrl}/${id}`).pipe(
            catchError(this.handleError('getEntry'))
        );
    }

    //creates a manual time entry, entryType should be 'MANUAL' when called from this flow
    createEntry(entry: TimeEntryRequest): Observable<TimeEntryResponse>{
        return this.http.post<TimeEntryResponse>(this.baseUrl, entry).pipe(
            catchError(this.handleError('createEntry'))
        );
    }

    //updates an existing entry, sends the full object since backend expects a full replace not patch
    updateEntry(id:string, entry: TimeEntryRequest): Observable<TimeEntryResponse>{
        return this.http.put<TimeEntryResponse>(`${this.baseUrl}/${id}`, entry).pipe(
            catchError(this.handleError('updateEntry'))
        );
    }
    
    //deletes an entry, backend will do a soft delete using the isDeleted flag, not a hard delete
    deleteEntry(id:string): Observable<void>{
        return this.http.delete<void>(`${this.baseUrl}/${id}`).pipe(
            catchError(this.handleError('deleteEntry'))
        );
    }

    //same pattern as TimerService, logs the failed operation then rethrows for the caller
    private handleError(operation:string){
        return(error: HttpErrorResponse) => { //was : (error:any)
            console.error(`[TimeEntryService] ${operation} failed:`, {
                status: error.status,
                message: error.message,
                url: error.url,
            });
            return throwError(() => error);
        };
    }
}