/*
Covers the sign up request, verifies payload sent to /auth/register, the shape of the response on success
and that a token gets store, it doesnt return one, the user still has to verify(although we dont actually have a verification system) their email and login separately.
also covers errors, both when the backend sends a message and when it doesnt.

the HttpTestingController intercepts the request instead of hitting the actual backend, and the router is provided since AuthService injects its at the class level  for logout()
*/

import { TestBed, ComponentFixture } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { provideHttpClient } from '@angular/common/http';
import {
  HttpTestingController,
  provideHttpClientTesting,
} from '@angular/common/http/testing';
import {
  AuthService,
  RegisterRequest,
  RegisterResponse,
} from '../../../core/services/auth.service';
import { SignupComponent } from './signup.component';
import { FormGroup } from '@angular/forms';
import { first } from 'rxjs';

interface SignupComponentInternals {
  signupForm: FormGroup;
  brandLogo: string;
  showPassword: boolean;
  submitted: boolean;
  loading: boolean;
  errorMessage: string;
  showNameError: boolean;
  nameErrorMessage: string;
  showSurnameError: boolean;
  surnameErrorMessage: string;
  showEmailError: boolean;
  emailErrorMessage: string;
  showPasswordError: boolean;
  passwordErrorMessage: string;
  showTermsError: boolean;
  termsErrorMessage: string;
  togglePasswordVisibility(): void;
  createAccount(): void;
}

describe('AuthService - register', () => {
  let service: AuthService;
  let httpMock: HttpTestingController;

  const validPayload: RegisterRequest = {
    firstName: 'John',
    lastName: 'Doe',
    email: 'john@momentum.co.za',
    password: 'Password1!!!!',
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        //router isn't actually used by register(), but AuthService injects it
        //at the class level (for logout), so it still needs to be provided
        provideRouter([]),
        provideHttpClient(),
        provideHttpClientTesting(),
      ],
    });

    service = TestBed.inject(AuthService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    //make sure every test flushed its request, catches accidental double calls too
    httpMock.verify();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('should POST the signup payload to /auth/register', () => {
    service.register(validPayload).subscribe();

    /* matching on url.endsWith rather than a hardcoded full path so this
    doesn't break if environment.apiUrl changes between env */
    const req = httpMock.expectOne((r) => r.url.endsWith('/auth/register'));

    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(validPayload);

    req.flush({
      id: '00000000-0000-0000-0003-000000000001',
      email: validPayload.email,
      firstName: validPayload.firstName,
      lastName: validPayload.lastName,
      createdAt: '2026-07-21T09:00:00.000Z',
      message:
        'Registered successfully, please check your email to verify your account.', //in actual sense this is emai;_verified is set to true in backend so we can move forward
      verificationToken: '12345',
    } satisfies RegisterResponse);
  });

  it('should return the created user on success', () => {
    let result: RegisterResponse | undefined;

    service.register(validPayload).subscribe((res) => (result = res));

    const req = httpMock.expectOne((r) => r.url.endsWith('/auth/register'));
    req.flush({
      id: '00000000-0000-0000-0003-000000000001',
      email: validPayload.email,
      firstName: validPayload.firstName,
      lastName: validPayload.lastName,
      createdAt: '2026-07-21T09:00:00.000Z',
      message:
        'Registered successfully, please check your email to verify your account.', //in actual sense this is emai;_verified is set to true in backend so we can move forward
      verificationToken: '12345',
    } satisfies RegisterResponse);

    expect(result?.email).toBe(validPayload.email);
    expect(result?.message).toContain('Registered successfully');
  });

  it('should not store an auth token after registering', () => {
    //register doesn't return a token (user has to verify + log in separately),
    //so unlike login(), there should be nothing written to localStorage here
    service.register(validPayload).subscribe();

    const req = httpMock.expectOne((r) => r.url.endsWith('/auth/register'));
    req.flush({
      id: '00000000-0000-0000-0003-000000000001',
      email: validPayload.email,
      firstName: validPayload.firstName,
      lastName: validPayload.lastName,
      createdAt: '2026-07-21T09:00:00.000Z',
      message:
        'Registered successfully, please check your email to verify your account.', //in actual sense this is emai;_verified is set to true in backend so we can move forward
      verificationToken: '12345',
    } satisfies RegisterResponse);

    expect(localStorage.getItem('auth_token')).toBeNull();
  });

  it('should surface the backend message when email is already taken', () => {
    let error: Error | undefined;

    service.register(validPayload).subscribe({
      error: (err) => (error = err),
    });

    const req = httpMock.expectOne((r) => r.url.endsWith('/auth/register'));
    req.flush(
      { message: 'An account with this email already exists.' },
      { status: 409, statusText: 'Conflict' },
    );

    expect(error?.message).toBe('An account with this email already exists.');
  });

  it('should fall back to a generic message when the backend sends no message', () => {
    //handleError() has a fallback string for exactly this case, worth locking down
    let error: Error | undefined;

    service.register(validPayload).subscribe({
      error: (err) => (error = err),
    });

    const req = httpMock.expectOne((r) => r.url.endsWith('/auth/register'));
    req.flush(null, { status: 500, statusText: 'Internal Server Error' });

    expect(error?.message).toBe('Something went wrong, try again in a bit.');
  });
});

/*
Covers SignupComponent itself: form validation display (name/surname/email/password/terms),
password visibility toggle, and the part the old spec never actually checked that
createAccount() calls AuthService.register() with the right payload and reacts correctly
to both the success and error paths (loading state, navigation, errorMessage)

Unlike the AuthService describe above, this one needs a real fixture since we're
rendering the component and asserting on both its class state and its template output
*/
describe('SignupComponent', () => {
  let component: SignupComponent;
  let fixture: ComponentFixture<SignupComponent>;
  let componentInstance: SignupComponentInternals;
  let httpMock: HttpTestingController;
  let router: Router;

  const validFormValue = {
    name: 'John',
    surname: 'Doe',
    email: 'john@company.com',
    password: 'Password1!!!',
    acceptedTerms: true,
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [SignupComponent],
      providers: [
        provideRouter([]),
        provideHttpClient(),
        provideHttpClientTesting(),
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(SignupComponent);
    component = fixture.componentInstance;
    componentInstance = component as unknown as SignupComponentInternals;
    httpMock = TestBed.inject(HttpTestingController);
    router = TestBed.inject(Router);

    fixture.detectChanges();
  });

  afterEach(() => {
    httpMock.verify();
  });

  //Test that the component is created successfully
  it('should create', () => {
    expect(component).toBeTruthy();
  });

  //Test that the signup form starts with default invalid values
  it('should initialise the signup form as invalid', () => {
    expect(componentInstance.signupForm.invalid).toBe(true);
    expect(componentInstance.signupForm.controls['name'].value).toBe('');
    expect(componentInstance.signupForm.controls['surname'].value).toBe('');
    expect(componentInstance.signupForm.controls['email'].value).toBe('');
    expect(componentInstance.signupForm.controls['password'].value).toBe('');
    expect(componentInstance.signupForm.controls['acceptedTerms'].value).toBe(
      false,
    );
  });

  //Test that the correct logo asset path is used
  it('should use the Momently logo asset', () => {
    expect(componentInstance.brandLogo).toBe('/assets/momently.png');
  });

  // Test password visibility toggle functionality
  it('should toggle password visibility', () => {
    expect(componentInstance.showPassword).toBe(false);

    componentInstance.togglePasswordVisibility();
    expect(componentInstance.showPassword).toBe(true);

    componentInstance.togglePasswordVisibility();
    expect(componentInstance.showPassword).toBe(false);
  });

  //Test validation errors when submitting an empty form
  it('should show required errors after an invalid submit', () => {
    componentInstance.createAccount();
    fixture.detectChanges();

    //createAccount() returns early on an invalid form, so there should be
    //no outgoing request to assert on here, just the validation state
    expect(componentInstance.showNameError).toBe(true);
    expect(componentInstance.nameErrorMessage).toBe('Name is required.');

    expect(componentInstance.showSurnameError).toBe(true);
    expect(componentInstance.surnameErrorMessage).toBe('Surname is required.');

    expect(componentInstance.showEmailError).toBe(true);
    expect(componentInstance.emailErrorMessage).toBe('Work email is required.');

    expect(componentInstance.showPasswordError).toBe(true);
    expect(componentInstance.passwordErrorMessage).toBe(
      'Password is required.',
    );

    expect(componentInstance.showTermsError).toBe(true);
    expect(componentInstance.termsErrorMessage).toBe(
      'You must accept the Terms of Service and Privacy Policy.',
    );
  });

  //Test name validation rules
  it('should validate name minimum length', () => {
    componentInstance.signupForm.controls['name'].setValue('J');
    componentInstance.signupForm.controls['name'].markAsTouched();

    expect(componentInstance.showNameError).toBe(true);
    expect(componentInstance.nameErrorMessage).toBe(
      'Name must be at least 2 characters.',
    );
  });

  //Test that valid names do not show errors
  it('should return an empty name error message when name is valid', () => {
    componentInstance.signupForm.controls['name'].setValue('John');
    componentInstance.signupForm.controls['name'].markAsTouched();

    expect(componentInstance.showNameError).toBe(false);
    expect(componentInstance.nameErrorMessage).toBe('');
  });

  //Test surname validation rules
  it('should validate surname minimum length', () => {
    componentInstance.signupForm.controls['surname'].setValue('D');
    componentInstance.signupForm.controls['surname'].markAsTouched();

    expect(componentInstance.showSurnameError).toBe(true);
    expect(componentInstance.surnameErrorMessage).toBe(
      'Surname must be at least 2 characters.',
    );
  });

  //Test that valid surnames do not show errors
  it('should return an empty surname error message when surname is valid', () => {
    componentInstance.signupForm.controls['surname'].setValue('Doe');
    componentInstance.signupForm.controls['surname'].markAsTouched();

    expect(componentInstance.showSurnameError).toBe(false);
    expect(componentInstance.surnameErrorMessage).toBe('');
  });

  //Test that terms error disappears once terms are accepted
  it('should return an empty terms error message when terms are accepted', () => {
    componentInstance.signupForm.controls['acceptedTerms'].setValue(true);
    componentInstance.signupForm.controls['acceptedTerms'].markAsTouched();

    expect(componentInstance.showTermsError).toBe(false);
    expect(componentInstance.termsErrorMessage).toBe('');
  });

  //Test email validation for incorrect email formats
  it('should validate email format', () => {
    componentInstance.signupForm.controls['email'].setValue('invalid-email');
    componentInstance.signupForm.controls['email'].markAsTouched();

    expect(componentInstance.showEmailError).toBe(true);
    expect(componentInstance.emailErrorMessage).toBe(
      'Enter a valid email address.',
    );
  });

  //Test that valid emails do not show errors
  it('should return an empty email error message when email is valid', () => {
    componentInstance.signupForm.controls['email'].setValue('john@company.com');
    componentInstance.signupForm.controls['email'].markAsTouched();

    expect(componentInstance.showEmailError).toBe(false);
    expect(componentInstance.emailErrorMessage).toBe('');
  });

  //Test password validation rules
  it('should validate password length and content requirements', () => {
    componentInstance.signupForm.controls['password'].setValue('password');
    componentInstance.signupForm.controls['password'].markAsTouched();

    expect(componentInstance.showPasswordError).toBe(true);
    expect(componentInstance.passwordErrorMessage).toBe(
      'Password must be at least 8 characters long with a mix of letters and numbers.',
    );
  });

  //Test that valid passwords do not show errors
  it('should return an empty password error message when password is valid', () => {
    componentInstance.signupForm.controls['password'].setValue('Password1!!!');
    componentInstance.signupForm.controls['password'].markAsTouched();

    expect(componentInstance.showPasswordError).toBe(false);
    expect(componentInstance.passwordErrorMessage).toBe('');
  });

  /* Test that a valid form actually sends the right payload to the backend
     the old version of this test only checked `submitted`, which gets set
     to true on line 1 of createAccount() regardless of form validity, so it
     never proved the valid-form path did anything. */
  it('should call register with the mapped payload when the form is valid', () => {
    componentInstance.signupForm.setValue(validFormValue);
    expect(componentInstance.signupForm.valid).toBe(true);

    componentInstance.createAccount();

    expect(componentInstance.submitted).toBe(true);
    expect(componentInstance.loading).toBe(true);

    const req = httpMock.expectOne((r) => r.url.endsWith('/auth/register'));
    expect(req.request.method).toBe('POST');
    // firstName/lastName here, not name/surname createAccount() renames them before sending
    expect(req.request.body).toEqual({
      firstName: 'John',
      lastName: 'Doe',
      email: 'john@company.com',
      password: 'Password1!!!',
    });

    req.flush({
      id: '00000000-0000-0000-0003-000000000002',
      email: 'john@company.com',
      firstName: 'John',
      lastName: 'Doe',
      createdAt: '2026-07-21T09:00:00.000Z',
      message:
        'Registered successfully, please check your email to verify your account.',
      verificationToken: '12345',
    } satisfies RegisterResponse);
  });

  // Test that a successful signup stops loading and redirects to /login?registered=true
  it('should navigate to verify email', () => {
    const navigateSpy = jest.spyOn(router, 'navigate').mockResolvedValue(true);

    componentInstance.signupForm.setValue(validFormValue);
    componentInstance.createAccount();

    const req = httpMock.expectOne((r) => r.url.endsWith('/auth/register'));
    req.flush({
      id: '00000000-0000-0000-0003-000000000002',
      email: 'john@company.com',
      firstName: 'John',
      lastName: 'Doe',
      createdAt: '2026-07-21T09:00:00.000Z',
      message:'Registered successfully, please check your email to verify your account.',
      verificationToken: '12345',
    } satisfies RegisterResponse);

    expect(componentInstance.loading).toBe(false);
    expect(navigateSpy).toHaveBeenCalledWith(
      ['/verify-email'],
      {
        state: {
            token: '12345',
            email: 'john@company.com',
            firstName: 'John'
        }
      }
    );
  });

  // Test that a failed signup surfaces the backend's error message instead of navigating
  it('should show an error message and stop loading when signup fails', () => {
    const navigateSpy = jest.spyOn(router, 'navigate').mockResolvedValue(true);

    componentInstance.signupForm.setValue(validFormValue);
    componentInstance.createAccount();

    const req = httpMock.expectOne((r) => r.url.endsWith('/auth/register'));
    req.flush(
      { message: 'An account with this email already exists.' },
      { status: 409, statusText: 'Conflict' },
    );

    expect(componentInstance.loading).toBe(false);
    expect(componentInstance.errorMessage).toBe(
      'An account with this email already exists.',
    );
    expect(navigateSpy).not.toHaveBeenCalled();
  });

  // Test that important UI content renders correctly
  it('should render key signup page content', () => {
    const compiled = fixture.nativeElement as HTMLElement;

    expect(compiled.querySelector('.hero-title')?.textContent).toContain(
      'Start Your',
    );
    expect(compiled.querySelector('.hero-title')?.textContent).toContain(
      'AI Productivity',
    );
    expect(compiled.querySelector('#signup-title')?.textContent).toContain(
      'Create your account',
    );
    expect(
      compiled.querySelector('label[for="signup-name"]')?.textContent,
    ).toContain('Name');
    expect(
      compiled.querySelector('label[for="signup-surname"]')?.textContent,
    ).toContain('Surname');
    expect(compiled.querySelector('.submit-button')?.textContent).toContain(
      'Create Account',
    );
    expect(compiled.querySelectorAll('.social-button').length).toBe(2);
  });
});
