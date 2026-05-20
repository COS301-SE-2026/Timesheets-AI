import { ComponentFixture, TestBed } from '@angular/core/testing';

import { TimesheetStatsComponent } from './timesheet-stats.component';

describe('TimesheetStatsComponent', () => {
  let component: TimesheetStatsComponent;
  let fixture: ComponentFixture<TimesheetStatsComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [TimesheetStatsComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(TimesheetStatsComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
