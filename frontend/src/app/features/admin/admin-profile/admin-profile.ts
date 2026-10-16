
import { HttpClient } from '@angular/common/http';
import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { Location } from '@angular/common';
import { AuthService } from '../../../core/services/auth.service';
import { DeleteAccount } from '../../../shared/delete-account/delete-account';

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

interface UpdateAdminProfileRequest {
    firstName: string;
    lastName: string;
    headquarters: string;
    profileImage: string;
    nationalId?: string;
}

@Component({
    selector: 'app-admin-profile',
    standalone: true,
    imports: [FormsModule, DeleteAccount],
    templateUrl: './admin-profile.html',
    styleUrl: './admin-profile.css',
})
export class AdminProfile implements OnInit {
    private readonly authService = inject(AuthService);
    private readonly http = inject(HttpClient);
    private readonly router = inject(Router);
    private readonly location = inject(Location);
    private readonly apiUrl = '/api/users/me/profile';

    // El panel de administración utiliza avatares de cliente
    // para los perfiles que no tienen una colección propia.
    readonly adminAvatars: string[] = [
        'assets/images/avatars/av_client_1.jpeg',
        'assets/images/avatars/av_client_2.jpeg',
        'assets/images/avatars/av_client_3.jpeg',
        'assets/images/avatars/av_client_4.jpeg',
        'assets/images/avatars/av_client_5.jpeg',
        'assets/images/avatars/av_seller_1.jpeg',
        'assets/images/avatars/av_seller_2.jpeg',
        'assets/images/avatars/av_seller_3.jpeg',
        'assets/images/avatars/av_seller_4.jpeg',
        'assets/images/avatars/av_seller_5.jpeg',

    ];

    readonly selectedAvatar = signal(this.adminAvatars[0]);

    readonly profile = signal<AdminProfileResponse | null>(null);
    readonly loading = signal(true);
    readonly profileError = signal<string | null>(null);
    readonly logoutError = signal<string | null>(null);

    readonly editing = signal(false);
    readonly saving = signal(false);
    readonly editError = signal<string | null>(null);
    readonly editSuccess = signal<string | null>(null);

    editForm: UpdateAdminProfileRequest = this.emptyForm();

    readonly fullName = computed(() => {
        const profile = this.profile();

        return profile
            ? [profile.firstName, profile.lastName].filter(Boolean).join(' ')
            : '';
    });

    readonly initials = computed(() => {
        const profile = this.profile();

        if (!profile) return 'A';

        const first = profile.firstName?.trim().charAt(0) ?? '';
        const last = profile.lastName?.trim().charAt(0) ?? '';

        return `${first}${last}`.toUpperCase() || 'A';
    });

    readonly displayAvatar = computed(() => {
        if (this.editing()) {
            return this.selectedAvatar();
        }

        return this.getValidAvatar(this.profile()?.profileImage);
    });

    readonly formattedHireDate = computed(() => {
        const date = this.profile()?.hireDate;

        if (!date) return 'No disponible';

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

    private emptyForm(): UpdateAdminProfileRequest {
        return {
            firstName: '',
            lastName: '',
            headquarters: '',
            profileImage: this.adminAvatars[0],
            nationalId: '',
        };
    }

    private normalizeAvatar(path: string | null | undefined): string {
        return path?.replace(/^\/+/, '') ?? '';
    }

    private getValidAvatar(path: string | null | undefined): string {
        const normalized = this.normalizeAvatar(path);

        return this.adminAvatars.includes(normalized)
            ? normalized
            : this.adminAvatars[0];
    }

    selectAvatar(avatar: string): void {
        if (!this.editing() || this.saving()) return;

        if (this.adminAvatars.includes(avatar)) {
            this.selectedAvatar.set(avatar);
            this.editForm.profileImage = avatar;
        }
    }

    loadProfile(): void {
        this.loading.set(true);
        this.profileError.set(null);

        this.http.get<AdminProfileResponse>(this.apiUrl, {
            withCredentials: true,
        }).subscribe({
            next: profile => {
                this.profile.set(profile);

                const avatar = this.getValidAvatar(profile.profileImage);
                this.selectedAvatar.set(avatar);

                this.editForm = {
                    firstName: profile.firstName ?? '',
                    lastName: profile.lastName ?? '',
                    headquarters: profile.headquarters ?? '',
                    profileImage: avatar,
                    nationalId: '',
                };

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

    startEditing(): void {
        const profile = this.profile();
        if (!profile) return;

        const avatar = this.getValidAvatar(profile.profileImage);

        this.selectedAvatar.set(avatar);
        this.editForm = {
            firstName: profile.firstName ?? '',
            lastName: profile.lastName ?? '',
            headquarters: profile.headquarters ?? '',
            profileImage: avatar,
            // Nunca rellenar este campo con el DNI enmascarado.
            nationalId: '',
        };

        this.editError.set(null);
        this.editSuccess.set(null);
        this.editing.set(true);
    }

    cancelEditing(): void {
        const profile = this.profile();

        this.selectedAvatar.set(this.getValidAvatar(profile?.profileImage));
        this.editing.set(false);
        this.editError.set(null);
        this.editForm = this.emptyForm();

        if (profile) {
            this.editForm = {
                firstName: profile.firstName ?? '',
                lastName: profile.lastName ?? '',
                headquarters: profile.headquarters ?? '',
                profileImage: this.getValidAvatar(profile.profileImage),
                nationalId: '',
            };
        }
    }

    saveProfile(): void {
        if (this.saving()) return;

        this.editError.set(null);
        this.editSuccess.set(null);

        const firstName = this.editForm.firstName.trim();
        const lastName = this.editForm.lastName.trim();
        const headquarters = this.editForm.headquarters.trim();

        if (firstName.length < 2 || firstName.length > 25) {
            this.editError.set('El nombre debe tener entre 2 y 25 caracteres.');
            return;
        }

        if (lastName.length < 2 || lastName.length > 50) {
            this.editError.set('Los apellidos deben tener entre 2 y 50 caracteres.');
            return;
        }

        if (headquarters.length > 100) {
            this.editError.set('La sede no puede superar los 100 caracteres.');
            return;
        }

        if (!this.adminAvatars.includes(this.selectedAvatar())) {
            this.editError.set('Selecciona una imagen de perfil válida.');
            return;
        }

        const payload: UpdateAdminProfileRequest = {
            firstName,
            lastName,
            headquarters,
            profileImage: this.selectedAvatar(),
        };

        const nationalId = this.editForm.nationalId?.trim();

        if (nationalId) {
            payload.nationalId = nationalId;
        }

        this.saving.set(true);

        this.http.put<AdminProfileResponse>(this.apiUrl, payload, {
            withCredentials: true,
        }).subscribe({
            next: response => {
                const updatedProfile: AdminProfileResponse = {
                    ...response,
                    profileImage: response.profileImage || payload.profileImage,
                    headquarters: response.headquarters ?? payload.headquarters,
                };

                this.profile.set(updatedProfile);
                this.selectedAvatar.set(
                    this.getValidAvatar(updatedProfile.profileImage),
                );

                this.saving.set(false);
                this.editing.set(false);
                this.editSuccess.set(
                    'Tus datos se han actualizado correctamente.',
                );
                this.editForm = this.emptyForm();
            },
            error: (error: {
                status?: number;
                error?: {
                    message?: string;
                    detail?: string;
                    errors?: Record<string, string>;
                };
            }) => {
                this.saving.set(false);

                const validationErrors = error.error?.errors
                    ? Object.values(error.error.errors).join('. ')
                    : null;
                let defaultErrorMessage =
                    'No se han podido guardar los cambios. Inténtalo de nuevo.';

                if (error.status === 409) {
                    defaultErrorMessage =
                        'El documento de identidad ya está registrado.';
                } else if (error.status === 400) {
                    defaultErrorMessage = 'Revisa los datos introducidos.';
                }

                this.editError.set(
                    validationErrors
                    ?? error.error?.detail
                    ?? error.error?.message
                    ?? defaultErrorMessage,
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
