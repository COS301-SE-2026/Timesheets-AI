/**
 * Author: Lerato Sibanda
 * Date: 2026-05-17
 * Purpose: Handles new user registration with form validation.
 * Related Requirement: -
 */

import { Component, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink, Router} from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';
@Component({
  selector: 'app-signup',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './signup.component.html',
  styleUrl: './signup.component.scss'
})
export class SignupComponent {

  // Form builder used to create reactive form structure
  private readonly formBuilder = inject(FormBuilder);

  private readonly router = inject(Router);

  private readonly authService = inject(AuthService);
  /* Toast state */
  protected toastMessage = '';
  protected showToast = false;

  // Logo image
  protected readonly brandLogo = '/assets/momently.png';

  // Toggles password visibility
  protected showPassword = false;

  // Tracks whether form has been submitted (used for validation display)
  protected submitted = false;
  
  //show loading
  protected loading = false;

  //Sends an error message
  protected errorMessage = '';
  

  // Reactive signup form definition
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

  // Toggles password visibility in the input field
  protected togglePasswordVisibility(): void {
    this.showPassword = !this.showPassword;
  }

  // Handles form submission and validation flow
  protected createAccount(): void {
    this.submitted = true;

    if (this.signupForm.invalid) {
      this.signupForm.markAllAsTouched();
      return;
    }

    this.loading = true;
    this.errorMessage = '';

    const {name, surname, email, password } = this.signupForm.getRawValue();
    this.authService
    .register({firstName:name, lastName: surname, email, password})
    .subscribe({
      next: () => {
        this.loading = false;
        //no token comes back here, they still need to verify email then log in

        this.router.navigate(['/login'], {
          queryParams: {registered: 'true'}
        });
      },
      error: err => {
        this.loading = false;
        this.errorMessage = err.message;
      }
    });
  }

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

  // Determines when email error should be shown
  protected get showEmailError(): boolean {
    const control = this.signupForm.controls.email;
    return (control.touched || this.submitted) && control.invalid;
  }

  // Returns appropriate email validation error message
  //TODO: add a template with a momentum.co.za maybe? 
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
    if (control.hasError('pattern')){
      return 'Needs an uppercase letter, lowercase letter, number, and special character.'
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

    /* Show a temporary demo toast */
  protected showDemoToast(message: string): void {
    this.toastMessage = message;
    this.showToast = true;
    setTimeout(() => { this.showToast = false; }, 4000);
  }

  /* Social login handler */
  protected onSocialLogin(provider: string): void {
    this.showDemoToast(
      `${provider} sign up is not available in Demo 1. Please use the email form.`
    );
  }
}