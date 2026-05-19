import { ComponentFixture, TestBed } from '@angular/core/testing';
import { SignupComponent } from './signup.component';

describe('SignupComponent', () => {

  let component: SignupComponent;
  let fixture: ComponentFixture<SignupComponent>;
  let componentInstance: any;

  // Runs before each test case
  beforeEach(async () => {

    // Configure the testing module with the standalone SignupComponent
    await TestBed.configureTestingModule({
      imports: [SignupComponent]
    })
      .compileComponents();

    // Create the component instance
    fixture = TestBed.createComponent(SignupComponent);

    component = fixture.componentInstance;

    componentInstance = component as any;

    fixture.detectChanges();
  });

  // Test that the component is created successfully
  it('should create', () => {
    expect(component).toBeTruthy();
  });

  // Test that the signup form starts with default invalid values
  it('should initialise the signup form as invalid', () => {
    expect(componentInstance.signupForm.invalid).toBe(true);
    expect(componentInstance.signupForm.controls.name.value).toBe('');
    expect(componentInstance.signupForm.controls.surname.value).toBe('');
    expect(componentInstance.signupForm.controls.email.value).toBe('');
    expect(componentInstance.signupForm.controls.password.value).toBe('');
    expect(componentInstance.signupForm.controls.acceptedTerms.value).toBe(false);
  });

  // Test that the correct logo asset path is used
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

  // Test validation errors when submitting an empty form
  it('should show required errors after an invalid submit', () => {

    componentInstance.createAccount();

    fixture.detectChanges();

    // Verify name validation errors
    expect(componentInstance.showNameError).toBe(true);
    expect(componentInstance.nameErrorMessage).toBe('Name is required.');

    // Verify surname validation errors
    expect(componentInstance.showSurnameError).toBe(true);
    expect(componentInstance.surnameErrorMessage).toBe('Surname is required.');

    // Verify email validation errors
    expect(componentInstance.showEmailError).toBe(true);
    expect(componentInstance.emailErrorMessage).toBe('Work email is required.');

    // Verify password validation errors
    expect(componentInstance.showPasswordError).toBe(true);
    expect(componentInstance.passwordErrorMessage).toBe('Password is required.');

    // Verify terms checkbox validation errors
    expect(componentInstance.showTermsError).toBe(true);
    expect(componentInstance.termsErrorMessage).toBe('You must accept the Terms of Service and Privacy Policy.');
  });

  // Test name validation rules
  it('should validate name minimum length', () => {

    // Set a name that is too short
    componentInstance.signupForm.controls.name.setValue('J');
    componentInstance.signupForm.controls.name.markAsTouched();

    expect(componentInstance.showNameError).toBe(true);
    expect(componentInstance.nameErrorMessage).toBe('Name must be at least 2 characters.');
  });

  // Test that valid names do not show errors
  it('should return an empty name error message when name is valid', () => {

    // Set a valid name
    componentInstance.signupForm.controls.name.setValue('John');
    componentInstance.signupForm.controls.name.markAsTouched();

    expect(componentInstance.showNameError).toBe(false);
    expect(componentInstance.nameErrorMessage).toBe('');
  });

  // Test surname validation rules
  it('should validate surname minimum length', () => {

    // Set a surname that is too short
    componentInstance.signupForm.controls.surname.setValue('D');
    componentInstance.signupForm.controls.surname.markAsTouched();

    expect(componentInstance.showSurnameError).toBe(true);
    expect(componentInstance.surnameErrorMessage).toBe('Surname must be at least 2 characters.');
  });

  // Test that valid surnames do not show errors
  it('should return an empty surname error message when surname is valid', () => {

    // Set a valid surname
    componentInstance.signupForm.controls.surname.setValue('Doe');
    componentInstance.signupForm.controls.surname.markAsTouched();

    expect(componentInstance.showSurnameError).toBe(false);
    expect(componentInstance.surnameErrorMessage).toBe('');
  });

  // Test that terms error disappears once terms are accepted
  it('should return an empty terms error message when terms are accepted', () => {

    // Simulate checking the terms checkbox
    componentInstance.signupForm.controls.acceptedTerms.setValue(true);
    componentInstance.signupForm.controls.acceptedTerms.markAsTouched();

    expect(componentInstance.showTermsError).toBe(false);
    expect(componentInstance.termsErrorMessage).toBe('');
  });

  // Test email validation for incorrect email formats
  it('should validate email format', () => {

    // Set an invalid email value
    componentInstance.signupForm.controls.email.setValue('invalid-email');
    componentInstance.signupForm.controls.email.markAsTouched();

    expect(componentInstance.showEmailError).toBe(true);
    expect(componentInstance.emailErrorMessage).toBe('Enter a valid email address.');
  });

  // Test that valid emails do not show errors
  it('should return an empty email error message when email is valid', () => {

    // Set a valid email value
    componentInstance.signupForm.controls.email.setValue('john@company.com');
    componentInstance.signupForm.controls.email.markAsTouched();

    expect(componentInstance.showEmailError).toBe(false);
    expect(componentInstance.emailErrorMessage).toBe('');
  });

  // Test password validation rules
  it('should validate password length and content requirements', () => {

    // Set a password without numbers
    componentInstance.signupForm.controls.password.setValue('password');
    componentInstance.signupForm.controls.password.markAsTouched();

    expect(componentInstance.showPasswordError).toBe(true);
    expect(componentInstance.passwordErrorMessage).toBe(
      'Password must be at least 8 characters long with a mix of letters and numbers.'
    );
  });

  // Test that valid passwords do not show errors
  it('should return an empty password error message when password is valid', () => {

    // Set a valid password
    componentInstance.signupForm.controls.password.setValue('Password1');
    componentInstance.signupForm.controls.password.markAsTouched();

    expect(componentInstance.showPasswordError).toBe(false);
    expect(componentInstance.passwordErrorMessage).toBe('');
  });

  // Test successful form submission with valid data
  it('should submit when the form is valid', () => {

    const consoleInfoSpy = jest.spyOn(console, 'info').mockImplementation();

    // Fill the form with valid values
    componentInstance.signupForm.setValue({
      name: 'John',
      surname: 'Doe',
      email: 'john@company.com',
      password: 'Password1',
      acceptedTerms: true
    });

    componentInstance.createAccount();

    // Verify form validity
    expect(componentInstance.signupForm.valid).toBe(true);

    // Verify submission logging
    expect(consoleInfoSpy).toHaveBeenCalledWith('Sign up submitted', {
      name: 'John',
      surname: 'Doe',
      email: 'john@company.com'
    });

    consoleInfoSpy.mockRestore();
  });

  // Test that important UI content renders correctly
  it('should render key signup page content', () => {

    // Access the rendered HTML
    const compiled = fixture.nativeElement as HTMLElement;

    // Verify hero section text
    expect(compiled.querySelector('.hero-title')?.textContent).toContain('Start Your');
    expect(compiled.querySelector('.hero-title')?.textContent).toContain('AI Productivity');

    // Verify form heading text
    expect(compiled.querySelector('#signup-title')?.textContent).toContain('Create your account');

    // Verify name and surname fields render
    expect(compiled.querySelector('label[for="signup-name"]')?.textContent).toContain('Name');
    expect(compiled.querySelector('label[for="signup-surname"]')?.textContent).toContain('Surname');

    // Verify submit button text
    expect(compiled.querySelector('.submit-button')?.textContent).toContain('Create Account');

    // Verify both social login buttons render
    expect(compiled.querySelectorAll('.social-button').length).toBe(2);
  });
});