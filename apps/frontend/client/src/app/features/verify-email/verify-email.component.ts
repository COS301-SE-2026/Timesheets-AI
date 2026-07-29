import { CommonModule } from '@angular/common';
import {Router} from '@angular/router'
import { Component, inject } from '@angular/core';
import { AuthService } from '../../core/services/auth.service';
import { MatIconModule } from '@angular/material/icon';

@Component({
  selector: 'app-verify-email',
  imports: [CommonModule, MatIconModule],
  templateUrl: './verify-email.component.html',
  styleUrl: './verify-email.component.scss'
})

export class VerifyEmailComponent {

  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);

  // made it protected to be consistent with other pages 
  // to prevent it from being able in component's public API - only accessed by component and its template

  protected token = '';
  protected email = '';
  protected firstName = '';
  protected loading = false;
  protected success = false;
  protected errorMessage = '';
  protected successMessage = '';
  protected copied = false;

  constructor() {
    console.log('VerifyEmailComponent loaded');
    const state = history.state ?? {};

    this.token = state.token ?? '';
    this.email = state.email ?? '';
    this.firstName =  state.firstName ?? '';
  }

  protected copyToken(): void {
    navigator.clipboard.writeText(this.token);

    this.copied = true;

    setTimeout(() => {
      this.copied = false;
    }, 200)
  }

  protected verifyEmail(): void {
    
    this.loading = true;
    this.errorMessage = '';

    this.authService.verifyEmail(this.token).subscribe({
      next: (response) => {
          this.loading = false;
          this.successMessage = response.message;

           setTimeout(() => {
            this.router.navigate(['/login']);
          }, 1500);

      },

      error: (err) => {
        this.loading = false;
        this.errorMessage = err.message;
      }
    });
  }
}
