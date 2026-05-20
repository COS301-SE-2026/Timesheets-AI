import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ComponentShowcaseComponent } from './component-showcase.component';

describe('ComponentShowcaseComponent', () => {
  let component: ComponentShowcaseComponent;
  let fixture: ComponentFixture<ComponentShowcaseComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ComponentShowcaseComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(ComponentShowcaseComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
