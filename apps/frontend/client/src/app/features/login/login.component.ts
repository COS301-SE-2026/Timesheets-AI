import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators} from '@angular/forms';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterModule],
  templateUrl: './login.component.html',
  styleUrl: './login.component.scss'
})
export class LoginComponent {

  // Form builder (same style as signup OR you can inject if you want full consistency)
  private readonly formBuilder = inject(FormBuilder);

  // Logo (fixes your NG error + allows reuse in template)
  protected readonly brandLogo = '/assets/momently.png';

  // UI state
  protected loading = false;
  protected errorMessage = '';
  protected showPassword = false;
  protected submitted = false;

  /* Toast state */
  protected toastMessage = '';
  protected showToast = false;

  // Login form
  protected readonly loginForm: FormGroup = this.formBuilder.group({
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required]],
    remember: [false]
  });

  // Toggle password visibility
  protected togglePassword(): void {
    this.showPassword = !this.showPassword;
  }

  /* Show a temporary demo toast message */
  protected showDemoToast(message: string): void {
    this.toastMessage = message;
    this.showToast = true;
    setTimeout(() => { this.showToast = false; }, 4000);
  }

  /* Forgot password handler */
  protected onForgotPassword(event: Event): void {
    event.preventDefault();
    this.showDemoToast(
      'Password reset is not available in Demo 1. Use the test credentials shared with your team.'
    );
  }

  /* Social login handler */
  protected onSocialLogin(provider: string): void {
    this.showDemoToast(
      `${provider} login is not available in Demo 1. Please use the email and password form.`
    );
  }
  

  // Submit handler
  protected onSubmit(): void {
    this.submitted = true;

    if (this.loginForm.invalid) {
      this.loginForm.markAllAsTouched();
      return;
    }

    this.loading = true;
    this.errorMessage = '';

    setTimeout(() => {
      this.loading = false;
      this.errorMessage = 'Demo only — no backend connected yet.';
    }, 1000);
  }

  // Email error logic
  protected get showEmailError(): boolean {
    const control = this.loginForm.controls['email'];
    return (control.touched || this.submitted) && control.invalid;
  }

  protected get emailErrorMessage(): string {
    const control = this.loginForm.controls['email'];

    if (control.hasError('required')) return 'Email is required.';
    if (control.hasError('email')) return 'Enter a valid email address.';

    return '';
  }

  // Password error logic
  protected get showPasswordError(): boolean {
    const control = this.loginForm.controls['password'];
    return (control.touched || this.submitted) && control.invalid;
  }

  
  protected get passwordErrorMessage(): string {
    const control = this.loginForm.controls['password'];

    if (control.hasError('required')) return 'Password is required.';

    return '';
  }
}