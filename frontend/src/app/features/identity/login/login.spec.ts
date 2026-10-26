import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpErrorResponse, HttpHeaders } from '@angular/common/http';
import { ActivatedRoute, convertToParamMap, ParamMap, Router } from '@angular/router';
import { of, throwError } from 'rxjs';
import { vi } from 'vitest';
import { AuthService } from '../../../core/services/auth.service';
import { Login } from './login';

describe('Login', () => {
  let component: Login;
  let fixture: ComponentFixture<Login>;
  let login: ReturnType<typeof vi.fn>;
  let verifyLoginOtp: ReturnType<typeof vi.fn>;
  let navigate: ReturnType<typeof vi.fn>;
  let routeSnapshot: { queryParamMap: ParamMap };

  beforeEach(async () => {
    login = vi.fn();
    verifyLoginOtp = vi.fn();
    navigate = vi.fn().mockResolvedValue(true);
    routeSnapshot = { queryParamMap: convertToParamMap({}) };
    await TestBed.configureTestingModule({
      imports: [Login],
      providers: [
        { provide: AuthService, useValue: { login, verifyLoginOtp } },
        { provide: Router, useValue: { navigate } },
        {
          provide: ActivatedRoute,
          useValue: { snapshot: routeSnapshot },
        },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(Login);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  afterEach(() => {
    vi.useRealTimers();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('accepts an existing short password and routes sellers to their catalog', () => {
    login.mockReturnValue(of({
      id: 'seller-1',
      email: 'seller@example.com',
      role: 'SELLER',
      active: true,
    }));
    component.loginForm.patchValue({
      email: 'seller@example.com',
      password: 'short',
    });

    component.onSubmit();

    expect(login).toHaveBeenCalledWith(expect.objectContaining({
      email: 'seller@example.com',
      password: 'short',
      totpCode: '',
    }));
    expect(navigate).toHaveBeenCalledWith(['/seller/catalog']);
  });

  it('routes administrators to the administration page', () => {
    login.mockReturnValue(of({
      id: 'admin-1',
      email: 'admin@example.com',
      role: 'ADMIN',
      active: true,
    }));
    component.loginForm.patchValue({
      email: 'admin@example.com',
      password: 'Password123!',
    });

    component.onSubmit();

    expect(navigate).toHaveBeenCalledWith(['/admin/usuarios']);
  });

  it('routes clients to their catalog', () => {
    login.mockReturnValue(of({
      id: 'client-1',
      email: 'client@example.com',
      role: 'CLIENT',
      active: true,
    }));
    component.loginForm.patchValue({
      email: 'client@example.com',
      password: 'Password123!',
    });

    component.onSubmit();

    expect(navigate).toHaveBeenCalledWith(['/client/catalog']);
  });

  it('shows the email code entry step after the backend requires 3FA', () => {
    login.mockReturnValue(of({ requires3fa: true, expiresInSeconds: 600 }));
    component.loginForm.patchValue({
      email: 'seller@example.com',
      password: 'Password123!',
    });

    component.onSubmit();
    fixture.detectChanges();

    expect(component.requires3FA).toBe(true);
    expect(component.requires2FA).toBe(false);
    expect(fixture.nativeElement.textContent).toContain('código de seis dígitos enviado al correo');
    expect(fixture.nativeElement.querySelector('#totpCode')).not.toBeNull();
  });

  it('submits the 3FA email code to the OTP endpoint and routes the authenticated seller', () => {
    login.mockReturnValue(of({ requires3fa: true, expiresInSeconds: 600 }));
    verifyLoginOtp.mockReturnValue(of({
      id: 'seller-1',
      email: 'seller@example.com',
      role: 'SELLER',
      active: true,
    }));
    component.loginForm.patchValue({
      email: 'seller@example.com',
      password: 'Password123!',
    });
    component.onSubmit();
    component.loginForm.controls['totpCode'].setValue('123456');

    component.onSubmit();

    expect(verifyLoginOtp).toHaveBeenCalledWith('123456');
    expect(login).toHaveBeenCalledTimes(1);
    expect(navigate).toHaveBeenCalledWith(['/seller/catalog']);
    expect(component.requires3FA).toBe(false);
  });

  it('moves from TOTP verification to email OTP verification', () => {
    login.mockReturnValueOnce(of({ requires2fa: true }))
      .mockReturnValueOnce(of({ requires3fa: true, expiresInSeconds: 600 }));
    component.loginForm.patchValue({
      email: 'seller@example.com',
      password: 'Password123!',
    });

    component.onSubmit();
    component.loginForm.controls['totpCode'].setValue('123456');
    component.onSubmit();
    fixture.detectChanges();

    expect(login).toHaveBeenCalledTimes(2);
    expect(component.requires2FA).toBe(false);
    expect(component.requires3FA).toBe(true);
    expect(component.loginForm.controls['totpCode'].value).toBe('');
  });

  it('shows the session-expired message from the redirect query parameter', () => {
    routeSnapshot.queryParamMap = convertToParamMap({ expired: 'true' });
    fixture = TestBed.createComponent(Login);

    expect(fixture.componentInstance.serverError).toBe('Tu sesión ha expirado');
  });

  it('navigates to password recovery when requested', () => {
    component.showForgotPasswordMessage();

    expect(navigate).toHaveBeenCalledWith(['/recuperar-password']);
  });

  it('blocks login and counts down after a rate-limit response', () => {
    vi.useFakeTimers();
    try {
      login.mockReturnValue(throwError(() => new HttpErrorResponse({
        status: 429,
        statusText: 'Too Many Requests',
        headers: new HttpHeaders({ 'Retry-After': '3' }),
        error: { message: 'Demasiados intentos de inicio de sesión.' },
      })));
      component.loginForm.patchValue({
        email: 'seller@example.com',
        password: 'short',
      });

      component.onSubmit();
      fixture.detectChanges();

      expect(component.retryAfterSeconds).toBe(3);
      expect(component.serverError).toContain('3 segundos');
      expect(fixture.nativeElement.querySelector('.btn-submit').disabled).toBe(true);

      vi.advanceTimersByTime(3000);

      expect(component.retryAfterSeconds).toBe(0);
      expect(component.serverError).toBe('Demasiados intentos de inicio de sesión.');
    } finally {
      vi.useRealTimers();
    }
  });

  it('shows the account-deleted message from the redirect query parameter', () => {
    routeSnapshot.queryParamMap = convertToParamMap({ deleted: 'true' });
    fixture = TestBed.createComponent(Login);

    expect(fixture.componentInstance.successMessage).toBe('Tu cuenta se ha eliminado correctamente.');
  });
});