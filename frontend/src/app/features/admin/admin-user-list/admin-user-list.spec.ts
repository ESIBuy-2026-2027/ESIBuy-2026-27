import { ComponentFixture, TestBed } from '@angular/core/testing';
import { signal } from '@angular/core';
import { provideRouter } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { of, throwError } from 'rxjs';
import { vi } from 'vitest';
import { AuthService } from '../../../core/services/auth.service';
import { AdminUserList } from './admin-user-list';

describe('AdminUserList', () => {
  let component: AdminUserList;
  let fixture: ComponentFixture<AdminUserList>;
  let obtenerTodosLosUsuarios: ReturnType<typeof vi.fn>;
  let eliminarUsuarioAdmin: ReturnType<typeof vi.fn>;
  let currentUser: ReturnType<typeof signal<{ id: string } | null>>;

  beforeEach(async () => {
    obtenerTodosLosUsuarios = vi.fn().mockReturnValue(of([]));
    eliminarUsuarioAdmin = vi.fn().mockReturnValue(of({ success: true, message: '', data: null, correlationId: null }));
    currentUser = signal<{ id: string } | null>(null);
    await TestBed.configureTestingModule({
      imports: [AdminUserList],
      providers: [
        provideRouter([]),
        { provide: AuthService, useValue: { obtenerTodosLosUsuarios, eliminarUsuarioAdmin, currentUser } },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(AdminUserList);
    component = fixture.componentInstance;
    fixture.detectChanges();
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
  
  it('renders the link to normal user creation', () => {
    component.cargarUsuarios();
    fixture.detectChanges();

    const rendered = fixture.nativeElement as HTMLElement;
    const btnAltaUsuario = rendered.querySelector('#btnDarAltaUsuario');
    
    expect(btnAltaUsuario).toBeTruthy();
    expect(btnAltaUsuario?.getAttribute('href')).toContain('/admin/usuarios/alta');
  });

  it('renders users returned by the admin endpoint and links to admin creation', () => {
    const users = [{
      id: 'user-1',
      email: 'ana@example.com',
      firstName: 'Ana',
      lastName: 'López',
      nationalId: '12345678Z',
      phone: '600123456',
      profileImage: '',
      role: 'CLIENT',
      active: true,
      createdAt: '2026-01-01T00:00:00Z',
    }];
    obtenerTodosLosUsuarios.mockReturnValue(of(users));

    component.cargarUsuarios();
    fixture.detectChanges();

    const rendered = fixture.nativeElement as HTMLElement;
    expect(rendered.textContent).toContain('Ana López');
    expect(rendered.textContent).toContain('ana@example.com');
    expect(rendered.querySelector('#btnDarAltaAdmin')?.getAttribute('href')).toContain('/admin/altas');
  });

  it('loads and displays the users returned by the admin endpoint', () => {
    const users = [{
      id: 'user-1',
      email: 'client@example.com',
      firstName: 'Ana',
      lastName: 'López',
      nationalId: '12345678Z',
      phone: '',
      profileImage: '',
      role: 'CLIENT',
      active: true,
      createdAt: '2026-01-01T00:00:00Z',
    }];
    obtenerTodosLosUsuarios.mockReturnValue(of(users));

    component.cargarUsuarios();

    expect(component.usuarios()).toEqual(users);
    expect(component.cargando()).toBe(false);
    expect(obtenerTodosLosUsuarios).toHaveBeenCalled();
  });

  it('shows a permissions error only when the endpoint returns 403', () => {
    obtenerTodosLosUsuarios.mockReturnValue(throwError(() =>
      new HttpErrorResponse({ status: 403, statusText: 'Forbidden' })));

    component.cargarUsuarios();

    expect(component.error()).toBe('No tienes permisos para consultar los usuarios.');
    expect(component.cargando()).toBe(false);
  });

  it('shows a general message for errors other than permission failures', () => {
    obtenerTodosLosUsuarios.mockReturnValue(throwError(() =>
      new HttpErrorResponse({ status: 404, statusText: 'Not Found' })));

    component.cargarUsuarios();

    expect(component.error()).toBe('No se pudo cargar la lista de usuarios. Inténtalo de nuevo.');
  });

  it('requires two confirmations and removes the account after a successful response', () => {
    const user = {
      id: '507f1f77bcf86cd799439011',
      email: 'ana@example.com',
      firstName: 'Ana',
      lastName: 'López',
      nationalId: null,
      phone: null,
      profileImage: null,
      role: 'CLIENT' as const,
      active: true,
      createdAt: null,
    };
    component.usuarios.set([user]);
    fixture.detectChanges();

    const rendered = fixture.nativeElement as HTMLElement;
    rendered.querySelector<HTMLButtonElement>('[aria-label="Eliminar usuario Ana López"]')?.click();
    fixture.detectChanges();
    expect(rendered.querySelector('[role="alertdialog"]')?.textContent).toContain('Retirar cuenta');

    rendered.querySelector<HTMLButtonElement>('.dialog-actions .btn-danger')?.click();
    fixture.detectChanges();
    expect(rendered.querySelector('[role="alertdialog"]')?.textContent).toContain('Confirmación final');
    rendered.querySelector<HTMLButtonElement>('.dialog-actions .btn-danger')?.click();

    expect(eliminarUsuarioAdmin).toHaveBeenCalledWith(user.id);
    expect(component.usuarios()).toEqual([]);
    expect(component.mensajeExito()).toContain('ana@example.com');
    expect(component.usuarioPendiente()).toBeNull();
  });

  it('prevents opening confirmation for the authenticated account', () => {
    const user = {
      id: 'admin-id',
      email: 'admin@example.com',
      firstName: 'Admin',
      lastName: 'ESIBuy',
      nationalId: null,
      phone: null,
      profileImage: null,
      role: 'ADMIN' as const,
      active: true,
      createdAt: null,
    };
    currentUser.set({ id: user.id });

    component.abrirConfirmacion(user);

    expect(component.usuarioPendiente()).toBeNull();
    expect(component.puedeEliminar(user)).toBe(false);
  });

  it('shows a safe permission message when deletion is rejected', () => {
    const user = {
      id: '507f1f77bcf86cd799439011',
      email: 'ana@example.com',
      firstName: 'Ana',
      lastName: 'López',
      nationalId: null,
      phone: null,
      profileImage: null,
      role: 'CLIENT' as const,
      active: true,
      createdAt: null,
    };
    eliminarUsuarioAdmin.mockReturnValue(throwError(() =>
      new HttpErrorResponse({ status: 403, statusText: 'Forbidden' })));
    component.abrirConfirmacion(user);
    component.confirmarPrimerPaso();

    component.eliminarUsuario();

    expect(component.errorEliminacion()).toBe('No tienes permisos para eliminar usuarios.');
    expect(component.usuarios()).toEqual([]);
  });

  it('explains that sellers with associated products cannot be deleted', () => {
    const user = {
      id: '507f1f77bcf86cd799439011',
      email: 'seller@example.com',
      firstName: 'Vera',
      lastName: 'Seller',
      nationalId: null,
      phone: null,
      profileImage: null,
      role: 'SELLER' as const,
      active: true,
      createdAt: null,
    };
    eliminarUsuarioAdmin.mockReturnValue(throwError(() =>
      new HttpErrorResponse({ status: 409, statusText: 'Conflict' })));
    component.abrirConfirmacion(user);
    component.confirmarPrimerPaso();

    component.eliminarUsuario();

    expect(component.errorEliminacion()).toBe(
      'No se puede eliminar este vendedor mientras tenga productos asociados.');
  });

  it('renders user-provided confirmation text as text rather than markup', () => {
    const user = {
      id: '507f1f77bcf86cd799439011',
      email: 'ana@example.com',
      firstName: '<img src=x onerror=alert(1)>',
      lastName: 'López',
      nationalId: null,
      phone: null,
      profileImage: null,
      role: 'CLIENT' as const,
      active: true,
      createdAt: null,
    };
    component.abrirConfirmacion(user);
    fixture.detectChanges();

    const rendered = fixture.nativeElement as HTMLElement;
    expect(rendered.querySelector('.delete-dialog img')).toBeNull();
    expect(rendered.querySelector('.delete-dialog')?.textContent).toContain(user.firstName);
  });
});