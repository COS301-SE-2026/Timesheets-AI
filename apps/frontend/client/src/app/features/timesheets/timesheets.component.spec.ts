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

  it('should render the page title', () => {
      const title= fixture.debugElement.query(By.css('.page-header__title'));
      expect(title.nativeElement.textContent.trim()).toBe('Timesheets');
  });

  it('it should render status filter chips', () => {
    const chips = fixture.debugElement.queryAll(By.css('.status-filters__chip'));
    expect(chips.length).toBe(component.statusFilters.length);
    })

    expect(dayCells.length).toBe(component.days.length);
  });

  // checks that each task in the dataset is rendered in the table
  it('should render all task rows', () => {
    const rows = fixture.debugElement.queryAll(
      By.css('.tracking-data-entry-item-row')
    );

    expect(rows.length).toBe(component.tasks.length);
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