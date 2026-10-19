
import { HttpClient } from '@angular/common/http';
import { Component, computed, CUSTOM_ELEMENTS_SCHEMA, inject, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { Location } from '@angular/common';
import { AuthService } from '../../../core/services/auth.service';
import { DeleteAccount } from '../../../shared/delete-account/delete-account';
import { TwoFactorSettingsComponent } from '../../../shared/two-factor-settings/two-factor-settings';
import { ShippingAddresses } from '../shipping-addresses/shipping-addresses';
import { PaymentMethods } from '../payment-methods/payment-methods';

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
    birthDate: string | null;
    totpEnabled?: boolean;
}

interface UpdateClientProfileRequest {
    firstName: string;
    lastName: string;
    phone: string;
    profileImage: string;
    birthDate: string;
    nationalId?: string;
}

@Component({
    selector: 'app-client-profile',
    standalone: true,
    imports: [FormsModule, DeleteAccount, TwoFactorSettingsComponent, ShippingAddresses, PaymentMethods],
    schemas: [CUSTOM_ELEMENTS_SCHEMA],
    templateUrl: './client-profile.html',
    styleUrl: './client-profile.css',
})
export class ClientProfile implements OnInit {
    private readonly authService = inject(AuthService);
    private readonly http = inject(HttpClient);
    private readonly router = inject(Router);
    private readonly location = inject(Location);
    private readonly apiUrl = '/api/users/me/profile';

    readonly profile = signal<ClientProfileResponse | null>(null);
    readonly loading = signal(true);
    readonly profileError = signal<string | null>(null);
    readonly logoutError = signal<string | null>(null);

    readonly editing = signal(false);
    readonly saving = signal(false);
    readonly editError = signal<string | null>(null);
    readonly editSuccess = signal<string | null>(null);

    // El estado 2FA se sincroniza con el componente compartido.
    readonly twoFactorEnabled = signal(false);

    // Avatares locales predefinidos para clientes.
    readonly clientAvatars: string[] = [
        'assets/images/avatars/av_client_1.jpeg',
        'assets/images/avatars/av_client_2.jpeg',
        'assets/images/avatars/av_client_3.jpeg',
        'assets/images/avatars/av_client_4.jpeg',
        'assets/images/avatars/av_client_5.jpeg',
    ];

    readonly selectedAvatar = signal(this.clientAvatars[0]);

    editForm: UpdateClientProfileRequest = this.emptyForm();

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

        switch (type) {
            case 'STANDARD':
                return 'Estándar';
            case 'PREMIUM':
                return 'Premium';
            default:
                return type ?? '';
        }
    });

    readonly isPremium = computed(
        () => this.profile()?.customerType === 'PREMIUM',
    );

    readonly maxBirthDate = this.getMaxBirthDate();

    readonly sections: ProfileSection[] = [
        { label: 'Datos personales', active: true },
        { label: 'Pedidos', active: false },
        { label: 'Direcciones de envío', active: false },
        { label: 'Suscripción premium', active: false },
        { label: 'Seguridad', active: false },
        { label: 'Métodos de pago', active: false },
    ];

    readonly activeSection = signal('personal');

    readonly initials = computed(() => {
        const profile = this.profile();

        if (profile) {
            const first = profile.firstName?.trim().charAt(0) ?? '';
            const last = profile.lastName?.trim().charAt(0) ?? '';
            const result = `${first}${last}`.toUpperCase();

            if (result) return result;
        }

        return this.email().split('@')[0]?.slice(0, 2).toUpperCase() || 'NA';
    });

    ngOnInit(): void {
        this.loadProfile();
    }

    selectSection(section: string): void {
        const availableSections = [
            'personal',
            'security',
            'addresses',
            'payments',
        ];

        if (!availableSections.includes(section)) {
            return;
        }

        this.activeSection.set(section);
    }

    private emptyForm(): UpdateClientProfileRequest {
        return {
            firstName: '',
            lastName: '',
            phone: '',
            profileImage: this.clientAvatars[0],
            birthDate: '',
            nationalId: '',
        };
    }

    private normalizeAvatarPath(path: string | null | undefined): string {
        const normalizedPath = path?.trim().replace(/^\/+/, '');

        if (normalizedPath && this.clientAvatars.includes(normalizedPath)) {
            return normalizedPath;
        }

        return this.clientAvatars[0];
    }

    private getMaxBirthDate(): string {
        const date = new Date();
        date.setFullYear(date.getFullYear() - 18);

        const year = date.getFullYear();
        const month = String(date.getMonth() + 1).padStart(2, '0');
        const day = String(date.getDate()).padStart(2, '0');

        return `${year}-${month}-${day}`;
    }

    loadProfile(): void {
        this.loading.set(true);
        this.profileError.set(null);

        this.http.get<ClientProfileResponse>(this.apiUrl, {
            withCredentials: true,
        }).subscribe({
            next: profile => {
                this.profile.set(profile);

                const avatar = this.normalizeAvatarPath(profile.profileImage);
                this.selectedAvatar.set(avatar);

                // El backend debe devolver totpEnabled para conocer el estado real.
                this.twoFactorEnabled.set(profile.totpEnabled === true);

                this.editForm = {
                    firstName: profile.firstName ?? '',
                    lastName: profile.lastName ?? '',
                    phone: profile.phone ?? '',
                    profileImage: avatar,
                    birthDate: profile.birthDate ?? '',
                    nationalId: '',
                };

                this.loading.set(false);
            },
            error: () => {
                this.loading.set(false);
                this.profileError.set(
                    'No se han podido cargar los datos de tu perfil. Inténtalo de nuevo.',
                );
            },
        });
    }

    startEditing(): void {
        const profile = this.profile();

        if (!profile) return;

        const avatar = this.normalizeAvatarPath(profile.profileImage);

        this.editForm = {
            firstName: profile.firstName ?? '',
            lastName: profile.lastName ?? '',
            phone: profile.phone ?? '',
            profileImage: avatar,
            birthDate: profile.birthDate ?? '',
            nationalId: '',
        };

        this.selectedAvatar.set(avatar);
        this.editError.set(null);
        this.editSuccess.set(null);
        this.editing.set(true);
    }

    selectAvatar(avatar: string): void {
        if (!this.clientAvatars.includes(avatar)) return;

        this.selectedAvatar.set(avatar);
        this.editForm.profileImage = avatar;
    }

    cancelEditing(): void {
        this.editing.set(false);
        this.editError.set(null);

        const profile = this.profile();
        const avatar = this.normalizeAvatarPath(profile?.profileImage);

        this.selectedAvatar.set(avatar);
        this.editForm = this.emptyForm();
        this.editForm.profileImage = avatar;
    }

    saveProfile(): void {
        if (this.saving()) return;

        this.editError.set(null);
        this.editSuccess.set(null);

        const firstName = this.editForm.firstName.trim();
        const lastName = this.editForm.lastName.trim();
        const phone = this.editForm.phone.trim();
        const birthDate = this.editForm.birthDate;

        if (firstName.length < 2 || firstName.length > 25) {
            this.editError.set('El nombre debe tener entre 2 y 25 caracteres.');
            return;
        }

        if (lastName.length < 2 || lastName.length > 50) {
            this.editError.set('Los apellidos deben tener entre 2 y 50 caracteres.');
            return;
        }

        if (phone && !/^\d{9}$/.test(phone)) {
            this.editError.set('El teléfono debe tener exactamente 9 cifras.');
            return;
        }

        if (!birthDate) {
            this.editError.set('La fecha de nacimiento es obligatoria.');
            return;
        }

        if (birthDate > this.maxBirthDate) {
            this.editError.set('Debes tener al menos 18 años.');
            return;
        }

        const payload: UpdateClientProfileRequest = {
            firstName,
            lastName,
            phone,
            profileImage: this.selectedAvatar(),
            birthDate,
        };

        const newNationalId = this.editForm.nationalId?.trim();

        if (newNationalId) {
            payload.nationalId = newNationalId;
        }

        this.saving.set(true);

        this.http.put<ClientProfileResponse>(
            this.apiUrl,
            payload,
            { withCredentials: true },
        ).subscribe({
            next: profile => {
                const avatar = this.normalizeAvatarPath(profile.profileImage);

                this.profile.set({
                    ...profile,
                    profileImage: avatar,
                });

                this.selectedAvatar.set(avatar);
                this.saving.set(false);
                this.editing.set(false);
                this.editSuccess.set(
                    'Tus datos se han actualizado correctamente.',
                );

                this.editForm = this.emptyForm();
                this.editForm.profileImage = avatar;
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

                this.editError.set(
                    validationErrors
                    ?? error.error?.detail
                    ?? error.error?.message
                    ?? (
                        error.status === 400
                            ? 'Revisa los datos introducidos. Comprueba que tienes al menos 18 años y que el teléfono tiene 9 cifras.'
                            : 'No se han podido guardar los cambios. Inténtalo de nuevo.'
                    ),
                );
            },
        });
    }

    onTwoFactorEnabledChange(enabled: boolean): void {
        this.twoFactorEnabled.set(enabled);

        const current = this.profile();
        if (current) {
            this.profile.set({ ...current, totpEnabled: enabled });
        }
    }

    logout(): void {
        this.logoutError.set(null);

        this.authService.logout().subscribe({
            next: () => void this.router.navigate(['/']),
            error: () => this.logoutError.set(
                'No se pudo cerrar la sesión. Inténtalo de nuevo.',
            ),
        });
    }

    goBack(): void {
        if (window.history.length > 1) {
            this.location.back();
        } else {
            window.history.replaceState(null, '', '/client/dashboard');
        }
    }
}
