import { HttpErrorResponse } from '@angular/common/http';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { of, throwError } from 'rxjs';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { AccountService } from '../../core/services/account.service';
import { AuthService, UserRole } from '../../core/services/auth.service';
import { DeleteAccount } from './delete-account';

describe('DeleteAccount', () => {
    const deleteOwnAccount = vi.fn();
    const clearCurrentUser = vi.fn();
    let fixture: ComponentFixture<DeleteAccount>;
    let el: HTMLElement;
    let navigate: ReturnType<typeof vi.fn>;

    async function render(role: UserRole): Promise<void> {
        await TestBed.configureTestingModule({
            imports: [DeleteAccount],
            providers: [
                provideRouter([]),
                { provide: AccountService, useValue: { deleteOwnAccount } },
                { provide: AuthService, useValue: { clearCurrentUser } },
            ],
        }).compileComponents();

        navigate = vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true) as unknown as ReturnType<typeof vi.fn>;
        fixture = TestBed.createComponent(DeleteAccount);
        fixture.componentRef.setInput('role', role);
        await fixture.whenStable();
        el = fixture.nativeElement as HTMLElement;
    }

    async function click(selector: string): Promise<void> {
        (el.querySelector(selector) as HTMLButtonElement).click();
        await fixture.whenStable();
    }

    async function type(value: string): Promise<void> {
        const input = el.querySelector('#delete-confirmation') as HTMLInputElement;
        input.value = value;
        input.dispatchEvent(new Event('input'));
        await fixture.whenStable();
    }

    async function goToStepTwo(): Promise<void> {
        await click('.danger-zone .danger-button');
        await click('.modal .danger-button');
    }

    beforeEach(() => {
        deleteOwnAccount.mockReset();
        clearCurrentUser.mockClear();
    });

    it('no abre el modal hasta que el usuario lo solicita', async () => {
        await render('CLIENT');
        expect(el.querySelector('.modal')).toBeNull();

        await click('.danger-zone .danger-button');

        expect(el.querySelector('.modal')).not.toBeNull();
        expect(el.textContent).toContain('Paso 1 de 2');
        expect(el.textContent).toContain('pedidos se conservarán');
        expect(deleteOwnAccount).not.toHaveBeenCalled();
    });

    it('avisa de que se eliminarán los productos al vendedor', async () => {
        await render('SELLER');
        await click('.danger-zone .danger-button');

        expect(el.textContent).toContain('todos tus productos');
        expect(el.textContent).toContain('pedidos en curso');
    });

    it('pide una segunda confirmación y solo habilita el botón al escribir ELIMINAR', async () => {
        await render('CLIENT');
        await goToStepTwo();

        const confirm = el.querySelector('.modal .danger-button') as HTMLButtonElement;
        expect(el.textContent).toContain('Paso 2 de 2');
        expect(confirm.disabled).toBe(true);

        await type('eliminar');
        expect(confirm.disabled).toBe(true);

        await type('ELIMINAR');
        expect(confirm.disabled).toBe(false);
        expect(deleteOwnAccount).not.toHaveBeenCalled();
    });

    it('elimina la cuenta, cierra la sesión local y redirige con mensaje de éxito', async () => {
        deleteOwnAccount.mockReturnValue(of(void 0));
        await render('CLIENT');
        await goToStepTwo();
        await type('ELIMINAR');
        await click('.modal .danger-button');

        expect(deleteOwnAccount).toHaveBeenCalledTimes(1);
        expect(clearCurrentUser).toHaveBeenCalled();
        expect(navigate).toHaveBeenCalledWith(['/'], { queryParams: { deleted: 'true' } });
    });

    it('muestra el mensaje del servidor cuando hay un conflicto (409) y mantiene el modal', async () => {
        deleteOwnAccount.mockReturnValue(
            throwError(() => new HttpErrorResponse({
                status: 409,
                error: { message: 'No puedes eliminar tu cuenta mientras tengas pedidos en curso' },
            })),
        );
        await render('SELLER');
        await goToStepTwo();
        await type('ELIMINAR');
        await click('.modal .danger-button');

        expect(el.querySelector('.modal-error')?.textContent).toContain('pedidos en curso');
        expect(el.querySelector('.modal')).not.toBeNull();
        expect(clearCurrentUser).not.toHaveBeenCalled();
    });

    it('muestra un mensaje genérico ante un error inesperado', async () => {
        deleteOwnAccount.mockReturnValue(throwError(() => new HttpErrorResponse({ status: 500 })));
        await render('CLIENT');
        await goToStepTwo();
        await type('ELIMINAR');
        await click('.modal .danger-button');

        expect(el.querySelector('.modal-error')?.textContent).toContain('No se pudo eliminar la cuenta');
    });

    it('muestra un mensaje de permisos ante un 403', async () => {
        deleteOwnAccount.mockReturnValue(throwError(() => new HttpErrorResponse({ status: 403 })));
        await render('CLIENT');
        await goToStepTwo();
        await type('ELIMINAR');
        await click('.modal .danger-button');

        expect(el.querySelector('.modal-error')?.textContent).toContain('No tienes permisos');
    });

    it('redirige al inicio cuando la sesión ha caducado (401)', async () => {
        deleteOwnAccount.mockReturnValue(throwError(() => new HttpErrorResponse({ status: 401 })));
        await render('CLIENT');
        await goToStepTwo();
        await type('ELIMINAR');
        await click('.modal .danger-button');

        expect(clearCurrentUser).toHaveBeenCalled();
        expect(navigate).toHaveBeenCalledWith(['/'], { queryParams: { expired: 'true' } });
    });

    it('permite cancelar sin eliminar nada', async () => {
        await render('CLIENT');
        await click('.danger-zone .danger-button');
        await click('.modal .secondary-button');

        expect(el.querySelector('.modal')).toBeNull();
        expect(deleteOwnAccount).not.toHaveBeenCalled();
    });

    it('un administrador no puede darse de baja a sí mismo', async () => {
        await render('ADMIN');
        const button = el.querySelector('.danger-zone .danger-button') as HTMLButtonElement;

        expect(button.disabled).toBe(true);
        expect(el.textContent).toContain('Otro administrador debe eliminar tu cuenta');

        button.click();
        await fixture.whenStable();
        expect(el.querySelector('.modal')).toBeNull();
    });
});