import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Router } from '@angular/router';
import { Observable, tap, catchError, throwError } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface RegisterRequest {
  firstName: string;
  lastName: string;
  email: string;
  password: string;
}

export interface RegisterResponse {
  id: string;
  email: string;
  firstName: string;
  lastName: string;
  createdAt: string;
  message: string;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface AuthResponse {
  token: string;
  expiresAt: string;
  requiresMfa: boolean;
  user: {
    id: string;
    email: string;
    firstName: string;
    lastName: string;
    avatarUrl: string | null;
    emailVerified: boolean;
    roles: string[];
    mfaEnabled: boolean;
  };
}

const TOKEN_KEY = 'auth_token';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);
  private readonly baseUrl = `${environment.apiUrl}/api/auth`;

  register(payload: RegisterRequest): Observable<RegisterResponse> {
    return this.http
      .post<RegisterResponse>(`${this.baseUrl}/register`, payload)
      .pipe(catchError(this.handleError));
  }

  login(payload: LoginRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.baseUrl}/login`, payload).pipe(
      tap((res) => localStorage.setItem(TOKEN_KEY, res.token)),
      catchError(this.handleError),
    );
  }
  googleAuth(idToken: string): Observable <AuthResponse> {
    return this.http.post<AuthResponse> (`${this.baseUrl}/google`, {idToken}).pipe(
        tap(res => localStorage.setItem(TOKEN_KEY, res.token)),
        catchError(this.handleError)
    );
  }

  logout(): void {
    const token = this.getToken();
    if (token) {
      this.http
        .post(`${this.baseUrl}/logout`, null, {
          headers: { Authorization: `Bearer ${token}` },
        })
        .subscribe();
    }
    localStorage.removeItem(TOKEN_KEY);
    this.router.navigate(['/login']);
  }

  getToken(): string | null {
    return localStorage.getItem(TOKEN_KEY);
  }

  isLoggedIn(): boolean {
    return !!this.getToken();
  }

  // backend sends errors back as MessageResponse { message, redirectUrl }
  // so we pull the message out here and pass it up as a clean string
  private handleError(error: HttpErrorResponse) {
    const message =
      error.error?.message || 'Something went wrong, try again in a bit.';
    return throwError(() => new Error(message));
  }
}
