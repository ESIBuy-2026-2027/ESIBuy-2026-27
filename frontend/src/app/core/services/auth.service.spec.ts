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

  it('calls DELETE on /api/admin/usuarios/:id with credentials to delete an admin user', () => {
    service.eliminarUsuarioAdmin('user-123').subscribe();

    const request = httpTesting.expectOne('/api/admin/usuarios/user-123');
    expect(request.request.method).toBe('DELETE');
    expect(request.request.withCredentials).toBe(true);
    request.flush({ success: true, message: 'Usuario eliminado correctamente', data: null, correlationId: null });
  });

  it('calls POST on /api/admin/alta with credentials to create an admin user', () => {
    const adminData = {
      dni: '12345678Z',
      nombre: 'Carlos',
      apellidos: 'Gómez Pérez',
      email: 'carlos.admin@esibuy.com',
      contrasena: 'Admin123456!#',
      repetirContrasena: 'Admin123456!#',
      sede: 'Ciudad Real',
      imagenPerfil: 'https://avatar.ejemplo.com/carlos.png'
    };

    service.crearAdministrador(adminData).subscribe((response: any) => {
      expect(response.email).toBe('carlos.admin@esibuy.com');
      expect(response.rol).toBe('ADMIN');
    });

    const request = httpTesting.expectOne('/api/admin/alta');
    expect(request.request.method).toBe('POST');
    expect(request.request.withCredentials).toBe(true);
    expect(request.request.body).toEqual(adminData);
    request.flush({
      id: 'admin-1',
      email: 'carlos.admin@esibuy.com',
      nombre: 'Carlos',
      apellidos: 'Gómez Pérez',
      dni: '12345678Z',
      rol: 'ADMIN',
      activo: true,
      fechaIncorporacion: '2026-10-07T12:00:00'
    });
  });
});
