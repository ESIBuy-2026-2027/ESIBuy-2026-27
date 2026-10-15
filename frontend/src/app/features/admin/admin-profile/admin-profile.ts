import { HttpClient } from '@angular/common/http';
import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { Router } from '@angular/router';
import { Location } from '@angular/common';


interface AdminProfileResponse {
    firstName: string;
    lastName: string;
    email: string;
    phone: string | null;
    maskedNationalId: string | null;
    profileImage: string | null;
    headquarters: string | null;
    hireDate: string | null;
}

@Component({
    selector: 'app-admin-profile',
    standalone: true,
    templateUrl: './admin-profile.html',
    styleUrl: './admin-profile.css',
})
export class AdminProfile implements OnInit {
    private readonly http = inject(HttpClient);
    private readonly router = inject(Router);
    private readonly apiUrl = '/api/users/me/profile';

    private readonly location = inject(Location);

    readonly profile = signal<AdminProfileResponse | null>(null);
    readonly loading = signal(true);
    readonly profileError = signal<string | null>(null);

    readonly fullName = computed(() => {
        const profile = this.profile();

        return profile
            ? [profile.firstName, profile.lastName].filter(Boolean).join(' ')
            : '';
    });

    readonly initials = computed(() => {
        const profile = this.profile();

        if (!profile) {
            return 'A';
        }

        const first = profile.firstName?.trim().charAt(0) ?? '';
        const last = profile.lastName?.trim().charAt(0) ?? '';
        return `${first}${last}`.toUpperCase() || 'A';
    });

    readonly formattedHireDate = computed(() => {
        const date = this.profile()?.hireDate;

        if (!date) {
            return 'No disponible';
        }

        const parsedDate = new Date(date);

        if (Number.isNaN(parsedDate.getTime())) {
            return date;
        }

        return new Intl.DateTimeFormat('es-ES', {
            day: '2-digit',
            month: '2-digit',
            year: 'numeric',
        }).format(parsedDate);
    });

    ngOnInit(): void {
        this.loadProfile();
    }

    loadProfile(): void {
        this.loading.set(true);
        this.profileError.set(null);

        this.http.get<AdminProfileResponse>(this.apiUrl, {
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
                        'No se ha podido acceder al perfil. Comprueba que has iniciado sesión con una cuenta de administrador.',
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
            window.history.replaceState(null, '', '/admin/usuarios');
        }
    }
}