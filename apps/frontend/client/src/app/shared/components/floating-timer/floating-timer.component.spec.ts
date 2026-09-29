import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { FloatingTimerComponent } from './floating-timer.component';

describe('FloatingTimerComponent', () => {
  let component: FloatingTimerComponent;
  let fixture: ComponentFixture<FloatingTimerComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [FloatingTimerComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
      ],
    })
    .compileComponents();

    fixture = TestBed.createComponent(FloatingTimerComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
