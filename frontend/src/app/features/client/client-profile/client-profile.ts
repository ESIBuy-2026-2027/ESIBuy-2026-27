import { HttpClient } from '@angular/common/http';
import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';

export interface ProfileSection {
    label: string;
    active: boolean;
}

interface ClientProfileResponse {
    firstName: string;
    lastName: string;
    email: string;
    phone: string | null;
    maskedNationalId: string | null;
    profileImage: string | null;
    customerType: string;
}

@Component({
    selector: 'app-client-profile',
    standalone: true,
    templateUrl: './client-profile.html',
    styleUrl: './client-profile.css',
})
export class ClientProfile implements OnInit {
    private readonly authService = inject(AuthService);
    private readonly http = inject(HttpClient);
    private readonly router = inject(Router);

    private readonly apiUrl = '/api/users/me/profile';

    readonly profile = signal<ClientProfileResponse | null>(null);
    readonly loading = signal(true);
    readonly profileError = signal<string | null>(null);
    readonly logoutError = signal<string | null>(null);

    readonly email = computed(
        () => this.profile()?.email ?? this.authService.currentUser()?.email ?? '',
    );

    readonly fullName = computed(() => {
        const profile = this.profile();

        return profile
            ? [profile.firstName, profile.lastName].filter(Boolean).join(' ')
            : '';
    });

    readonly customerTypeLabel = computed(() => {
        const type = this.profile()?.customerType;

        if (!type) {
            return '';
        }

        return type === 'STANDARD' ? 'Estándar' : type;
    });

    readonly isPremium = computed(
        () => this.profile()?.customerType === 'PREMIUM',
    );

    readonly sections: ProfileSection[] = [
        { label: 'Datos personales', active: true },
        { label: 'Mis pedidos', active: false },
        { label: 'Direcciones de envío', active: false },
        { label: 'Suscripción premium', active: false },
        { label: 'Seguridad', active: false },
        { label: 'Métodos de pago', active: false },
    ];

    readonly initials = computed(() => {
        const profile = this.profile();

        if (profile) {
            const first = profile.firstName?.trim().charAt(0) ?? '';
            const last = profile.lastName?.trim().charAt(0) ?? '';
            const initials = `${first}${last}`.toUpperCase();

            if (initials) {
                return initials;
            }
        }

        return this.email().split('@')[0]?.slice(0, 2).toUpperCase() || 'NA';
    });

    ngOnInit(): void {
        this.loadProfile();
    }

    loadProfile(): void {
        this.loading.set(true);
        this.profileError.set(null);

        this.http.get<ClientProfileResponse>(this.apiUrl, {
            withCredentials: true,
        }).subscribe({
            next: (profile) => {
                this.profile.set(profile);
                this.loading.set(false);
            },
            error: (error: { status?: number }) => {
                this.loading.set(false);

                if (error.status === 401 || error.status === 403) {
                    this.profileError.set(
                        'No se ha podido acceder al perfil. Comprueba que has iniciado sesión con una cuenta de cliente.',
                    );
                    return;
                }

                this.profileError.set(
                    'No se han podido cargar los datos del perfil. Inténtalo de nuevo.',
                );
            },
        });
    }

    logout(): void {
        this.logoutError.set(null);

        this.authService.logout().subscribe({
            next: () => {
                void this.router.navigate(['/']);
            },
            error: () => {
                this.logoutError.set(
                    'No se pudo cerrar la sesión. Inténtalo de nuevo.',
                );
            },
        });
    }

}