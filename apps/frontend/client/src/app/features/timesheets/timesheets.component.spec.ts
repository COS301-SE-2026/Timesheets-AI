import { ComponentFixture, TestBed } from '@angular/core/testing';
import { TimesheetsComponent } from './timesheets.component';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';

describe('TimesheetsComponent', () => {
  let fixture: ComponentFixture<TimesheetsComponent>;
  let component: TimesheetsComponent;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [TimesheetsComponent],
      providers: [provideHttpClient(), provideHttpClientTesting()],
    }).compileComponents();

    fixture = TestBed.createComponent(TimesheetsComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  // ensures the component loads without crashing
  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
