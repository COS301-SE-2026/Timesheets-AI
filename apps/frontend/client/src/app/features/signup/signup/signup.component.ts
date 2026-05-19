import { Component, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';

@Component({
  selector: 'app-signup',
  imports: [ReactiveFormsModule],
  templateUrl: './signup.component.html',
  styleUrl: './signup.component.scss'
})
export class SignupComponent {

  // Form builder used to create reactive form structure
  private readonly formBuilder = inject(FormBuilder);

  // Logo image
  protected readonly brandLogo = '/assets/momently.png';

  // Toggles password visibility
  protected showPassword = false;

  // Tracks whether form has been submitted (used for validation display)
  protected submitted = false;

  // Reactive signup form definition
  protected readonly signupForm = this.formBuilder.nonNullable.group({
    email: ['', [Validators.required, Validators.email]],
    password: [
      '',
      [
        Validators.required,
        Validators.minLength(8),
        Validators.pattern(/^(?=.*[A-Za-z])(?=.*\d).+$/)
      ]
    ],
    acceptedTerms: [false, Validators.requiredTrue]
  });

  // Toggles password visibility in the input field
  protected togglePasswordVisibility(): void {
    this.showPassword = !this.showPassword;
  }

  // Handles form submission and validation flow
  protected createAccount(): void {
    this.submitted = true;

    // Stop submission if form is invalid
    if (this.signupForm.invalid) {
      this.signupForm.markAllAsTouched();
      return;
    }

    // INTEGRATE API HERE 
    console.info('Sign up submitted', {
      email: this.signupForm.controls.email.value
    });
  }

  // Determines when email error should be shown
  protected get showEmailError(): boolean {
    const control = this.signupForm.controls.email;
    return (control.touched || this.submitted) && control.invalid;
  }

  // Returns appropriate email validation error message
  protected get emailErrorMessage(): string {
    const control = this.signupForm.controls.email;

    if (control.hasError('required')) {
      return 'Work email is required.';
    }
    if (control.hasError('email')) {
      return 'Enter a valid email address.';
    }

    return '';
  }

  // Determines when password error should be shown
  protected get showPasswordError(): boolean {
    const control = this.signupForm.controls.password;
    return (control.touched || this.submitted) && control.invalid;
  }

  // Returns appropriate password validation error message
  protected get passwordErrorMessage(): string {
    const control = this.signupForm.controls.password;

    if (control.hasError('required')) {
      return 'Password is required.';
    }
    if (control.hasError('minlength') || control.hasError('pattern')) {
      return 'Password must be at least 8 characters long with a mix of letters and numbers.';
    }

    return '';
  }

  // Determines when terms validation error should be shown
  protected get showTermsError(): boolean {
    const control = this.signupForm.controls.acceptedTerms;
    return (control.touched || this.submitted) && control.invalid;
  }

  // Returns terms validation error message
  protected get termsErrorMessage(): string {
    return this.showTermsError
      ? 'You must accept the Terms of Service and Privacy Policy.'
      : '';
  }
}