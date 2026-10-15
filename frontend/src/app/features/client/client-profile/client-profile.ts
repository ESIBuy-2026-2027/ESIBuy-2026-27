import { Component, computed, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';

export interface ProfileSection {
    label: string;
    active: boolean;
}

@Component({
    selector: 'app-client-profile',
    standalone: true,
    templateUrl: './client-profile.html',
    styleUrl: './client-profile.css',
})
export class ClientProfile {
    private readonly authService = inject(AuthService);
    private readonly router = inject(Router);

    // Maquetación: de momento solo disponemos del email de la sesión.
    // El resto de datos llegará con el endpoint GET /api/users/me/profile.
    readonly email = computed(() => this.authService.currentUser()?.email ?? '');
    readonly logoutError = signal<string | null>(null);

    readonly isPremium = false;

    readonly sections: ProfileSection[] = [
        { label: 'Datos personales', active: true },
        { label: 'Mis pedidos', active: false },
        { label: 'Direcciones de envío', active: false },
        { label: 'Suscripción premium', active: false },
        { label: 'Seguridad', active: false },
        { label: 'Métodos de pago', active: false },
    ];

    readonly initials = computed(() => {
        const local = this.email().split('@')[0] ?? '';
        return local.slice(0, 2).toUpperCase() || 'NA';
    });

    logout(): void {
        this.logoutError.set(null);
        this.authService.logout().subscribe({
            next: () => {
                void this.router.navigate(['/']);
            },
            error: () => this.logoutError.set('No se pudo cerrar la sesión. Inténtalo de nuevo.'),
        });
    }
}