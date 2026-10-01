import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { vi } from 'vitest';
import { authInterceptor } from './auth.interceptor';
import { AuthService } from '../services/auth.service';

describe('authInterceptor', () => {
  let http: HttpClient;
  let httpTesting: HttpTestingController;
  let clearCurrentUser: ReturnType<typeof vi.fn>;
  let navigate: ReturnType<typeof vi.fn>;

  beforeEach(() => {
    clearCurrentUser = vi.fn();
    navigate = vi.fn().mockResolvedValue(true);
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([authInterceptor])),
        provideHttpClientTesting(),
        { provide: AuthService, useValue: { clearCurrentUser } },
        { provide: Router, useValue: { navigate } },
      ],
    });
    http = TestBed.inject(HttpClient);
    httpTesting = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpTesting.verify());

  it('clears local authentication and redirects after a protected request returns 401', () => {
    http.get('/api/products/summary').subscribe({ error: () => undefined });

    httpTesting.expectOne('/api/products/summary').flush(
      { message: 'Autenticación requerida' },
      { status: 401, statusText: 'Unauthorized' },
    );

    expect(clearCurrentUser).toHaveBeenCalledOnce();
    expect(navigate).toHaveBeenCalledWith(['/login'], {
      queryParams: { expired: 'true' },
    });
  });

  it('does not redirect for login, session lookup, or logout responses', () => {
    http.post('/api/users/login', {}).subscribe({ error: () => undefined });
    httpTesting.expectOne('/api/users/login').flush({}, {
      status: 401,
      statusText: 'Unauthorized',
    });

    http.get('/api/users/me').subscribe({ error: () => undefined });
    httpTesting.expectOne('/api/users/me').flush({}, {
      status: 401,
      statusText: 'Unauthorized',
    });

    http.post('/api/users/logout', {}).subscribe({ error: () => undefined });
    httpTesting.expectOne('/api/users/logout').flush({}, {
      status: 401,
      statusText: 'Unauthorized',
    });

    expect(clearCurrentUser).not.toHaveBeenCalled();
    expect(navigate).not.toHaveBeenCalled();
  });
});
