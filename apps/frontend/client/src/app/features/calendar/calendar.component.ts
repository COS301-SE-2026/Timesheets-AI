import { CommonModule, DatePipe } from '@angular/common';
import { Component, inject, signal, ViewChild, OnInit} from '@angular/core';
import {  EventClickArg, CalendarOptions} from '@fullcalendar/core';
import { FullCalendarComponent, FullCalendarModule} from '@fullcalendar/angular';
import  dayGridPlugin from '@fullcalendar/daygrid';
import timeGridPlugin from '@fullcalendar/timegrid';
import interactionPlugin from '@fullcalendar/interaction';
import { CalendarProvider, AppEvent } from './calendar.model';
import { CalendarService } from './calendar.services';
import { HttpClient } from '@angular/common/http';
import { ActivatedRoute, Router} from '@angular/router';
export type CalendarView= 'dayGridMonth' | 'timeGridWeek' | 'timeGridDay';
@Component({
  selector: 'app-calendar',
  standalone: true,
  imports: [ CommonModule, FullCalendarModule, DatePipe],
  templateUrl: './calendar.component.html',
  styleUrl: './calendar.component.scss'
})


export class CalendarComponent implements OnInit{
  @ViewChild('calendar')
  calendarComponent!: FullCalendarComponent;

  private readonly calendarService= inject(CalendarService);
  private readonly http= inject(HttpClient);
  private readonly route= inject(ActivatedRoute);
  private readonly router= inject(Router);


  activeView= signal<CalendarView>('dayGridMonth');
  provider= signal<CalendarProvider>( 'google');
  isSyncing= signal<boolean>(false);
  selectedEvent= signal<AppEvent | null>(null);
  currentDateTitle= signal<string>('');

  // HEADER PILL STUFF
  isConnected= signal<boolean>(false);
  lastSyncedLabel= signal<string | null>(null);

  calendarOptions: CalendarOptions={
    plugins:[
      dayGridPlugin,
      timeGridPlugin,
      interactionPlugin
    ],

    initialView: 'dayGridMonth',
    headerToolbar: false,
    height: 'auto',
    editable: false,
    selectable: false,
    events: (fetchInfo, successCallback, failureCallback)=>{
      this.calendarService.getEvents(
        this.formatCalendarDate(fetchInfo.start),
        this.formatCalendarDate(fetchInfo.end)
      ).subscribe(
        {
          next: (events)=>{
            this.isConnected.set(true);
            this.lastSyncedLabel.set(
              this.formatSyncedLabel(new Date().toISOString())
            );
            successCallback(events);
          },
          error: (error)=>{
            console.error('Failed to load calendar evemts.', error);
            this.isConnected.set(false);
            failureCallback(error);
          }
        }
      );
    },

    eventClick: (info: EventClickArg)=> this.handleEventClick(info),

    // ADDING THE FC COLOURS
    eventClassNames:(arg)=>{
      const category= arg.event.extendedProps['category'] || 'meetings';
      return [`fc-event-${category}`];
    },

    eventContent:(arg)=> this.renderEventContent(arg),
    dayHeaderContent:(arg)=> this.renderDayHeaderContent(arg),

    slotLabelFormat:{
      hour:'2-digit',
      minute: '2-digit',
      hour12: false
    },

    slotMinTime: '06:00:00',
    slotMaxTime: '17:30:00',
    slotDuration: '0:15:00',
    slotLabelInterval: '01:00:00',
    eventMinHeight: 40,
    eventShortHeight: 30,
    expandRows: true,
    dayMaxEventRows: true,

    datesSet:(dateInfo)=>{
      this.currentDateTitle.set(dateInfo.view.title);
    }
  };

  
  ngOnInit(): void{
    // CHECKING IF COMING BACK FROM OAUTH REDIRECT
    this.route.queryParams.subscribe(
      params=>{
        if(params['connected']=== 'true'){
          this.isConnected.set(true);
          this.syncCalendar();

          // CLEANING PARAM FROM BROWSER BAR
          this.router.navigate(
            [], {
              queryParams: {connected: null},
              queryParamsHandling: 'merge'
            }
          );
        }
      }
    );
    // const urlParams= new URLSearchParams(window.location.search);
    // if(urlParams.has('code') && urlParams.has('state')){
    //   this.isConnected.set(true);
    // }
  }

  changeView(view: CalendarView): void{
    this.activeView.set(view);
    this.calendarComponent.getApi().changeView(view);
  }

  connectGoogleCalendar():void{
    this.http.get('/api/integrations/google/calendar/connect',{
      responseType: 'text'
    }).subscribe(
      {
        next:(authUrl: string)=>{
          window.location.href= authUrl;
        },
        error:(error)=>{
          console.error('Failed to connect Google Calendar', error);
        }
      }
    );
  }

  private formatCalendarDate(date: Date): string{
    const year= date.getFullYear();
    const month= String(date.getMonth()+1).padStart(2, '0');
    const day= String(date.getDate()).padStart(2, '0');
    const hours= String(date.getHours()).padStart(2, '0');
    const minutes= String(date.getMinutes()).padStart(2, '0');
    const seconds= String(date.getSeconds()).padStart(2, '0');

    return `${year}-${month}-${day}T${hours}:${minutes}:${seconds}`;
  }

  navigate(direction: 'prev' | 'next' | 'today'):void{
    const calendar= this.calendarComponent.getApi();

    if(direction=== 'prev') calendar.prev();
    if(direction=== 'next') calendar.next();
    if(direction=== 'today') calendar.today();
  }

  syncCalendar(): void{
    this.isSyncing.set(true);

    const api= this.calendarComponent?.getApi();

    if(api){
      api.refetchEvents();
    }

    this.isSyncing.set(false);
  }

  handleEventClick(info: EventClickArg): void{
    const rawProps= info.event.extendedProps;
    const categoryKey= rawProps['category'] || 'meetings';

    this.selectedEvent.set({
      id: info.event.id,
      title: info.event.title,
      start: info.event.start?.toISOString() || '',
      end: info.event.end?.toISOString() || '',
      description: rawProps['description'],
      location: rawProps['location'],
      provider: this.provider(),
      category: categoryKey,
      categoryLabel: rawProps['categoryLabel'] || this.getCategoryLabel(categoryKey),
      organizer: rawProps['organizer']
    });
  }

  closeEventDetails(): void{
    this.selectedEvent.set(null);
  }

  private renderEventContent(arg: any){
    if(arg.view.type=== 'dayGridMonth'){
      const category= arg.event.extendedProps['category'] || 'meetings';

      return{
        html:`
          <div class="custom-event-pill">
            <span class="event-dot ${category}">
            </span>
            <span class="event-title">
              ${arg.event.title}
            </span>

            ${
              arg.timeText ? `
                <span class="event-time">
                  ${arg.timeText}
                </span>
              `: ''
            }
          </div>`
      };
    }

    return true;
  }

  private renderDayHeaderContent(arg: any){
    if(arg.view.type=== 'dayGridMonth'){
      return true;
    }

    const weekday=arg.date.toLocaleDateString('en-US', {weekday: 'short'}).toUpperCase();
    const dayNumber= arg.date.getDate();

    return{
      html:
        `<div class="custom-day-header">
          <span class="day-name">
            ${weekday}
          </span>
          <span class="day-number ${arg.isToday? 'is-today': ''}">
            ${dayNumber}
          </span>
        </div>
        
        `
    }
  }

  private getCategoryLabel(category: string){
    const labels: Record<string, string>={
      meetings: 'Meeting',
      work: 'Work',
      calls: 'Call',
      deadlines: 'Deadline'
    };

    return labels[category] || 'Event';
  }

  private formatSyncedLabel(isoDate: string):string{
    const synced=new Date(isoDate);
    const diffMs= Date.now()- synced.getTime();
    const diffMinutes= Math.round(diffMs/60000);
    if(diffMinutes< 1){
      return 'synced just now';
    }

    if(diffMinutes< 60){
      return `synced ${diffMinutes}m ago`;
    }

    const diffHours= Math.round(diffMinutes/60);
    if(diffHours< 24){
      return `synced ${diffHours}h ago`;
    }

    const diffDays= Math.round(diffHours/24);
    return `synced ${diffDays}d ago`;
    
  }
}
