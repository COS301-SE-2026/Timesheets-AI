import { ComponentFixture, TestBed } from '@angular/core/testing';

import { TimesheetAiAssistantComponent } from './timesheet-ai-assistant.component';

describe('TimesheetAiAssistantComponent', () => {
  let component: TimesheetAiAssistantComponent;
  let fixture: ComponentFixture<TimesheetAiAssistantComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [TimesheetAiAssistantComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(TimesheetAiAssistantComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
