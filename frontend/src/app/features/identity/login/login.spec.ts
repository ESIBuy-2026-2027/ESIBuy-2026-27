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
  let navigate: ReturnType<typeof vi.fn>;
  let routeSnapshot: { queryParamMap: ParamMap };

  beforeEach(async () => {
    login = vi.fn();
    navigate = vi.fn().mockResolvedValue(true);
    routeSnapshot = { queryParamMap: convertToParamMap({}) };
    await TestBed.configureTestingModule({
      imports: [Login],
      providers: [
        { provide: AuthService, useValue: { login } },
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
    component.loginForm.setValue({
      email: 'seller@example.com',
      password: 'short',
    });

    component.onSubmit();

    expect(login).toHaveBeenCalledWith({
      email: 'seller@example.com',
      password: 'short',
    });
    expect(navigate).toHaveBeenCalledWith(['/seller/catalog']);
  });

  it('routes administrators to the administration page', () => {
    login.mockReturnValue(of({
      id: 'admin-1',
      email: 'admin@example.com',
      role: 'ADMIN',
      active: true,
    }));
    component.loginForm.setValue({
      email: 'admin@example.com',
      password: 'Password123!',
    });

    component.onSubmit();

    expect(navigate).toHaveBeenCalledWith(['/admin/usuarios']);
  });

  it('shows the session-expired message from the redirect query parameter', () => {
    routeSnapshot.queryParamMap = convertToParamMap({ expired: 'true' });
    fixture = TestBed.createComponent(Login);

    expect(fixture.componentInstance.serverError).toBe('Tu sesión ha expirado');
  });

  it('shows the forgot password information message', () => {
    component.showForgotPasswordMessage();

    expect(component.forgotPasswordMessage).toBe(
      'La recuperación de contraseña estará disponible próximamente.'
    );
  });

  it('blocks login and counts down after a rate-limit response', () => {
    vi.useFakeTimers();
    login.mockReturnValue(throwError(() => new HttpErrorResponse({
      status: 429,
      statusText: 'Too Many Requests',
      headers: new HttpHeaders({ 'Retry-After': '3' }),
      error: { message: 'Demasiados intentos de inicio de sesión.' },
    })));
    component.loginForm.setValue({
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
    vi.useRealTimers();
  });
});