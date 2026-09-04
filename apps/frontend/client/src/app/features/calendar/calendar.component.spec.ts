import { ComponentFixture, TestBed } from '@angular/core/testing';
import { CalendarService } from './calendar.services';
import { provideRouter } from '@angular/router';
import { provideHttpClient } from '@angular/common/http';
import { of } from 'rxjs';

jest.mock('@fullcalendar/angular', ()=>{
  const angular= require('@angular/core');

  class MockFullCalendarComponent{
    getApi(){
        return{
          changeView: jest.fn(),
          refetchEvents: jest.fn(),
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
      getEvents: jest.fn().mockReturnValue(of([]))
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

  it('should use Google as default provider',()=>{
    expect(component.provider()).toBe('google');
  });

  it('should initially be disconnected', ()=>{
    expect(component.isConnected()).toBe(false);
  });
});
