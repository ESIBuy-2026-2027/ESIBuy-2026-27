import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { signal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { of } from 'rxjs';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { AuthService } from '../../../core/services/auth.service';
import { ClientProfile } from './client-profile';

describe('ClientProfile', () => {
    let fixture: ComponentFixture<ClientProfile>;
    let httpMock: HttpTestingController;
    const logout = vi.fn(() => of(void 0));

    const mockProfile = {
        firstName: 'Ana',
        lastName: 'García',
        email: 'ana@esibuy.com',
        phone: '600123456',
        maskedNationalId: '***4567*',
        profileImage: 'assets/images/avatars/av_client_1.jpeg',
        customerType: 'STANDARD',
        birthDate: '1990-01-01',
        totpEnabled: false,
    };

    beforeEach(async () => {
        logout.mockClear();
        await TestBed.configureTestingModule({
            imports: [ClientProfile],
            providers: [
                provideHttpClient(),
                provideHttpClientTesting(),
                provideRouter([]),
                {
                    provide: AuthService,
                    useValue: { currentUser: signal({ email: 'ana@esibuy.com', role: 'CLIENT' }), logout },
                },
            ],
        }).compileComponents();

        httpMock = TestBed.inject(HttpTestingController);
        fixture = TestBed.createComponent(ClientProfile);
        fixture.detectChanges();

        const req = httpMock.expectOne('/api/users/me/profile');
        expect(req.request.method).toBe('GET');
        req.flush(mockProfile);

        fixture.detectChanges();
        await fixture.whenStable();
    });

    it('muestra el título y los campos en solo lectura', () => {
        const el: HTMLElement = fixture.nativeElement;
        expect(el.querySelector('h1')?.textContent).toContain('Perfil');
        expect(el.querySelector('.section-title')?.textContent).toContain('Datos personales');
        el.querySelectorAll('input').forEach((input) => expect(input.readOnly).toBe(true));
        const clientLinks = el.querySelectorAll('.seller-navigation .navigation-link');
        expect(clientLinks).toHaveLength(5);
        expect(clientLinks[2].textContent).toContain('Mis pedidos');
        expect(clientLinks[4].getAttribute('href')).toBe('/client/profile');
        expect(clientLinks[4].getAttribute('aria-current')).toBe('page');
    });

    it('permite plegar y desplegar la navegación global del cliente', () => {
        const sidebar = fixture.nativeElement.querySelector('.seller-shell') as HTMLElement;
        const toggle = fixture.nativeElement.querySelector('.app-sidebar-toggle') as HTMLButtonElement;

        toggle.click();
        fixture.detectChanges();
        expect(sidebar.classList.contains('app-sidebar-collapsed')).toBe(true);

        const openToggle = fixture.nativeElement.querySelector('.app-sidebar-toggle-open') as HTMLButtonElement;
        openToggle.click();
        fixture.detectChanges();
        expect(sidebar.classList.contains('app-sidebar-collapsed')).toBe(false);
    });

    it('muestra el correo de la sesión', () => {
        const email = fixture.nativeElement.querySelector('#email') as HTMLInputElement;
        expect(email.value).toBe('ana@esibuy.com');
    });

    it('muestra las secciones del perfil', () => {
        const navItems = fixture.nativeElement.querySelectorAll('.sidebar-nav .nav-item');
        expect(navItems.length).toBe(4);
    });

    it('cierra sesión y vuelve al inicio', () => {
        const navigate = vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);
        (fixture.nativeElement.querySelector('.logout-button') as HTMLButtonElement).click();
        expect(logout).toHaveBeenCalled();
        expect(navigate).toHaveBeenCalledWith(['/']);
    });
});