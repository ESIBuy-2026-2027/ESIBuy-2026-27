import { signal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { of } from 'rxjs';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { AuthService } from '../../../core/services/auth.service';
import { ClientProfile } from './client-profile';

describe('ClientProfile', () => {
    let fixture: ComponentFixture<ClientProfile>;
    const logout = vi.fn(() => of(void 0));

    beforeEach(async () => {
        logout.mockClear();
        await TestBed.configureTestingModule({
            imports: [ClientProfile],
            providers: [
                provideRouter([]),
                {
                    provide: AuthService,
                    useValue: { currentUser: signal({ email: 'ana@esibuy.com', role: 'CLIENT' }), logout },
                },
            ],
        }).compileComponents();

        fixture = TestBed.createComponent(ClientProfile);
        await fixture.whenStable();
    });

    it('muestra el título y los campos en solo lectura', () => {
        const el: HTMLElement = fixture.nativeElement;
        expect(el.querySelector('h1')?.textContent).toContain('Datos personales');
        el.querySelectorAll('input').forEach((input) => expect(input.readOnly).toBe(true));
    });

    it('muestra el correo de la sesión', () => {
        const email = fixture.nativeElement.querySelector('#email') as HTMLInputElement;
        expect(email.value).toBe('ana@esibuy.com');
    });

    it('deja las secciones de otras HU como próximamente', () => {
        const disabled = fixture.nativeElement.querySelectorAll('.nav-item.is-disabled');
        expect(disabled.length).toBe(5);
    });

    it('cierra sesión y vuelve al inicio', () => {
        const navigate = vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);
        (fixture.nativeElement.querySelector('.logout-button') as HTMLButtonElement).click();
        expect(logout).toHaveBeenCalled();
        expect(navigate).toHaveBeenCalledWith(['/']);
    });
});