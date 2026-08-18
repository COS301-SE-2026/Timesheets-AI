// Author: Cleopatra Kwenda
// Date: 2026-08-18
// Purpose: has the http services that fetch
// the calander data from backend(still mocking rn)
// Related Requirements: N/A

import { Injectable, inject } from "@angular/core";
import { HttpClient, HttpParams } from "@angular/common/http";
import { of, Observable } from "rxjs";
import { delay } from "rxjs/operators";
import{
    AppEvent,
    CalendarProvider
}from './calendar.model';

@Injectable({
    providedIn: 'root'
})

export class CalendarService{
    private http=inject(HttpClient);
    private readonly apiUrl= '/api/calendar';

    // mocking data rn
    private readonly mockOutlookEvents: AppEvent[]=[
        {
            id: '1',
            title: 'Daily StandUp (Outlook)',
            start: '2026-08-18T09:00:00',
            end: '2026-08-18T09:30:00',
            category: 'purple'
        },
        {
            id: '2',
            title: 'Sprint Planning',
            start: '2026-08-18T11:00:00',
            end: '2026-08-18T12:30:00',
            category: 'blue'
        },
        {
            id: '3',
            title: 'Architecture Discussion',
            start: '2026-08-18T14:00:00',
            end: '2026-08-18T15:00:00',
            category: 'green'
        },
    ];

    private readonly mockGoogleEvents: AppEvent[]=[
        {
            id: '101',
            title: 'Client Meeting (Google Meet)',
            start: '2026-08-18T10:00:00',
            end: '2026-08-18T11:00:00',
            category: 'orange'
        },
        {
            id: '102',
            title: 'UX Design Discussion',
            start: '2026-08-18T13:00:00',
            end: '2026-08-18T14:00:00',
            category: 'purple'
        },

    ];
}