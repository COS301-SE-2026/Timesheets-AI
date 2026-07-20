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

  // Test if task rows get rendered
  it('should render all task rows', () => {
    const rows = fixture.debugElement.queryAll(By.css('.entry-table tbody tr'));
    expect(rows.length).toBe(component.tasks().length);
  });

  // Test if grand total renders
  it('should show the grand total in the footer', () => {
    const grandTotal = fixture.debugElement.query(By.css('.grand-total'));
    expect(grandTotal.nativeElement.textContent).toContain(component.grandTotal());
  });

  // Test if the submit timesheet button renders
it('should show Submit Timesheets for drafts', () => {
  const submit = ComponentFixture.debugElement.query(By.css('.detail-actions .btn--primary'));
  expect(submit).toBeTruthy();
  expect(submit.nativeElement.textContent).toContain('Submit Timesheet');
});