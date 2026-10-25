import { TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { signal, WritableSignal } from '@angular/core';
import { of } from 'rxjs';
import { AuthService, AuthenticatedUser } from './core/services/auth.service';
import { App } from './app';
import { CartService } from './core/services/cart.service';

describe('App', () => {
  let currentUser: WritableSignal<AuthenticatedUser | null>;
  let logout: ReturnType<typeof vi.fn>;
  let cartService: { load: ReturnType<typeof vi.fn>; clear: ReturnType<typeof vi.fn>; itemCount: WritableSignal<number> };

  beforeEach(async () => {
    currentUser = signal<AuthenticatedUser | null>(null);
    logout = vi.fn().mockReturnValue(of(undefined));
    cartService = { load: vi.fn(), clear: vi.fn(), itemCount: signal(0) };

    await TestBed.configureTestingModule({
      imports: [App],
      providers: [
        provideRouter([]),
        { provide: AuthService, useValue: { currentUser, logout } },
        { provide: CartService, useValue: cartService },
      ],
    })
      .compileComponents();
  });

  it('should create the app', () => {
    const fixture = TestBed.createComponent(App);
    const app = fixture.componentInstance;
    expect(app).toBeTruthy();
  });

  it('should load the authenticated client cart and show its count in the header', () => {
    currentUser.set({
      id: 'client-1',
      email: 'client@example.com',
      role: 'CLIENT',
      active: true,
    });
    cartService.itemCount.set(3);
    const fixture = TestBed.createComponent(App);
    fixture.detectChanges();

    const cartLink = (fixture.nativeElement as HTMLElement).querySelector<HTMLAnchorElement>('.header-cart');
    expect(cartService.load).toHaveBeenCalled();
    expect(cartLink?.getAttribute('href')).toBe('/client/cart');
    expect(cartLink?.textContent).toContain('3');
  });

  it('should render the router outlet', async () => {
    const fixture = TestBed.createComponent(App);
    await fixture.whenStable();
    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelector('router-outlet')).not.toBeNull();
  });

  it('should show a non-navigating profile option and a working logout option', async () => {
    currentUser.set({
      id: 'user-1',
      email: 'user@example.com',
      role: 'CLIENT',
      active: true,
    });
    const fixture = TestBed.createComponent(App);
    fixture.detectChanges();

    const profileButton = (fixture.nativeElement as HTMLElement)
      .querySelector<HTMLButtonElement>('.profile-button');
    expect(profileButton?.querySelector('.visually-hidden')?.textContent?.trim()).toBe('Perfil');
    expect(profileButton?.querySelector('.profile-chevron')?.getAttribute('aria-hidden')).toBe('true');
    profileButton?.click();
    fixture.detectChanges();

    const menu = (fixture.nativeElement as HTMLElement).querySelector('#profile-menu');
    const menuButtons = Array.from(menu?.querySelectorAll<HTMLButtonElement>('button') ?? []);
    const navigate = vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);
    const profileOption = menuButtons.find((button) => button.textContent?.trim() === 'Perfil');
    const logoutOption = menuButtons.find((button) => button.textContent?.trim() === 'Cerrar sesión');

    expect(profileOption).toBeDefined();
    profileOption?.click();
    fixture.detectChanges();
    expect(navigate).toHaveBeenCalledWith(['/client/profile']);

    profileButton?.click();
    fixture.detectChanges();

    const reopenedMenu = (fixture.nativeElement as HTMLElement).querySelector('#profile-menu');
    const freshLogoutOption = Array.from(reopenedMenu?.querySelectorAll<HTMLButtonElement>('button') ?? [])
      .find((button) => button.textContent?.trim() === 'Cerrar sesión');

    expect(freshLogoutOption).toBeDefined();
    freshLogoutOption?.click();
    fixture.detectChanges();
    expect(logout).toHaveBeenCalledOnce();
    expect((fixture.nativeElement as HTMLElement).querySelector('#profile-menu')).toBeNull();
  });

  it('should close the account menu before navigating to the seller catalog', () => {
    currentUser.set({
      id: 'seller-1',
      email: 'seller@example.com',
      role: 'SELLER',
      active: true,
    });
    const fixture = TestBed.createComponent(App);
    fixture.detectChanges();

    const profileButton = (fixture.nativeElement as HTMLElement)
      .querySelector<HTMLButtonElement>('.profile-button');
    profileButton?.click();
    fixture.detectChanges();

    const navigate = vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);
    fixture.componentInstance.goCatalog();
    fixture.detectChanges();

    expect(navigate).toHaveBeenCalledWith(['/seller/catalog']);
    expect((fixture.nativeElement as HTMLElement).querySelector('#profile-menu')).toBeNull();
  });

  it('should render Iniciar sesión and Registrarse buttons when unauthenticated', () => {
    currentUser.set(null);
    const fixture = TestBed.createComponent(App);
    fixture.detectChanges();

    const compiled = fixture.nativeElement as HTMLElement;
    const loginLink = compiled.querySelector<HTMLAnchorElement>('.auth-nav-link');
    const registerLink = compiled.querySelector<HTMLAnchorElement>('.auth-nav-button');

    expect(loginLink?.textContent?.trim()).toBe('Iniciar sesión');
    expect(loginLink?.getAttribute('href')).toBe('/login');
    expect(registerLink?.textContent?.trim()).toBe('Registrarse');
    expect(registerLink?.getAttribute('href')).toBe('/register');
  });

  it('should render "Catálogo" button linking to /client/catalog for CLIENT', () => {
    currentUser.set({
      id: 'client-1',
      email: 'client@example.com',
      role: 'CLIENT',
      active: true,
    });
    const fixture = TestBed.createComponent(App);
    fixture.detectChanges();

    const compiled = fixture.nativeElement as HTMLElement;
    const roleButton = compiled.querySelector<HTMLAnchorElement>('.session-nav-button');

    expect(roleButton).not.toBeNull();
    expect(roleButton?.textContent?.trim()).toBe('Catálogo');
    expect(roleButton?.getAttribute('href')).toBe('/client/catalog');
  });

  it('should render "Catálogo" button linking to /seller/catalog for SELLER', () => {
    currentUser.set({
      id: 'seller-1',
      email: 'seller@example.com',
      role: 'SELLER',
      active: true,
    });
    const fixture = TestBed.createComponent(App);
    fixture.detectChanges();

    const compiled = fixture.nativeElement as HTMLElement;
    const roleButton = compiled.querySelector<HTMLAnchorElement>('.session-nav-button');

    expect(roleButton).not.toBeNull();
    expect(roleButton?.textContent?.trim()).toBe('Catálogo');
    expect(roleButton?.getAttribute('href')).toBe('/seller/catalog');
  });

  it('should render "Gestión Usuarios" button linking to /admin/usuarios for ADMIN', () => {
    currentUser.set({
      id: 'admin-1',
      email: 'admin@example.com',
      role: 'ADMIN',
      active: true,
    });
    const fixture = TestBed.createComponent(App);
    fixture.detectChanges();

    const compiled = fixture.nativeElement as HTMLElement;
    const roleButton = compiled.querySelector<HTMLAnchorElement>('.session-nav-button');

    expect(roleButton).not.toBeNull();
    expect(roleButton?.textContent?.trim()).toBe('Gestión Usuarios');
    expect(roleButton?.getAttribute('href')).toBe('/admin/usuarios');
  });
});