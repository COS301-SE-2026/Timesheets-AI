// Author: Cleopatra Kwenda
// Date: 2026-08-18
// Purpose: hold the data models the helper function for 
// the calendar page with objects for google and outlook
// Related Requirements: N/A

export type CalendarProvider='outlook' | 'google';

export type EventCategory= | 'meetings' | 'work' | 'calls'| 'deadline';

export interface AppEvent {
    id:string;
    title: string;
    start: string;
    end: string;
    description?: string;
    location?: string;
    provider: CalendarProvider;
    category?:  EventCategory;
    categoryLabel?:string;

    organizer?:{
        name?: string;
        email?: string;
    }
}