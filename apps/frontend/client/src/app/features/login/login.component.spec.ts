import { ComponentFixture, TestBed } from '@angular/core/testing';
import { LoginComponent } from './login.component';
import { provideRouter } from '@angular/router';

describe('LoginComponent', () => {

  let component: LoginComponent;
  let fixture: ComponentFixture<LoginComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [LoginComponent],
      providers: [provideRouter([])]
    }).compileComponents();

    fixture = TestBed.createComponent(LoginComponent);
    component = fixture.componentInstance;

    fixture.detectChanges();
  });

  it('should create login component', () => {
    expect(component).toBeTruthy();
  });


  it('should render login page content correctly', () => {
    const compiled = fixture.nativeElement as HTMLElement;

    /** Ensures hero section exists and contains greeting text */
    expect(compiled.querySelector('.hero-title')).toBeTruthy();
    expect(compiled.querySelector('.hero-title')?.textContent)
      .toContain('Welcome back');

    /** Ensures login CTA button exists (not exact wording) */
    expect(compiled.querySelector('.submit-button')).toBeTruthy();
  });

  it('should submit form when valid', () => {
    jest.useFakeTimers();

    const comp = component as any;
    const spy = jest.spyOn(console, 'info').mockImplementation();

    comp.loginForm.setValue({
      email: 'john@company.com',
      password: 'Password1',
      remember: false
    });

    comp.onSubmit();

    expect(comp.submitted).toBe(true);
    expect(comp.loading).toBe(true);

    jest.advanceTimersByTime(1000);

    expect(comp.loading).toBe(false);
    expect(comp.errorMessage)
      .toBe('Demo only — no backend connected yet.');

    spy.mockRestore();
    jest.useRealTimers();
  });

  it('should show email error for invalid email format', () => {
    const comp = component as any;

    comp.loginForm.controls['email'].setValue('wrong-email');
    comp.loginForm.controls['email'].markAsTouched();

    expect(comp.showEmailError).toBe(true);
    expect(comp.emailErrorMessage).toBe('Enter a valid email address.');
  });

  it('should clear email error when email becomes valid', () => {
    const comp = component as any;

    comp.loginForm.controls['email'].setValue('john@company.com');
    comp.loginForm.controls['email'].markAsTouched();

    expect(comp.showEmailError).toBe(false);
    expect(comp.emailErrorMessage).toBe('');
  });

  it('should show password required error', () => {
    const comp = component as any;

    comp.loginForm.controls['password'].setValue('');
    comp.loginForm.controls['password'].markAsTouched();

    expect(comp.showPasswordError).toBe(true);
    expect(comp.passwordErrorMessage).toBe('Password is required.');
  });

  it('should clear password error when valid password is entered', () => {
    const comp = component as any;

    comp.loginForm.controls['password'].setValue('Password1');
    comp.loginForm.controls['password'].markAsTouched();

    expect(comp.showPasswordError).toBe(false);
    expect(comp.passwordErrorMessage).toBe('');
  });

  it('should toggle password visibility', () => {
    const comp = component as any;

    expect(comp.showPassword).toBe(false);

    comp.togglePassword();
    expect(comp.showPassword).toBe(true);

    comp.togglePassword();
    expect(comp.showPassword).toBe(false);
  });

  it('should submit form when valid', () => {
    const comp = component as any;
    const spy = jest.spyOn(console, 'info').mockImplementation();

    comp.loginForm.setValue({
      email: 'john@company.com',
      password: 'Password1',
      remember: false
    });

    comp.onSubmit();

    expect(comp.loginForm.valid).toBe(true);
    expect(comp.submitted).toBe(true);
    expect(comp.loading).toBe(true);

    spy.mockRestore();
  });

  it('should show validation errors when submitting invalid form', () => {
    const comp = component as any;

    comp.onSubmit();

    expect(comp.showEmailError).toBe(true);
    expect(comp.showPasswordError).toBe(true);
  });

  it('should trigger validation when submit is clicked', () => {
    const comp = component as any;

    comp.onSubmit();

    expect(comp.submitted).toBe(true);
    expect(comp.loginForm.invalid).toBe(true);
  });

  it('should match login page snapshot', () => {
    const compiled = fixture.nativeElement as HTMLElement;

    expect(compiled).toMatchSnapshot();
  });

});