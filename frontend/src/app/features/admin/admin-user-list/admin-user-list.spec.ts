import { ComponentFixture, TestBed } from '@angular/core/testing';
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

  beforeEach(async () => {
    obtenerTodosLosUsuarios = vi.fn().mockReturnValue(of([]));
    await TestBed.configureTestingModule({
      imports: [AdminUserList],
      providers: [
        provideRouter([]),
        { provide: AuthService, useValue: { obtenerTodosLosUsuarios } },
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
});