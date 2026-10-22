import { provideHttpClient } from '@angular/common/http';
import {
  HttpTestingController,
  provideHttpClientTesting,
} from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import {
  AuthService,
  AuthenticatedUser,
  CreateAdminRequest,
} from './auth.service';

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
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
      ],
    });

    service = TestBed.inject(AuthService);
    httpTesting = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpTesting.verify());

  it('stores the authenticated user after login and sends cookies', () => {
    service
      .login({ email: user.email, password: 'short' })
      .subscribe((result) => {
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
    service.login({
      email: user.email,
      password: 'short',
    }).subscribe();

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

  it('deletes an admin user using DELETE and credentials', () => {
    service.deleteAdminUser('user-123').subscribe();

    const request = httpTesting.expectOne(
      '/api/admin/users/user-123',
    );

    expect(request.request.method).toBe('DELETE');
    expect(request.request.withCredentials).toBe(true);

    request.flush({
      success: true,
      message: 'Usuario eliminado correctamente',
      data: null,
      correlationId: null,
    });
  });

  it('creates an admin user using POST and credentials', () => {
    const adminData: CreateAdminRequest = {
      nationalId: '12345678Z',
      firstName: 'Carlos',
      lastName: 'Gómez Pérez',
      email: 'carlos.admin@esibuy.com',
      password: 'Admin123456!#',
      confirmPassword: 'Admin123456!#',
      headquarters: 'Ciudad Real',
      profileImage: 'assets/images/avatars/av_client_1.jpeg',
    };

    service.createAdmin(adminData).subscribe((response) => {
      expect(response).toEqual({
        id: 'admin-1',
        email: 'carlos.admin@esibuy.com',
        firstName: 'Carlos',
        lastName: 'Gómez Pérez',
        nationalId: '12345678Z',
        role: 'ADMIN',
        active: true,
      });
    });

    const request = httpTesting.expectOne('/api/admin/users');

    expect(request.request.method).toBe('POST');
    expect(request.request.withCredentials).toBe(true);
    expect(request.request.body).toEqual(adminData);

    request.flush({
      id: 'admin-1',
      email: 'carlos.admin@esibuy.com',
      firstName: 'Carlos',
      lastName: 'Gómez Pérez',
      nationalId: '12345678Z',
      role: 'ADMIN',
      active: true,
    });
  });
});