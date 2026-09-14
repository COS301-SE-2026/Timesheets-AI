import { Component, inject, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';

@Component({
  selector: 'app-verify-email',
  templateUrl: './verify-email.component.html',
  styleUrl: './verify-email.component.scss'
})
export class VerifyEmailComponent implements OnInit {

  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly authService = inject(AuthService);

  protected loading = true;
  protected success = false;
  protected message = '';

  protected goToLogin(): void {
    this.router.navigate(['/login']);
  }

  ngOnInit(): void {
    const token = this.route.snapshot.queryParamMap.get('token');

    if (!token) {
      this.loading = false;
      this.message = 'No verification token was provided.';
      return;
    }

    this.authService.verifyEmail(token).subscribe({
      next: (response) => {
        this.loading = false;
        this.success = true;
        this.message = response.message;

        setTimeout(() => {
          this.router.navigate(['/login'], {
            queryParams: { verified: 'true' }
          });
        }, 2000);
      },
      error: (err) => {
        this.loading = false;
        this.success = false;
        this.message = err.message;
      }
    });
  }
}
