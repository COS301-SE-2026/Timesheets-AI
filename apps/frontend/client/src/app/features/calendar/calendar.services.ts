// Author: Cleopatra Kwenda
// Date: 2026-08-18
// Purpose: has the http services that fetch
// the calander data from backend(still mocking rn)
// Related Requirements: N/A

import { Injectable, inject } from "@angular/core";
import { HttpClient, HttpParams } from "@angular/common/http";
import { Observable } from "rxjs";
import{
    AppEvent,
    CalendarProvider
}from './calendar.model';


// BACKEND RESPONSES
export interface CalendarStatus{
    connected: boolean;
    provider: string;
    lastSyncedAt: string | null;
}

interface CalendarEventsResponse{
    events: AppEvent[];
    
}
@Injectable({
    providedIn: 'root'
})

export class CalendarService{
    private http=inject(HttpClient);
    private readonly apiUrl= '/api/calendar';
    private readonly googleCalendarApiUrl= '/api/integrations/google/calendar';

    // mocking data rn
    private readonly mockOutlookEvents: AppEvent[]=[
        {
            id: '1',
            title: 'Daily StandUp (Outlook)',
            start: '2026-08-18T09:00:00',
            end: '2026-08-18T09:30:00',
            provider: 'outlook',
            category: 'purple'
        },
        {
            id: '2',
            title: 'Sprint Planning',
            start: '2026-08-18T11:00:00',
            end: '2026-08-18T12:30:00',
            provider: 'outlook',
            category: 'blue',
            location: 'Boardroom A'
        },
        {
            id: '3',
            title: 'Architecture Discussion',
            start: '2026-08-18T14:00:00',
            end: '2026-08-18T15:00:00',
            provider: 'outlook',
            category: 'green'
        },
    ];

    private readonly mockGoogleEvents: AppEvent[]=[
        {
            id: '101',
            title: 'Client Meeting (Google Meet)',
            start: '2026-08-18T10:00:00',
            end: '2026-08-18T11:00:00',
            category: 'orange',
            provider: 'google'
        },
        {
            id: '102',
            title: 'UX Design Discussion',
            start: '2026-08-18T13:00:00',
            end: '2026-08-18T14:00:00',
            provider: 'google' 
        },

    ];

    getEvents(
        provider: CalendarProvider,
        start: string,
        end: string
    ): Observable<AppEvent[]>{
        let params= new HttpParams();

        if (start){
            params=params.set('start', start);
        }
        if (end){
            params=params.set('end', end);
        }

        return this.http.get<CalendarEventsResponse>(
            `${this.calendarApiUrl}/events`,
            { params }
        ).pipe(
            mapResponse=> mapResponse.events
        );
        const selectedEvents= provider === 'outlook'? this.mockOutlookEvents: this.mockGoogleEvents;
        return of( selectedEvents).pipe(delay(200));
    }
}