import { Injectable, inject, signal } from '@angular/core';
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
export interface AuthUser {
  id: string;
  email: string;
  firstName: string;
  lastName: string;
  avatarUrl: string | null;
  emailVerified: boolean;
  roles: string[];
  mfaEnabled: boolean;
}
export interface AuthResponse {
  token: string;
  expiresAt: string;
  requiresMfa: boolean;
  user: AuthUser;
}

const TOKEN_KEY = 'auth_token';
/*
Patched: Zamokuhle Zwane, 25 July 2026
login and googleAuth were only ever storing the token
*/
const USER_KEY = 'auth_user';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);
  // the environment already provides '/api' prefix 
  // keep only "/auth" here to avoid it generating "api/api/* urls" 
  // this approach works correctly with Nginx reverse proxy 
  private readonly baseUrl = `${environment.apiUrl}/auth`;

  //signal so that any component reacts automatically to the moment login/logout happens.
  //refs: https://angular.dev/guide/signals

  readonly currentUser = signal<AuthUser | null>(this.loadStoredUser());
  constructor() {
    /*
    if there's a token but no stored user, that's someone who logged in
    before persistSession() existed, there's no /api/users/me endpoint to
    backfill the name from, and the JWT itself only carries email/userId,
    no name. rather than leave them stuck on "Guest" indefinitely, clear
    the stale token so the route guard kicks them to /login for one clean
    re-login, after which persistSession() takes over normally
    */
    if (this.getToken() && !this.currentUser()) {
      console.warn(
        '[AuthService] token found with no stored user, forcing re-login',
      );
      localStorage.removeItem(TOKEN_KEY);
    }
  }

  register(payload: RegisterRequest): Observable<RegisterResponse> {
    return this.http
      .post<RegisterResponse>(`${this.baseUrl}/register`, payload)
      .pipe(catchError(this.handleError));
  }

  login(payload: LoginRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.baseUrl}/login`, payload).pipe(
      tap((res) => this.persistSession(res)),
      catchError(this.handleError),
    );
  }

  googleAuth(idToken: string): Observable<AuthResponse> {
    return this.http
      .post<AuthResponse>(`${this.baseUrl}/google`, { idToken })
      .pipe(
        tap((res) => this.persistSession(res)),
        catchError(this.handleError),
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
    localStorage.removeItem(USER_KEY);
    this.currentUser.set(null);
    this.router.navigate(['/login']);
  }

  getToken(): string | null {
    return localStorage.getItem(TOKEN_KEY);
  }

  isLoggedIn(): boolean {
    return !!this.getToken();
  }
  //this stores the token and user object on login/googleauth and updates the signal
  private persistSession(res: AuthResponse): void {
    localStorage.setItem(TOKEN_KEY, res.token);
    localStorage.setItem(USER_KEY, JSON.stringify(res.user));
    this.currentUser.set(res.user);
  }

  //this will read whstever was last stord, used to seed the signal on app start up and page refress doesnt lose name until next login
  private loadStoredUser(): AuthUser | null {
    const raw = localStorage.getItem(USER_KEY);
    if (!raw) return null;
    try {
      return JSON.parse(raw) as AuthUser;
    } catch {
      //correct or stale values from older app version
      console.error('[AuthService] failed to parse stored user, clearing it');
      localStorage.removeItem(USER_KEY);
      return null;
    }
  }

  // backend sends errors back as MessageResponse { message, redirectUrl }
  // so we pull the message out here and pass it up as a clean string
  private handleError(error: HttpErrorResponse) {
    const message =
      error.error?.message || 'Something went wrong, try again in a bit.';
    return throwError(() => new Error(message));
  }

  refreshUser(): Observable<AuthResponse> {

    const token = this.getToken();
    
    if (!token) {
      return throwError(() => new Error('No token available'));
    }
    
    return this.http.get<AuthResponse>(`${this.baseUrl}/auth/me`, {
      headers: { Authorization: `Bearer ${token}` }
    }).pipe(tap((res) => this.persistSession(res)), catchError(this.handleError));
  }
}
