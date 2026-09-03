// Author: Cleopatra Kwenda
// Date: 2026-08-18
// Purpose: has the http services that fetch
// the calander data from backend(still mocking rn)
// Related Requirements: N/A

import { Injectable, inject } from "@angular/core";
import { HttpClient, HttpParams } from "@angular/common/http";
import { Observable, map } from "rxjs";
import{
    AppEvent,
}from './calendar.model';


// BACKEND RESPONSES
// export interface CalendarStatus{
//     connected: boolean;
//     provider: string;
//     lastSyncedAt: string | null;
// }

interface BackenCalendarEvent{
    title: string;
    startTime: string;
    endTime: string;
    externalEventId: string;
}
@Injectable({
    providedIn: 'root'
})

export class CalendarService{
    private http=inject(HttpClient);
    private readonly apiUrl= '/api/calendar';
    private readonly googleCalendarApiUrl= '/api/integrations/google/calendar';

    // mocking data rn
    // private readonly mockOutlookEvents: AppEvent[]=[
    //     {
    //         id: '1',
    //         title: 'Daily StandUp (Outlook)',
    //         start: '2026-08-18T09:00:00',
    //         end: '2026-08-18T09:30:00',
    //         provider: 'outlook',
    //         category: 'meetings'
    //     },
    //     {
    //         id: '2',
    //         title: 'Sprint Planning',
    //         start: '2026-08-18T11:00:00',
    //         end: '2026-08-18T12:30:00',
    //         provider: 'outlook',
    //         category: 'meetings',
    //         location: 'Boardroom A'
    //     },
    //     {
    //         id: '3',
    //         title: 'Architecture Discussion',
    //         start: '2026-08-18T14:00:00',
    //         end: '2026-08-18T15:00:00',
    //         provider: 'outlook',
    //         category: 'work'
    //     },
    // ];

    // private readonly mockGoogleEvents: AppEvent[]=[
    //     {
    //         id: '101',
    //         title: 'Client Meeting (Google Meet)',
    //         start: '2026-08-18T10:00:00',
    //         end: '2026-08-18T11:00:00',
    //         category: 'call',
    //         provider: 'google'
    //     },
    //     {
    //         id: '102',
    //         title: 'UX Design Discussion',
    //         start: '2026-08-18T13:00:00',
    //         end: '2026-08-18T14:00:00',
    //         provider: 'google' 
    //     },

    // ];

    getEvents(
        // provider: CalendarProvider,
        startTime: string,
        endTime: string
    ): Observable<AppEvent[]>{
        const params= new HttpParams()
            .set('startTime',startTime)
            .set('endTime', endTime);

        return this.http.get<BackenCalendarEvent[]>(
            `${this.apiUrl}/events`,
            { params }
        ).pipe(
            map(events=> events.map(
                event=> this.mapBackendEvent(event)
            ))
        );
        // const selectedEvents= provider === 'outlook'? this.mockOutlookEvents: this.mockGoogleEvents;
        // return of( selectedEvents).pipe(delay(200));
    }

    // getGoogleConnectionStatus(): Observable<CalendarStatus>{
    //     return this.http.get<CalendarStatus>(
    //         `${this.googleCalendarApiUrl}/status`
    //     );
    // }

    connectGoogleCalendar(): Observable<string>{
        return this.http.get(
            `${this.googleCalendarApiUrl}/connect`,
            { responseType: 'text'}
        );
    }

    // disconnectGoogleCalendar(): Observable<void>{
    //     return this.http.post<void>(
    //         `${this.googleCalendarApiUrl}/disconnect`,
    //         {}
    //     );
    // }

    getEvent(externalEventId:string): Observable<AppEvent>{
        return this.http.get<BackenCalendarEvent>(
            `${this.apiUrl}/events/${externalEventId}`
        ).pipe(
            map(event=> this.mapBackendEvent(event))
        );
    }

    // CONVERTS BACKEND DTO TO APPS INTERNAL CALENDAR MODEL
    private mapBackendEvent(event: BackenCalendarEvent): AppEvent{
        return{
            id: event.externalEventId,
            title: event.title,
            start: event.startTime,
            end: event.endTime,

            // BACKEND DOESNT PROVIDE THESE FIELDS
            provider: 'google',
            category: 'meetings'
        };
    }
}