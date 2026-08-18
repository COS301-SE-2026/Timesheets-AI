import { CommonModule } from '@angular/common';
import { Component } from '@angular/core';
import { FullCalendarComponent, FullCalendarModule} from '@fullcalendar/angular';
import { CalenderOptions } from '@fullcalendar/core';
import { dayGridPlugin} from '@fullcalendar/daygrid';
import { timeGridPlugin} from '@fullcalendar/timegrid';

@Component({
  selector: 'app-calendar',
  standalone: true,
  imports: [ CommonModule, FullCalendarModule],
  templateUrl: './calendar.component.html',
  styleUrl: './calendar.component.scss'
})

export class CalendarComponent {

}
