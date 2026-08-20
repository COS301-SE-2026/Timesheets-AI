import { CommonModule, DatePipe } from '@angular/common';
import { Component, inject, signal, OnInit, ViewChild} from '@angular/core';
import { EventClickInfo, FullCalendarComponent, FullCalendarModule, CalendarOptions} from '@fullcalendar/angular';
import  dayGridPlugin from 'fullcalendar/daygrid';
import timeGridPlugin from 'fullcalendar/timegrid';
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
  @ViewChild('calendar')calendarComponent!: FullCalendarComponent;

  private calendarService= inject(CalendarService);

  activeView= signal<CalendarView>('dayGridMonth');
  provider= signal<CalendarProvider>( 'outlook');
  isSyncing= signal<boolean>(false);
  selectedEvent= signal<AppEvent | null>(null);

  // HEADER PILL STUFF
  isConnected= signal<boolean>(false);
  lastSyncedLabel= signal<string | null>(null);

  calendarOptions: CalendarOptions={
    plugins:[
      dayGridPlugin,
      timeGridPlugin
    ],

    initialView: 'dayGridMonth',
    headerToolbar: false,
    height: 'auto',
    editable: false,
    selectable: false,
    events: [],
    eventClick: (info: EventClickInfo)=> this.handleEventClick(info),

    // ADDING THE FC COLOURS
    eventClass:(arg)=>{
      const category= arg.event.extendedProps['category'];
      return category && category !== 'blue'? `fc-event-${category}`: '';
    }
  };

  
  ngOnInit(): void{
    this.loadEvents();
  }

  loadEvents(): void{
    this.calendarService.getEvents(this.provider(), '', '').subscribe(events=>
    {
      const api= this.calendarComponent?.getApi();
      if(api){
        api.removeAllEventSources();
        api.addEventSource(events);
      }else{
        this.calendarOptions.events= events;
      }
    });
  }

  changeView(view: CalendarView): void{
    this.activeView.set(view);
    this.calendarComponent.getApi().changeView(view);
  }

  navigate(direction: 'prev' | 'next' | 'today'):void{
    const calander= this.calendarComponent.getApi();

    if(direction=== 'prev') calander.prev();
    if(direction=== 'next') calander.next();
    if(direction=== 'today') calander.today();
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
        },
        error:()=> this.isSyncing.set(false)
    });
  }

  handleEventClick(info: EventClickInfo): void{
    const rawProps= info.event.extendedProps;

    this.selectedEvent.set({
      id: info.event.id,
      title: info.event.title,
      start: info.event.start?.toISOString() || '',
      end: info.event.end?.toISOString() || '',
      description: rawProps['description'],
      location: rawProps['location'],
      provider: this.provider(),
      category: rawProps['category'],
      organizer: rawProps['organizer']
    });
  }

  closeEventDetails(): void{
    this.selectedEvent.set(null);
  }

  private formatSyncedLabel(isoDate: string):string{
    const synced=new Date(isoDate);
    const diffMs= Date.now()- synced.getTime();
    const diffMinutes= Math.round(diffMs/60000);
    if(diffMinutes< 1){
      return 'synced just now';
    }

    if(diffMinutes< 60){
      return 'synced ${diffMinutes}m ago';
    }

    const diffHours= Math.round(diffMinutes/60);
    if(diffHours< 24){
      return 'synced ${diffHours}h ago';
    }

    const diffDays= Math.round(diffHours/24);
    return 'synced ${diffDays}d ago';
    
  }
}
