import { TestBed } from '@angular/core/testing';
import { ActivatedRouteSnapshot, provideRouter, Router, RouterStateSnapshot, UrlTree } from '@angular/router';
import { signal } from '@angular/core';
import { AuthenticatedUser, AuthService } from '../services/auth.service';
import { authGuard } from './auth.guard';
import { roleGuard } from './role.guard';

describe('authentication route guards', () => {
  const currentUser = signal<AuthenticatedUser | null>(null);

  beforeEach(() => {
    currentUser.set(null);
    TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        { provide: AuthService, useValue: { currentUser: currentUser.asReadonly() } },
      ],
    });
  });

  it('redirects unauthenticated navigation home with its return URL', () => {
    const result = TestBed.runInInjectionContext(() =>
      authGuard({} as ActivatedRouteSnapshot, { url: '/admin/usuarios' } as RouterStateSnapshot));

    expect(result).toBeInstanceOf(UrlTree);
    expect(TestBed.inject(Router).serializeUrl(result as UrlTree))
      .toBe('/?returnUrl=%2Fadmin%2Fusuarios');
  });

  it('allows an authenticated user through the authentication guard', () => {
    currentUser.set({
      id: 'user-1',
      email: 'admin@example.com',
      role: 'ADMIN',
      active: true,
    });

    const result = TestBed.runInInjectionContext(() =>
      authGuard({} as ActivatedRouteSnapshot, { url: '/admin/usuarios' } as RouterStateSnapshot));

    expect(result).toBe(true);
  });

  it('allows only the required role and redirects other roles home', () => {
    currentUser.set({
      id: 'user-1',
      email: 'seller@example.com',
      role: 'SELLER',
      active: true,
    });

    const allowed = TestBed.runInInjectionContext(() =>
      roleGuard('SELLER')({} as ActivatedRouteSnapshot, { url: '/products/new' } as RouterStateSnapshot));
    const denied = TestBed.runInInjectionContext(() =>
      roleGuard('ADMIN')({} as ActivatedRouteSnapshot, { url: '/admin/usuarios' } as RouterStateSnapshot));

    expect(allowed).toBe(true);
    expect(denied).toBeInstanceOf(UrlTree);
    expect(TestBed.inject(Router).serializeUrl(denied as UrlTree)).toBe('/');
  });

  it('allows any role from an array of allowed roles', () => {
    currentUser.set({
      id: 'admin-1',
      email: 'admin@example.com',
      role: 'ADMIN',
      active: true,
    });

    const allowed = TestBed.runInInjectionContext(() =>
      roleGuard(['CLIENT', 'ADMIN'])({} as ActivatedRouteSnapshot, { url: '/client/catalog' } as RouterStateSnapshot));

    expect(allowed).toBe(true);

    currentUser.set({
      id: 'seller-1',
      email: 'seller@example.com',
      role: 'SELLER',
      active: true,
    });

    const denied = TestBed.runInInjectionContext(() =>
      roleGuard(['CLIENT', 'ADMIN'])({} as ActivatedRouteSnapshot, { url: '/client/catalog' } as RouterStateSnapshot));

    expect(denied).toBeInstanceOf(UrlTree);
  });

  it('redirects unauthenticated role checks home', () => {
    const result = TestBed.runInInjectionContext(() =>
      roleGuard('ADMIN')({} as ActivatedRouteSnapshot, { url: '/admin/usuarios' } as RouterStateSnapshot));

    expect(result).toBeInstanceOf(UrlTree);
    expect(TestBed.inject(Router).serializeUrl(result as UrlTree))
      .toBe('/?returnUrl=%2Fadmin%2Fusuarios');
  });
});
