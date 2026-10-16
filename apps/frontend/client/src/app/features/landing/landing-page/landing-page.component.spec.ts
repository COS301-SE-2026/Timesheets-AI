import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { LandingPageComponent } from './landing-page.component';

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
  ])('should display %s', (text) => {
    const compiled = fixture.nativeElement;
    expect(compiled.textContent).toContain(text);
  });

  it('should show 4 feature cards', () => {
    const compiled = fixture.nativeElement;
    const cards = compiled.querySelectorAll('.features-card');
    expect(cards).toHaveLength(4);
  });

  it('should show company logos', () => {
    const compiled = fixture.nativeElement;
    const logos = compiled.querySelectorAll('.logos-container img');
    expect(logos).toHaveLength(4);
  });

  it('should render the capability marquee with pills', () => {
    const compiled = fixture.nativeElement;
    const pills = compiled.querySelectorAll('.marquee-pill');
    expect(pills.length).toBeGreaterThan(0);
    // two identical groups (one for a11y-hidden duplicate)
    expect(compiled.querySelectorAll('.marquee-group')).toHaveLength(2);
  });

  it('should render the project forecast section', () => {
    const compiled = fixture.nativeElement;

    expect(compiled.querySelector('.forecast-section')).toBeTruthy();
    expect(compiled.querySelector('.forecast-panel')).toBeTruthy();
    expect(compiled.querySelector('.ai-callout')).toBeTruthy();

    const bars = compiled.querySelectorAll('.sprint-row');
    expect(bars).toHaveLength(4);

    const points = compiled.querySelectorAll('.forecast-points li');
    expect(points).toHaveLength(3);
  });

  it('should render mission and vision cards', () => {
    const compiled = fixture.nativeElement;
    const cards = compiled.querySelectorAll('.mission-card');
    expect(cards).toHaveLength(2);
  });

  it('should render impact stats', () => {
    const compiled = fixture.nativeElement;
    const stats = compiled.querySelectorAll('.stats-section .stat-item');
    expect(stats).toHaveLength(4);
  });

  it('should render the landing FAQ with platform questions', () => {
    const compiled = fixture.nativeElement;
    const items = compiled.querySelectorAll('.landing-faq .faq-item');
    expect(items.length).toBeGreaterThanOrEqual(5);
  });

  it('should render the closing call-to-action section', () => {
    const compiled = fixture.nativeElement;
    expect(compiled.querySelector('.cta-section')).toBeTruthy();
  });

  it('should render in-page section navigation with anchor links', () => {
    const compiled = fixture.nativeElement;
    const links = compiled.querySelectorAll('.nav-links a');
    expect(links.length).toBeGreaterThanOrEqual(4);

    const hrefs = Array.from(links).map((a: HTMLAnchorElement) => a.getAttribute('href'));
    expect(hrefs).toContain('#features');
    expect(hrefs).toContain('#forecast');
    expect(hrefs).toContain('#mission');
    expect(hrefs).toContain('#faq');
  });

  it('should render the site footer', () => {
    const compiled = fixture.nativeElement;
    expect(compiled.querySelector('.site-footer')).toBeTruthy();
  });

  it('should switch to help view and back to home view', () => {
    const compiled = fixture.nativeElement;

    // home view content present
    expect(compiled.querySelector('.forecast-section')).toBeTruthy();

    component.setActiveTab('help');
    fixture.detectChanges();

    // help view content present, home content gone
    expect(compiled.querySelector('.help-view-wrapper')).toBeTruthy();
    expect(compiled.querySelector('.forecast-section')).toBeFalsy();

    component.setActiveTab('home');
    fixture.detectChanges();
    expect(compiled.querySelector('.forecast-section')).toBeTruthy();
  });
});