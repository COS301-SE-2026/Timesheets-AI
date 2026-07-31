import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { LandingPageComponent } from './landing-page.component';
import { log } from 'node:console';

describe('LandingPageComponent', () => {
  let component: LandingPageComponent;
  let fixture: ComponentFixture<LandingPageComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [LandingPageComponent],
      providers: [provideRouter([])]
    })
    .compileComponents();

    fixture = TestBed.createComponent(LandingPageComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it.each([
    ['Time tracking'],
    ['Login'],
    ['Signup']
    ])('should display %s', (text)=> {
    const compiled= fixture.nativeElement;

    expect(compiled.textContent).toContain(text);
  });


  it('should show 4 feature cards', ()=> {
    const compiled= fixture.nativeElement;

    const cards=compiled.querySelectorAll('.features-card')

    expect(cards).toHaveLength(4);
  });

  it('should show company logos', ()=> {
    const compiled= fixture.nativeElement;

    const logos= compiled.querySelectorAll('.logos-container img')

    expect(logos).toHaveLength(4);
  });
});
