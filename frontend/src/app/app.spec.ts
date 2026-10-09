import { TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { signal, WritableSignal } from '@angular/core';
import { of } from 'rxjs';
import { AuthService, AuthenticatedUser } from './core/services/auth.service';
import { App } from './app';

describe('App', () => {
  let currentUser: WritableSignal<AuthenticatedUser | null>;
  let logout: ReturnType<typeof vi.fn>;

  beforeEach(async () => {
    currentUser = signal<AuthenticatedUser | null>(null);
    logout = vi.fn().mockReturnValue(of(undefined));

    await TestBed.configureTestingModule({
      imports: [App],
      providers: [
        provideRouter([]),
        { provide: AuthService, useValue: { currentUser, logout } },
      ],
    })
      .compileComponents();
  });

  it('should create the app', () => {
    const fixture = TestBed.createComponent(App);
    const app = fixture.componentInstance;
    expect(app).toBeTruthy();
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
    const profileOption = menuButtons.find((button) => button.textContent?.trim() === 'Perfil');
    const logoutOption = menuButtons.find((button) => button.textContent?.trim() === 'Cerrar sesión');

    expect(profileOption?.disabled).toBe(true);
    expect(logoutOption).toBeDefined();
    logoutOption?.click();
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
});