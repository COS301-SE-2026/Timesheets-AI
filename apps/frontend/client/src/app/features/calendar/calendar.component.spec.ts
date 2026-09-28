import { ComponentFixture, TestBed } from '@angular/core/testing';
import { CalendarService } from './calendar.services';
import { provideRouter } from '@angular/router';
import { provideHttpClient } from '@angular/common/http';
import { of } from 'rxjs';
import { connect } from 'node:http2';
import { lstatSync } from 'node:fs';

jest.mock('@fullcalendar/angular', ()=>{
  const angular= require('@angular/core');

  class MockFullCalendarComponent{
    getApi(){
        return{
          changeView: jest.fn(),
          prev: jest.fn(),
          next: jest.fn(),
          today: jest.fn(),
        };
      }
  }

  angular.Component({
      selector: 'full-calendar',
      standalone: true,
      template: '',
    })(MockFullCalendarComponent);

  class MockFullCalendarModule{}

  angular.NgModule({
      imports: [MockFullCalendarComponent],
      exports: [MockFullCalendarComponent],
    })(MockFullCalendarModule);

  return{
    FullCalendarModule: MockFullCalendarModule,
    FullCalendarComponent: MockFullCalendarComponent,
  };
});

jest.mock('@fullcalendar/daygrid', ()=> ({__esModule: true, default:{}}));
jest.mock('@fullcalendar/timegrid', ()=> ({__esModule: true, default:{}}));
jest.mock('@fullcalendar/interaction', ()=> ({__esModule: true, default:{}}));

const {
  CalendarComponent
}= require('./calendar.component');

describe('CalendarComponent', () => {
  let component: InstanceType<typeof CalendarComponent>;
  let fixture: ComponentFixture<InstanceType<typeof CalendarComponent>>;
  let mockCalendarService: jest.Mocked<Partial<CalendarService>>;

  beforeEach(async () => {
    mockCalendarService={
      getEvents: jest.fn().mockReturnValue(of([])),

      getCalendarStatus: jest.fn().mockReturnValue(
        of(
          {
            connected: false,
            provider: null,
            lastSyncedAt: null,
          }
        )
      ),
    }; 

    await TestBed.configureTestingModule({
      imports: [CalendarComponent],

      providers:[
        {
          provide: CalendarService,
          useValue: mockCalendarService,
        },
        provideRouter([]),
        provideHttpClient(),
      ],
    })
    .compileComponents();

    fixture = TestBed.createComponent(CalendarComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should initially have no calendar provider',()=>{
    expect(component.provider()).toBeNull();
  });

  it('should initially be disconnected', ()=>{
    expect(component.isConnected()).toBe(false);
  });
});
