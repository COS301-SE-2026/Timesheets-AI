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

  it('should display the logo', ()=> {
    const compiled= fixture.nativeElement;

    const logo= compiled.querySelector('.brand-logo');

    expect(logo).toBeTruthy();
  });

  it('should display the hero title', ()=> {
    const compiled= fixture.nativeElement;

    expect(compiled.textContent).toContain('Timesheets');
  });

  it('should display Login button', ()=> {
    const compiled= fixture.nativeElement;

    expect(compiled.textContent).toContain('Login');
  });

  it('should display Sign Up button', ()=> {
    const compiled= fixture.nativeElement;

    expect(compiled.textContent).toContain('Signup');
  });

  it('should show 4 feature cards', ()=> {
    const compiled= fixture.nativeElement;

    const cards=compiled.querySelectorAll('.features-card')

    expect(cards.length).toHaveLength(4);
  });

  it('should show company logos', ()=> {
    const compiled= fixture.nativeElement;

    const logos= compiled.querySelectorAll('.logos-container img')

    expect(logos.length).toHaveLength(4);
  });
});
