import { httpErrorMessage } from '../../core/utils/http-error-message';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, computed, ElementRef, HostListener, inject, input, signal } from '@angular/core';
import { Router } from '@angular/router';
import { AccountService } from '../../core/services/account.service';
import { AuthService, UserRole } from '../../core/services/auth.service';

export const DELETE_CONFIRMATION_WORD = 'ELIMINAR';

const CONSEQUENCES: Record<UserRole, string[]> = {
    CLIENT: [
        'Se eliminarán tus datos personales, tus favoritos y tus listas.',
        'Tus pedidos se conservarán de forma anónima en el histórico de la plataforma.',
        'No podrás volver a iniciar sesión con esta cuenta.',
    ],
    SELLER: [
        'Se eliminarán tus datos personales y todos tus productos del catálogo.',
        'No podrás eliminar tu cuenta mientras tengas pedidos en curso.',
        'No podrás volver a iniciar sesión con esta cuenta.',
    ],
    ADMIN: [],
};

@Component({
    selector: 'app-delete-account',
    standalone: true,
    templateUrl: './delete-account.html',
    styleUrl: './delete-account.css',
})
export class DeleteAccount {
    private readonly accountService = inject(AccountService);
    private readonly authService = inject(AuthService);
    private readonly router = inject(Router);
    private readonly host = inject<ElementRef<HTMLElement>>(ElementRef);

    readonly role = input.required<UserRole>();

    readonly confirmationWord = DELETE_CONFIRMATION_WORD;
    readonly open = signal(false);
    readonly step = signal<1 | 2>(1);
    readonly confirmText = signal('');
    readonly deleting = signal(false);
    readonly error = signal<string | null>(null);

    // Un administrador no puede darse de baja a sí mismo: debe hacerlo otro administrador.
    readonly blocked = computed(() => this.role() === 'ADMIN');
    readonly consequences = computed(() => CONSEQUENCES[this.role()]);
    readonly canConfirm = computed(
        () => this.confirmText().trim() === DELETE_CONFIRMATION_WORD && !this.deleting(),
    );

    openDialog(): void {
        if (this.blocked()) {
            return;
        }
        this.step.set(1);
        this.confirmText.set('');
        this.error.set(null);
        this.open.set(true);
        this.focusFirst();
    }

    @HostListener('document:keydown.escape')
    closeDialog(): void {
        if (this.deleting()) {
            return;
        }
        this.open.set(false);
    }

    goToConfirmation(): void {
        this.step.set(2);
        this.error.set(null);
        this.focusFirst();
    }

    onConfirmInput(event: Event): void {
        this.confirmText.set((event.target as HTMLInputElement).value);
    }

    confirmDeletion(): void {
        if (!this.canConfirm()) {
            return;
        }
        this.deleting.set(true);
        this.error.set(null);
        this.accountService.deleteOwnAccount().subscribe({
            next: () => {
                this.deleting.set(false);
                this.open.set(false);
                this.authService.clearCurrentUser();
                void this.router.navigate(['/'], { queryParams: { deleted: 'true' } });
            },
            error: (error: unknown) => {
                this.deleting.set(false);
                if (error instanceof HttpErrorResponse && error.status === 401) {
                    this.open.set(false);
                    this.authService.clearCurrentUser();
                    void this.router.navigate(['/'], { queryParams: { expired: 'true' } });
                    return;
                }
                this.error.set(this.errorMessage(error));
            },
        });
    }

    private errorMessage(error: unknown): string {
        if (error instanceof HttpErrorResponse) {
            if (error.status === 0) {
                return httpErrorMessage(error) ?? 'No se pudo eliminar la cuenta. Inténtalo de nuevo más tarde.';
            }
            if (error.status === 403) {
                return 'No tienes permisos para eliminar esta cuenta.';
            }
            if (error.status === 404) {
                return 'No hemos encontrado tu cuenta. Es posible que ya haya sido eliminada.';
            }
            if (error.status === 409) {
                const message = (error.error as { message?: string } | null)?.message;
                return message || 'No se puede eliminar la cuenta en este momento.';
            }
        }
        return 'No se pudo eliminar la cuenta. Inténtalo de nuevo más tarde.';
    }

    private focusFirst(): void {
        setTimeout(() => {
            this.host.nativeElement.querySelector<HTMLElement>('[data-autofocus]')?.focus();
        });
    }
}
