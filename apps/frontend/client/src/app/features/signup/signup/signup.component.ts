import { Component, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';

@Component({
  selector: 'app-signup',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './signup.component.html',
  styleUrl: './signup.component.scss'
})
export class SignupComponent {

  // Form builder for reactive form setup
  private readonly formBuilder = inject(FormBuilder);

  // Router for navigation after signup
  private readonly router = inject(Router);

  // Brand logo asset
  protected readonly brandLogo = '/assets/momently.png';

  // UI state
  protected showPassword = false;
  protected submitted = false;

  // Reactive form definition
  protected readonly signupForm = this.formBuilder.nonNullable.group({
    name: ['', [Validators.required, Validators.minLength(2)]],
    surname: ['', [Validators.required, Validators.minLength(2)]],
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

  // Toggle password visibility
  protected togglePasswordVisibility(): void {
    this.showPassword = !this.showPassword;
  }

  // Handle form submission
  protected createAccount(): void {
    this.submitted = true;

    // Stop if form is invalid
    if (this.signupForm.invalid) {
      this.signupForm.markAllAsTouched();
      return;
    }

    // Simulated submission (replace with API call later)
    console.info('Sign up submitted', this.signupForm.value);

    // Navigate to timesheets page after successful signup
    this.router.navigate(['/timesheets']);
  }

  /* ============================================================
     VALIDATION HELPERS
     ============================================================ */

  protected get showNameError(): boolean {
    const control = this.signupForm.controls.name;
    return (control.touched || this.submitted) && control.invalid;
  }

  protected get nameErrorMessage(): string {
    const control = this.signupForm.controls.name;

    if (control.hasError('required')) {
      return 'Name is required.';
    }
    if (control.hasError('minlength')) {
      return 'Name must be at least 2 characters.';
    }

    return '';
  }

  protected get showSurnameError(): boolean {
    const control = this.signupForm.controls.surname;
    return (control.touched || this.submitted) && control.invalid;
  }

  protected get surnameErrorMessage(): string {
    const control = this.signupForm.controls.surname;

    if (control.hasError('required')) {
      return 'Surname is required.';
    }
    if (control.hasError('minlength')) {
      return 'Surname must be at least 2 characters.';
    }

    return '';
  }

  protected get showEmailError(): boolean {
    const control = this.signupForm.controls.email;
    return (control.touched || this.submitted) && control.invalid;
  }

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

  protected get showPasswordError(): boolean {
    const control = this.signupForm.controls.password;
    return (control.touched || this.submitted) && control.invalid;
  }

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

  protected get showTermsError(): boolean {
    const control = this.signupForm.controls.acceptedTerms;
    return (control.touched || this.submitted) && control.invalid;
  }

  protected get termsErrorMessage(): string {
    return this.showTermsError
      ? 'You must accept the Terms of Service and Privacy Policy.'
      : '';
  }
}