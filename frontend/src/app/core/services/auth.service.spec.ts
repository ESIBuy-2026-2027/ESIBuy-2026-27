import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { AuthService, AuthenticatedUser } from './auth.service';

describe('AuthService', () => {
  let service: AuthService;
  let httpTesting: HttpTestingController;

  const user: AuthenticatedUser = {
    id: 'user-1',
    email: 'seller@example.com',
    role: 'SELLER',
    active: true,
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(AuthService);
    httpTesting = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpTesting.verify());

  it('stores the authenticated user after login and sends cookies', () => {
    service.login({ email: user.email, password: 'short' }).subscribe((result) => {
      expect(result).toEqual(user);
      expect(service.currentUser()).toEqual(user);
      expect(service.isAuthenticated()).toBe(true);
    });

    const request = httpTesting.expectOne('/api/users/login');
    expect(request.request.method).toBe('POST');
    expect(request.request.withCredentials).toBe(true);
    request.flush(user);
  });

  it('loads the current user from the server', () => {
    service.me().subscribe((result) => {
      expect(result).toEqual(user);
      expect(service.currentUser()).toEqual(user);
    });

    const request = httpTesting.expectOne('/api/users/me');
    expect(request.request.withCredentials).toBe(true);
    request.flush(user);
  });

  it('treats an absent session at startup as unauthenticated', () => {
    service.loadCurrentUser().subscribe((result) => {
      expect(result).toBeNull();
      expect(service.currentUser()).toBeNull();
    });

    httpTesting.expectOne('/api/users/me').flush(
      { message: 'Autenticación requerida' },
      { status: 401, statusText: 'Unauthorized' },
    );
  });

  it('clears the user after successful logout', () => {
    service.login({ email: user.email, password: 'short' }).subscribe();
    httpTesting.expectOne('/api/users/login').flush(user);

    service.logout().subscribe(() => {
      expect(service.currentUser()).toBeNull();
      expect(service.isAuthenticated()).toBe(false);
    });

    const request = httpTesting.expectOne('/api/users/logout');
    expect(request.request.method).toBe('POST');
    expect(request.request.withCredentials).toBe(true);
    request.flush(null);
  });
});
