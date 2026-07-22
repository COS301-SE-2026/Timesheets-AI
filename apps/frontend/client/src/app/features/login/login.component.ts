/**
 * Author: Cleopatra Kwenda
 * Date: 2026-05-15
 * Purpose: Handles user authentication through login form with validation
 * Related Requirement: -
 */

import {
  AfterViewInit,
  Component,
  ElementRef,
  inject,
  ViewChild,
} from '@angular/core';

import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import {
  FormBuilder,
  FormGroup,
  ReactiveFormsModule,
  Validators,
} from '@angular/forms';
import { AuthService } from '../../core/services/auth.service';
import { environment } from '../../../environments/environment';

declare const google: any;

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterModule],
  templateUrl: './login.component.html',
  styleUrl: './login.component.scss',
})
export class LoginComponent implements AfterViewInit {
  // Form builder (same style as signup OR you can inject if you want full consistency)
  private readonly formBuilder = inject(FormBuilder);

  // Logo (fixes your NG error + allows reuse in template)
   readonly brandLogo = '/assets/momently.png';

  private readonly route = inject(ActivatedRoute);
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);

  // UI state
   loading = false;
   errorMessage = '';
   showPassword = false;
   submitted = false;

  /* Toast state */
   toastMessage = '';
   showToast = false;

  // Login form
   readonly loginForm: FormGroup = this.formBuilder.group({
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required]],
    remember: [false],
  });

  // This shows a quick toast if they came from a succesful signup
  constructor() {
    if (this.route.snapshot.queryParams['registered'] === 'true') {
      this.showDemoToast('Account created! Please login!');
    }
  }

  // Toggle password visibility
   togglePassword(): void {
    this.showPassword = !this.showPassword;
  }

  /* Show a temporary demo toast message */
   showDemoToast(message: string): void {
    this.toastMessage = message;
    this.showToast = true;
    setTimeout(() => {
      this.showToast = false;
    }, 4000);
  }

  /* Forgot password handler */
   onForgotPassword(event: Event): void {
    event.preventDefault();
    this.showDemoToast('Password reset is not available yet.');
  }

  @ViewChild('googleBtn') googleBtn!: ElementRef;

  ngAfterViewInit(): void {
    google.accounts.id.initialize({
      client_id: environment.googleClientId,
      callback: (response: { credential: string }) =>
        this.handleGoogleCredential(response.credential),
    });

    google.accounts.id.renderButton(this.googleBtn.nativeElement, {
      theme: 'outline',
      size: 'large',
      width: 320,
    });
  }
   triggerGoogleLogin(): void {
    const hiddenGoogleButton =
      this.googleBtn.nativeElement.querySelector('div[role="button"]');
    hiddenGoogleButton?.click();
  }

  //TODO: Fix the MFA thing, after it shows up in the UI
  private handleGoogleCredential(idToken: string): void {
    this.loading = true;
    this.errorMessage = '';

    this.authService.googleAuth(idToken).subscribe({
      next: (res) => {
        this.loading = false;
        if (res.requiresMfa) {
          this.showDemoToast('MFA is not supported in the UI yet');
          return;
        }
        this.router.navigate(['/log-time']);
      },
      error: (err) => {
        this.loading = false;
        this.errorMessage = err.message;
      },
    });
  }
   onSocialLogin(provider: string): void {
    this.showDemoToast(
      `${provider} login is not available yet, use email and password.`,
    );
  }

  // Submit handler
   onSubmit(): void {
    this.submitted = true;

    if (this.loginForm.invalid) {
      this.loginForm.markAllAsTouched();
      return;
    }

    this.loading = true;
    this.errorMessage = '';

    const { email, password } = this.loginForm.value;

    this.authService.login({ email, password }).subscribe({
      next: (res) => {
        this.loading = false;
        if (res.requiresMfa) {
          this.showDemoToast('MFA is not supported in the UI yet.');
          return;
        }
        this.router.navigate(['/log-time']);
      },

      error: (err) => {
        this.loading = false;
        this.errorMessage = err.message;
      },
    });
  }

  // Email error logic
   get showEmailError(): boolean {
    const control = this.loginForm.controls['email'];
    return (control.touched || this.submitted) && control.invalid;
  }

   get emailErrorMessage(): string {
    const control = this.loginForm.controls['email'];

    if (control.hasError('required')) return 'Email is required.';
    if (control.hasError('email')) return 'Enter a valid email address.';

    return '';
  }

  // Password error logic
   get showPasswordError(): boolean {
    const control = this.loginForm.controls['password'];
    return (control.touched || this.submitted) && control.invalid;
  }

  
   get passwordErrorMessage(): string {
    const control = this.loginForm.controls['password'];

    if (control.hasError('required')) return 'Password is required.';

    return '';
  }
}
