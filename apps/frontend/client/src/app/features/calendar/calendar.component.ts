import { CommonModule } from '@angular/common';
import { Component, inject, viewChild } from '@angular/core';
import { FullCalendarComponent, FullCalendarModule} from '@fullcalendar/angular';
import { CalenderOptions } from '@fullcalendar/core';
import { dayGridPlugin} from '@fullcalendar/daygrid';
import { timeGridPlugin} from '@fullcalendar/timegrid';
import { CalendarProvider } from './calendar.model';
import { CalendarService } from './calendar.services';
import { plugins } from 'chart.js';

@Component({
  selector: 'app-calendar',
  standalone: true,
  imports: [ CommonModule, FullCalendarModule],
  templateUrl: './calendar.component.html',
  styleUrl: './calendar.component.scss'
})

export class CalendarComponent {
  @viewChild('calendar')calendarComponent!: FullCalendarComponent;

  private calendarService= inject(CalendarService);

  activeView: CalendarView= 'dayGridMonth';
  provider: CalendarProvider= 'outlook';

  calendarOptions: CalenderOptions={
    plugins:[
      dayGridPlugin,
      timeGridPlugin
    ],

    initialView: 'dayGridMonth',
    headerToolbar: false,
    height: 'auto',
    editable: false,
    selectable: false,
    events: []
  };

  ngOnInit(): void{
    this.loadEvents();
  }

  loadEvents(): void{
    this.calendarService.getEvents(this.provider, '', '').subscribe(events=>
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

  
}
