import { CommonModule, DatePipe } from '@angular/common';
import { Component, inject, signal, OnInit, ViewChild} from '@angular/core';
import {  EventClickArg, FullCalendarComponent, FullCalendarModule, CalendarOptions} from '@fullcalendar/angular';
import  dayGridPlugin from '@fullcalendar/daygrid';
import timeGridPlugin from '@fullcalendar/timegrid';
import interactionPlugin from '@fullcalendar/interaction';
import { CalendarProvider, AppEvent } from './calendar.model';
import { CalendarService } from './calendar.services';
export type CalendarView= 'dayGridMonth' | 'timeGridWeek' | 'timeGridDay';
@Component({
  selector: 'app-calendar',
  standalone: true,
  imports: [ CommonModule, FullCalendarModule, DatePipe],
  templateUrl: './calendar.component.html',
  styleUrl: './calendar.component.scss'
})


export class CalendarComponent implements OnInit {
  @ViewChild('calendar')
  calendarComponent!: FullCalendarComponent;

  private calendarService= inject(CalendarService);

  activeView= signal<CalendarView>('dayGridMonth');
  provider= signal<CalendarProvider>( 'outlook');
  isSyncing= signal<boolean>(false);
  selectedEvent= signal<AppEvent | null>(null);
  currentDateTitle= signal<string>('');

  // HEADER PILL STUFF
  isConnected= signal<boolean>(false);
  lastSyncedLabel= signal<string | null>('synced just now');

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
    events: [],
    eventClick: (info: EventClickArg)=> this.handleEventClick(info),

    // ADDING THE FC COLOURS
    eventClassName:(arg)=>{
      const category= arg.event.extendedProps['category'];
      return category ? [`fc-event-${category}`]: [];
    },

    eventContent:(arg)=> {
      this.renderEventContent(arg)
    },

    datesSet:(dateInfo)=>{
      this.currentDateTitle.set(dateInfo.view.title);
    }
  };

  
  ngOnInit(): void{
    this.loadEvents();
  }

  loadEvents(): void{
    this.calendarService.getEvents(this.provider(), '', '').subscribe(
    {
      next: (events)=>{
        const api= this.calendarComponent?.getApi();
        if(api){
          api.removeAllEventSources();
          api.addEventSource(events);
        }else{
          this.calendarOptions.events= events;
        }

        this.isConnected.set(true);

        this.lastSyncedLabel.set(
          this.formatSyncedLabel(
            new Date().toISOString()
          )
        );
      },

      error:()=>{
        this.isConnected.set(false);
      }
    });
  }

  changeView(view: CalendarView): void{
    this.activeView.set(view);
    this.calendarComponent.getApi().changeView(view);
  }

  navigate(direction: 'prev' | 'next' | 'today'):void{
    const calandar= this.calendarComponent.getApi();

    if(direction=== 'prev') calandar.prev();
    if(direction=== 'next') calandar.next();
    if(direction=== 'today') calandar.today();
  }

  syncCalendar(): void{
    this.isSyncing.set(true);

    this.calendarService.getEvents(this.provider(),'', '').subscribe({
      next: (events)=>{
        const api= this.calendarComponent?.getApi();
        if(api){
          api.removeAllEventSources();
          api.addEventSource(events);
        }
          this.isSyncing.set(false);
          this.isConnected.set(true);

          this.lastSyncedLabel.set(
            this.formatSyncedLabel(
              new Date().toISOString()
            )
          );
        },
        error:()=> {
          this.isSyncing.set(false);
          this.isConnected.set(false);
        }
    });
  }

  handleEventClick(info: EventClickInfo): void{
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
                <span class="event-title">
                  ${arg.timeText}
                </span>
              `: ''
            }
          </div>`
      };
    }

    return true;
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
