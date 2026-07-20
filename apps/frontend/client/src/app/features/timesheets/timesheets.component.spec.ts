import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { By } from '@angular/platform-browser';
import {TimesheetsComponent } from './timesheets.component'

describe('TimesheetsComponent', () => {
  let fixture: ComponentFixture<TimesheetsComponent>;
  let component: TimesheetsComponent;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [TimesheetsComponent],
      providers: [provideRouter([])]
    }).compileComponents();

    fixture = TestBed.createComponent(TimesheetsComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();
  });

  // ensures the component loads without crashing
  it('should create the component', () => {
    expect(component).toBeTruthy();
  });

  //ensure title loads
  it('should render the page title', () => {
      const title= fixture.debugElement.query(By.css('.page-header__title'));
      expect(title.nativeElement.textContent.trim()).toBe('Timesheets');
  });

  //ensures status chips load
  it('it should render status filter chips', () => {
    const chips = fixture.debugElement.queryAll(By.css('.status-filters__chip'));
    expect(chips.length).toBe(component.statusFilters.length);
    })

  });

  //ensures summary cards load
  it('should render the summary card for the selected week', () => {
    const card = fixture.debugElement.query(By.css('.summary-card'));
    expect(card).toBeTruthy();
    expect(card.nativeElement.textContent).toCobtain('Week 29');
  });

  it('should render weekly columns in the entry table', () => {
    const dayHeaders = fixture.debugElement.queryAll(By.css('.col-day__name'));
    expect(dayHeaders.length).toBe(component.days().length);
  });



  // ensures the current day column is visually highlighted
  it('should highlight today column', () => {
    const highlightedCells = fixture.debugElement.queryAll(
      By.css('.highlighted-today-column')
    );

    expect(highlightedCells.length).toBeGreaterThan(0);
  });

  // confirms that the progress bar component is rendered
  it('should render progress bar component', () => {
    const progress = fixture.debugElement.query(
      By.css('app-progress-bar')
    );

    expect(progress).toBeTruthy();
  });

  // confirms that the header component is rendered
  it('should render header component', () => {
    const header = fixture.debugElement.query(
      By.css('app-header')
    );

    expect(header).toBeTruthy();
  });
});