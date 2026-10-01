import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap, ParamMap, Router } from '@angular/router';
import { of } from 'rxjs';
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

  it('accepts a short password and routes sellers to product creation', () => {
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
    expect(navigate).toHaveBeenCalledWith(['/products/new']);
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
});