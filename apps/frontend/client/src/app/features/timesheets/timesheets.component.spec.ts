import { ComponentFixture, TestBed } from '@angular/core/testing';
import { TimesheetsComponent } from './timesheets.component';
import { By } from '@angular/platform-browser';

describe('TimesheetsComponent', () => {
  let fixture: ComponentFixture<TimesheetsComponent>;
  let component: TimesheetsComponent;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [TimesheetsComponent]
    }).compileComponents();

    fixture = TestBed.createComponent(TimesheetsComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  // ensures the component loads without crashing
  it('should create the component', () => {
    expect(component).toBeTruthy();
  });

  // verifies that all 7 days are rendered in the header row
  it('should render all days in header row', () => {
    const dayCells = fixture.debugElement.queryAll(
      By.css('.date-cell')
    );

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