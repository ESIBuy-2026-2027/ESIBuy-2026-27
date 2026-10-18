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
  let getAllUsers: ReturnType<typeof vi.fn>;
  let deleteUserAdmin: ReturnType<typeof vi.fn>;
  let deactivateUserAdmin: ReturnType<typeof vi.fn>;
  let currentUser: ReturnType<typeof signal<{ id: string } | null>>;

  beforeEach(async () => {
    getAllUsers = vi.fn().mockReturnValue(of([]));
    deleteUserAdmin = vi.fn().mockReturnValue(of({ success: true, message: '', data: null, correlationId: null }));
    deactivateUserAdmin = vi.fn().mockReturnValue(of({ success: true, message: '', data: null, correlationId: null }));
    currentUser = signal<{ id: string } | null>(null);
    await TestBed.configureTestingModule({
      imports: [AdminUserList],
      providers: [
        provideRouter([]),
        {
          provide: AuthService,
          useValue: {
            getAllUsers,
            deleteAdminUser: deleteUserAdmin,
            deactivateAdminUser: deactivateUserAdmin,
            currentUser,
          },
        },
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

  it('does not render a link to administrative user creation', () => {
    component.loadUsers();
    fixture.detectChanges();

    const rendered = fixture.nativeElement as HTMLElement;

    expect(rendered.querySelector('#btnDarAltaUsuario')).toBeNull();
    expect(rendered.textContent).not.toContain('Dar de alta usuario');
  });

  it('renders a link to the home page', () => {
    const rendered = fixture.nativeElement as HTMLElement;
    expect(rendered.querySelector('.back-home-button')?.getAttribute('href')).toBe('');
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
      role: 'CLIENT' as const,
      active: true,
      blocked: false,
      createdAt: '2026-01-01T00:00:00Z',
    }];
    getAllUsers.mockReturnValue(of(users));

    component.loadUsers();
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
      role: 'CLIENT' as const,
      active: true,
      blocked: false,
      createdAt: '2026-01-01T00:00:00Z',
    }];
    getAllUsers.mockReturnValue(of(users));

    component.loadUsers();

    expect(component.users()).toEqual(users);
    expect(component.loading()).toBe(false);
    expect(getAllUsers).toHaveBeenCalled();
  });

  it('toggles a user active state through the admin endpoint', () => {
    const user = {
      id: 'user-1',
      email: 'client@example.com',
      firstName: 'Ana',
      lastName: 'López',
      nationalId: null,
      phone: null,
      profileImage: null,
      role: 'CLIENT' as const,
      active: true,
      blocked: false,
      createdAt: null,
    };
    component.users.set([user]);

    component.changeUserStatus(user, 'desactivar');

    expect(deactivateUserAdmin).toHaveBeenCalledWith(user.id);
    expect(component.users()[0].active).toBe(false);
  });

  it('shows a permissions error only when the endpoint returns 403', () => {
    getAllUsers.mockReturnValue(throwError(() =>
      new HttpErrorResponse({ status: 403, statusText: 'Forbidden' })));

    component.loadUsers();

    expect(component.error()).toBe('No tienes permisos para consultar los usuarios.');
    expect(component.loading()).toBe(false);
  });

  it('shows a general message for errors other than permission failures', () => {
    getAllUsers.mockReturnValue(throwError(() =>
      new HttpErrorResponse({ status: 404, statusText: 'Not Found' })));

    component.loadUsers();

    expect(component.error()).toBe('No se pudo cargar la lista de usuarios. Inténtalo de nuevo.');
  });

  it('requires two confirmations and removes another administrator after a successful response', () => {
    const user = {
      id: '507f1f77bcf86cd799439011',
      email: 'admin@example.com',
      firstName: 'Ana',
      lastName: 'López',
      nationalId: null,
      phone: null,
      profileImage: null,
      role: 'ADMIN' as const,
      active: true,
      blocked: false,
      createdAt: null,
    };
    component.users.set([user]);
    fixture.detectChanges();

    const rendered = fixture.nativeElement as HTMLElement;
    rendered.querySelector<HTMLButtonElement>('[aria-label="Eliminar administrador Ana López"]')?.click();
    fixture.detectChanges();
    expect(rendered.querySelector('[role="alertdialog"]')?.textContent).toContain('Retirar cuenta');

    rendered.querySelector<HTMLButtonElement>('.dialog-actions .btn-danger')?.click();
    fixture.detectChanges();
    expect(rendered.querySelector('[role="alertdialog"]')?.textContent).toContain('Confirmación final');
    rendered.querySelector<HTMLButtonElement>('.dialog-actions .btn-danger')?.click();

    expect(deleteUserAdmin).toHaveBeenCalledWith(user.id);
    expect(component.users()).toEqual([]);
    expect(component.successMessage()).toContain('admin@example.com');
    expect(component.pendingUser()).toBeNull();
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
      blocked: false,
      createdAt: null,
    };
    currentUser.set({ id: user.id });

    component.openConfirmation(user);

    expect(component.pendingUser()).toBeNull();
    expect(component.canDelete(user)).toBe(false);
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
      role: 'ADMIN' as const,
      active: true,
      blocked: false,
      createdAt: null,
    };
    deleteUserAdmin.mockReturnValue(throwError(() =>
      new HttpErrorResponse({ status: 403, statusText: 'Forbidden' })));
    component.openConfirmation(user);
    component.confirmFirstStep();

    component.deleteUser();

    expect(component.deletionError()).toBe('No tienes permisos para eliminar usuarios.');
    expect(component.users()).toEqual([]);
  });

  it('does not offer deletion for client or seller accounts', () => {
    const users = [
      {
        id: 'client-id', email: 'client@example.com', firstName: 'Ana', lastName: 'Client', nationalId: null,
        phone: null, profileImage: null, role: 'CLIENT' as const, active: true, blocked: false, createdAt: null
      },
      {
        id: 'seller-id', email: 'seller@example.com', firstName: 'Vera', lastName: 'Seller', nationalId: null,
        phone: null, profileImage: null, role: 'SELLER' as const, active: true, blocked: false, createdAt: null
      },
    ];
    component.users.set(users);
    fixture.detectChanges();

    const rendered = fixture.nativeElement as HTMLElement;
    expect(rendered.querySelector('[aria-label="Eliminar administrador Ana Client"]')).toBeNull();
    expect(rendered.querySelector('[aria-label="Eliminar administrador Vera Seller"]')).toBeNull();
    expect(users.every((user) => !component.canDelete(user))).toBe(true);
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
      role: 'ADMIN' as const,
      active: true,
      blocked: false,
      createdAt: null,
    };
    component.openConfirmation(user);
    fixture.detectChanges();

    const rendered = fixture.nativeElement as HTMLElement;
    expect(rendered.querySelector('.delete-dialog img')).toBeNull();
    expect(rendered.querySelector('.delete-dialog')?.textContent).toContain(user.firstName);
  });

  it('cancels confirmation and resets modal state when cancelarConfirmacion is called', () => {
    const user = {
      id: '507f1f77bcf86cd799439011',
      email: 'ana@example.com',
      firstName: 'Ana',
      lastName: 'López',
      nationalId: null,
      phone: null,
      profileImage: null,
      role: 'ADMIN' as const,
      active: true,
      blocked: false,
      createdAt: null,
    };
    component.openConfirmation(user);
    expect(component.pendingUser()).toEqual(user);
    expect(component.confirmationStep()).toBe(1);

    component.confirmFirstStep();
    expect(component.confirmationStep()).toBe(2);

    component.cancelConfirmation();
    expect(component.pendingUser()).toBeNull();
    expect(component.confirmationStep()).toBe(1);
    expect(component.deletionError()).toBe('');
  });

  it('closes the modal when Escape key is pressed', () => {
    const user = {
      id: '507f1f77bcf86cd799439011',
      email: 'ana@example.com',
      firstName: 'Ana',
      lastName: 'López',
      nationalId: null,
      phone: null,
      profileImage: null,
      role: 'ADMIN' as const,
      active: true,
      blocked: false,
      createdAt: null,
    };
    component.openConfirmation(user);
    expect(component.pendingUser()).toEqual(user);

    const escapeEvent = new KeyboardEvent('keydown', { key: 'Escape', cancelable: true });
    document.dispatchEvent(escapeEvent);

    expect(component.pendingUser()).toBeNull();
    expect(component.confirmationStep()).toBe(1);
  });

  it('initializes mensajeExito if history.state.successMessage is present', () => {
    history.replaceState({ successMessage: 'Los cambios se han guardado correctamente.' }, '');
    component.ngOnInit();
    expect(component.successMessage()).toBe('Los cambios se han guardado correctamente.');
    history.replaceState(null, '');
  });
});
