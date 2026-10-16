import { HttpClient } from '@angular/common/http';
import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { Location } from '@angular/common';
import { AuthService } from '../../../core/services/auth.service';
import { DeleteAccount } from '../../../core/services/delete-account/delete-account';
import { Router } from '@angular/router';

interface SellerProfileResponse {
    firstName: string;
    lastName: string;
    email: string;
    phone: string | null;
    maskedNationalId: string | null;
    profileImage: string | null;
    businessName: string | null;
    defaultCategory: string | null;
}

@Component({
    selector: 'app-seller-profile',
    standalone: true,
    imports: [DeleteAccount],
    templateUrl: './seller-profile.html',
    styleUrl: './seller-profile.css',
})
export class SellerProfile implements OnInit {
    private readonly authService = inject(AuthService);
    private readonly router = inject(Router);

    private readonly http = inject(HttpClient);
    private readonly apiUrl = '/api/users/me/profile';

    private readonly location = inject(Location);

    readonly profile = signal<SellerProfileResponse | null>(null);
    readonly loading = signal(true);
    readonly profileError = signal<string | null>(null);
    readonly logoutError = signal<string | null>(null);


    readonly fullName = computed(() => {
        const profile = this.profile();

        return profile
            ? [profile.firstName, profile.lastName]
                .filter(Boolean)
                .join(' ')
            : '';
    });

    readonly initials = computed(() => {
        const profile = this.profile();

        if (!profile) {
            return 'V';
        }

        const first = profile.firstName?.trim().charAt(0) ?? '';
        const last = profile.lastName?.trim().charAt(0) ?? '';
        const result = `${first}${last}`.toUpperCase();

        return result || 'V';
    });

    ngOnInit(): void {
        this.loadProfile();
    }

    loadProfile(): void {
        this.loading.set(true);
        this.profileError.set(null);

        this.http.get<SellerProfileResponse>(this.apiUrl, {
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
                        'No se ha podido acceder al perfil. Comprueba que has iniciado sesión con una cuenta de vendedor.',
                    );
                    return;
                }

                this.profileError.set(
                    'No se han podido cargar los datos del perfil. Inténtalo de nuevo.',
                );
            },
        });
    }

    goBack(): void {
        if (window.history.length > 1) {
            this.location.back();
        } else {
            window.history.replaceState(null, '', '/seller/catalog');
        }
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