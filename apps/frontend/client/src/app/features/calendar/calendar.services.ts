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
    CalendarProvider,
}from './calendar.model';


// BACKEND RESPONSES
export interface CalendarStatus{
    connected: boolean;
    provider: CalendarProvider| null;
    lastSyncedAt: string | null;
}

interface BackendCalendarEvent{
    title: string;
    startTime: string;
    endTime: string;
    externalEventId: string;
    participants: string[];
    provider: string;
}
@Injectable({
    providedIn: 'root'
})

export class CalendarService{
    private readonly http=inject(HttpClient);
    private readonly apiUrl= '/api/calendar';
    private readonly integrationsApiUrl = '/api/integrations';
    //the backend is now the one that determines which connected calendar provider is used
    getEvents(startTime: string, endTime: string, ): Observable<AppEvent[]>{
        const params= new HttpParams()
            .set('startTime',startTime)
            .set('endTime', endTime);

        return this.http.get<BackendCalendarEvent[]>(`${this.apiUrl}/events`, { params })
            .pipe(
                map(events => {
                    return events.map(
                        event => this.mapBackendEvent(event)
                    );
                })
            );
    }

    getCalendarStatus(): Observable<CalendarStatus>{
        return this.http.get<CalendarStatus>(
            `${this.apiUrl}/status`
        );
    }

    connectCalendar(provider: 'GOOGLE' | 'MICROSOFT'): Observable<string> {
        const providerPath =
            provider === 'MICROSOFT' ? 'microsoft' : 'google';

        return this.http.get(
            `${this.integrationsApiUrl}/${providerPath}/calendar/connect`,
            { responseType: 'text' }
        );
    }

    // disconnectGoogleCalendar(): Observable<void>{
    //     return this.http.post<void>(
    //         `${this.googleCalendarApiUrl}/disconnect`,
    //         {}
    //     );
    // }

    getEvent(externalEventId: string): Observable<AppEvent> {
        return this.http.get<BackendCalendarEvent>(
            `${this.apiUrl}/events/${encodeURIComponent(externalEventId)}`
        ).pipe(
            map(event => this.mapBackendEvent(event))
        );
    }

    // CONVERTS BACKEND DTO TO APPS INTERNAL CALENDAR MODEL
    private mapBackendEvent(event: BackendCalendarEvent): AppEvent{
        const category= this.getEventCategory(event.title);
        
        return{
            id: event.externalEventId,
            title: event.title,
            start: event.startTime,
            end: event.endTime,
            provider: this.mapProvider(event.provider),
            participants: event.participants ?? [],
            category,
            categoryLabel: this.getCategoryLabel(category)
        };
    }

    private getCategoryLabel(category: AppEvent['category']): string {
        const labels: Record<string, string> = {
            meetings: 'Meeting',
            work: 'Work',
            calls: 'Call',
            deadline: 'Deadline'
        };

        return category ? labels[category] || 'Event' : 'Event';
    }

    private mapProvider(provider: string): CalendarProvider {
        switch (provider) {
            case 'MICROSOFT_CALENDAR':
                return 'microsoft';

            case 'GOOGLE_CALENDAR':
                return 'google';

            default:
                throw new Error(`Unsupported calendar provider: ${provider}`);
        }
    }

    private getEventCategory(title: string): AppEvent['category']{
        const normalizedTitle=title.toLowerCase();

        if( normalizedTitle.includes('deadline') || normalizedTitle.includes('due')){
            return 'deadline';
        }

        if( normalizedTitle.includes('call') || normalizedTitle.includes('check-in') || normalizedTitle.includes('check in')){
            return 'calls';
        }

        if( normalizedTitle.includes('sprint planning') || normalizedTitle.includes('sprint duration') || normalizedTitle.includes(' work')){
            return 'work';
        }

        return 'meetings';

    }
}