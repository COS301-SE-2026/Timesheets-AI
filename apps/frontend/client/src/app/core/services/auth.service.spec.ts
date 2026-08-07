/*
This file is handling AuthService, im honestly just creating this file because i need to improve coverage
This one needed its own spec because none existed, coverage was only coming in sideways through login.component.spec.ts and signup.component.spec.ts.
I'm seeding/clearing localStorage directly per test since AuthService reads
it synchronously in the constructor

Author: Zamokuhle Zwane
Date: 06 August 2026
*/

import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import {
  HttpTestingController,
  provideHttpClientTesting,
} from '@angular/common/http/testing';
import { Router } from '@angular/router';
import {
  AuthService,
  AuthResponse,
  AuthUser,
  RegisterRequest,
  RegisterResponse,
} from './auth.service';

describe('AuthService', () => {
  let httpMock: HttpTestingController;
  let router: { navigate: jest.Mock };

  const mockUser: AuthUser = {
    id: 'user-1',
    email: 'enzokuhle@momentum.com',
    firstName: 'Enzokuhle',
    lastName: 'Khumalo',
    avatarUrl: null,
    emailVerified: true,
    roles: ['DEVELOPER'],
    mfaEnabled: false,
  };

  const mockAuthResponse: AuthResponse = {
    token: 'fake-jwt-token',
    expiresAt: '2026-08-08T09:00:00',
    requiresMfa: false,
    user: mockUser,
  };
   function configureAuthService(): AuthService {
    router = { navigate: jest.fn() };
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: Router, useValue: router },
      ],
    });
    httpMock = TestBed.inject(HttpTestingController);
    return TestBed.inject(AuthService);
  }

  afterEach(() => {
    localStorage.clear();
    httpMock.verify();
  });

  describe('constructor / loadStoredUser()', () => {
    it('should seed currentUser from a valid stored user on startup', () => {
      localStorage.setItem('auth_user', JSON.stringify(mockUser));
      localStorage.setItem('auth_token', 'stale-but-valid-token');

      const service = configureAuthService();

      expect(service.currentUser()).toEqual(mockUser);
    });

    it('should default currentUser to null when nothing is stored', () => {
      const service = configureAuthService();
      expect(service.currentUser()).toBeNull();
    });

    it('should clear a corrupted auth_user value and default to null', () => {
      //this covers the catch branch in loadStoredUser(), old app versions could have left behind a value that isn't valid JSON
      localStorage.setItem('auth_user', '{not valid json');

      const service = configureAuthService();

      expect(service.currentUser()).toBeNull();
      expect(localStorage.getItem('auth_user')).toBeNull();
    });

    it('should clear a leftover token when there is a token but no stored user', () => {
      //this is the pre-persistSession() migration case documented in the constructor comment: force a clean re-login instead of a stuck "Guest"
      localStorage.setItem('auth_token', 'orphaned-token');

      const service = configureAuthService();

      expect(service.currentUser()).toBeNull();
      expect(localStorage.getItem('auth_token')).toBeNull();
    });
  });

   describe('register()', () => {
    it('should POST to /api/auth/register', () => {
      const service = configureAuthService();
      const payload: RegisterRequest = {
        firstName: 'Zamokuhle',
        lastName: 'Zwane',
        email: 'zamokuhle@momentum.com',
        password: 'Password123!',
      };
      const response: RegisterResponse = {
        id: 'user-1',
        email: payload.email,
        firstName: payload.firstName,
        lastName: payload.lastName,
        createdAt: '2026-08-01T09:00:00',
        message: 'Registered successfully',
      };
       let result: RegisterResponse | undefined;
      service.register(payload).subscribe((res) => (result = res));

      const req = httpMock.expectOne('/api/auth/register');
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual(payload);
      req.flush(response);

      expect(result).toEqual(response);
    });
  });
   describe('login()', () => {
    it('should POST to /api/auth/login and persist the session on success', () => {
      const service = configureAuthService();

      service.login({ email: mockUser.email, password: 'pw' }).subscribe();

      const req = httpMock.expectOne('/api/auth/login');
      expect(req.request.method).toBe('POST');
      req.flush(mockAuthResponse);

      expect(localStorage.getItem('auth_token')).toBe('fake-jwt-token');
      expect(JSON.parse(localStorage.getItem('auth_user')!)).toEqual(mockUser);
      expect(service.currentUser()).toEqual(mockUser);
    });
    it('should surface the backend MessageResponse message on failure', () => {
      // backend errors come back as { message, redirectUrl }, see handleError()
      const service = configureAuthService();
      let caught: Error | undefined;

      service.login({ email: mockUser.email, password: 'wrong' }).subscribe({
        error: (err) => (caught = err),
      });

      const req = httpMock.expectOne('/api/auth/login');
      req.flush(
        { message: 'Invalid credentials' },
        { status: 401, statusText: 'Unauthorised' },
      );

      expect(caught?.message).toBe('Invalid credentials');
    });

    it('should fall back to a generic message when the backend sends no message', () => {
      const service = configureAuthService();
      let caught: Error | undefined;

      service.login({ email: mockUser.email, password: 'wrong' }).subscribe({
        error: (err) => (caught = err),
      });

      const req = httpMock.expectOne('/api/auth/login');
      req.flush(null, { status: 500, statusText: 'Internal Server Error' });

      expect(caught?.message).toBe('Something went wrong, try again in a bit.');
    });
  });

   describe('googleAuth()', () => {
    it('should POST the idToken to /api/auth/google and persist the session', () => {
      const service = configureAuthService();

      service.googleAuth('google-id-token').subscribe();

      const req = httpMock.expectOne('/api/auth/google');
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual({ idToken: 'google-id-token' });
      req.flush(mockAuthResponse);

      expect(service.currentUser()).toEqual(mockUser);
    });
  });

  describe('getToken() / isLoggedIn()', () => {
    it('should reflect whatever is in localStorage', () => {
      const service = configureAuthService();
      expect(service.isLoggedIn()).toBe(false);

      localStorage.setItem('auth_token', 'a-token');
      expect(service.getToken()).toBe('a-token');
      expect(service.isLoggedIn()).toBe(true);
    });
  });


describe('logout()', () => {
    it('should POST to /api/auth/logout with the bearer token, clear storage, and navigate to /login', () => {
      const service = configureAuthService();
      localStorage.setItem('auth_token', 'a-token');
      localStorage.setItem('auth_user', JSON.stringify(mockUser));
      service.currentUser.set(mockUser);

      service.logout();

      const req = httpMock.expectOne('/api/auth/logout');
      expect(req.request.method).toBe('POST');
      expect(req.request.headers.get('Authorization')).toBe('Bearer a-token');
      req.flush(null);

      expect(localStorage.getItem('auth_token')).toBeNull();
      expect(localStorage.getItem('auth_user')).toBeNull();
      expect(service.currentUser()).toBeNull();
      expect(router.navigate).toHaveBeenCalledWith(['/login']);
    });

    it('should skip the network call and still clear state when there is no token', () => {
      const service = configureAuthService();

      service.logout();

      httpMock.expectNone('/api/auth/logout');
      expect(router.navigate).toHaveBeenCalledWith(['/login']);
    });
  });
});