
import { HttpClient } from '@angular/common/http';
import { Component, computed, CUSTOM_ELEMENTS_SCHEMA, inject, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { Location } from '@angular/common';
import { AuthService } from '../../../core/services/auth.service';
import { DeleteAccount } from '../../../shared/delete-account/delete-account';
import QRCode from 'qrcode';
import { TwoFactorService, TwoFactorSetupResponse, } from '../../../core/services/two-factor.service';
import { TwoFactorSettingsComponent } from '../../../shared/two-factor-settings/two-factor-settings';
import { ShippingAddresses } from '../shipping-addresses/shipping-addresses';

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
    imports: [FormsModule, DeleteAccount, TwoFactorSettingsComponent, ShippingAddresses],
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
    private readonly twoFactorService = inject(TwoFactorService);
    readonly qrImage = signal<string | null>(null);
    readonly qrGenerating = signal(false);
    readonly copiedSecret = signal(false);

    readonly profile = signal<ClientProfileResponse | null>(null);
    readonly loading = signal(true);
    readonly profileError = signal<string | null>(null);
    readonly logoutError = signal<string | null>(null);

    readonly editing = signal(false);
    readonly saving = signal(false);
    readonly editError = signal<string | null>(null);
    readonly editSuccess = signal<string | null>(null);

    // Estado de autenticación en dos factores.
    readonly twoFactorEnabled = signal(false);
    readonly twoFactorSetup = signal<TwoFactorSetupResponse | null>(null);
    readonly twoFactorCode = signal('');
    readonly recoveryCode = signal('');
    readonly backupCodes = signal<string[]>([]);
    readonly twoFactorBusy = signal(false);
    readonly twoFactorError = signal<string | null>(null);
    readonly twoFactorSuccess = signal<string | null>(null);

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
        { label: 'Mis pedidos', active: false },
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
        if (section !== 'personal' && section !== 'security' && section != 'addresses') {
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

    // ---------------- 2FA ----------------

    async startTwoFactorSetup(): Promise<void> {
        if (this.twoFactorBusy() || this.twoFactorEnabled()) return;

        this.clearTwoFactorMessages();
        this.twoFactorSetup.set(null);
        this.qrImage.set(null);
        this.copiedSecret.set(false);
        this.backupCodes.set([]);
        this.twoFactorBusy.set(true);
        this.qrGenerating.set(true);

        this.twoFactorService.setup().subscribe({
            next: result => {
                // Conserva aquí el código actual que genera el QR.
                this.twoFactorSetup.set(result);

                void QRCode.toDataURL(result.qrUri, {
                    errorCorrectionLevel: 'M',
                    margin: 2,
                    width: 240,
                })
                    .then(dataUrl => {
                        this.qrImage.set(dataUrl);
                    })
                    .catch(() => {
                        this.qrImage.set(null);
                        this.twoFactorError.set(
                            'No se ha podido generar el QR. Puedes configurar la aplicación introduciendo manualmente el secreto.',
                        );
                    })
                    .finally(() => {
                        this.qrGenerating.set(false);
                        this.twoFactorBusy.set(false);

                        if (!this.twoFactorError()) {
                            this.twoFactorSuccess.set(
                                'Escanea el QR con tu aplicación autenticadora o introduce manualmente el secreto. Después, confirma con el código de seis cifras.',
                            );
                        }
                    });
            },
            error: error => {
                this.qrGenerating.set(false);
                this.twoFactorBusy.set(false);
                this.twoFactorError.set(
                    this.twoFactorService.getErrorMessage(
                        error,
                        'No se ha podido iniciar la configuración de la autenticación en dos factores.',
                    ),
                );
            },
        });
    }

    onTwoFactorEnabledChange(enabled: boolean): void {
        this.refreshTwoFactorStateInProfile(enabled);
    }

    async copyTwoFactorSecret(): Promise<void> {
        const secret = this.twoFactorSetup()?.secret;

        if (!secret) return;

        this.clearTwoFactorMessages();

        try {
            await navigator.clipboard.writeText(secret);
            this.copiedSecret.set(true);
            this.twoFactorSuccess.set('Secreto copiado al portapapeles.');
        } catch {
            this.copiedSecret.set(false);
            this.twoFactorError.set(
                'No se ha podido copiar automáticamente. Selecciona y copia el secreto manualmente.',
            );
        }
    }

    downloadBackupCodes(): void {
        const codes = this.backupCodes();

        if (!codes.length) return;

        const content = [
            'ESIBUY - Códigos de recuperación de autenticación en dos factores',
            'Guarda este archivo en un lugar seguro.',
            'Cada código debe tratarse como información confidencial.',
            '',
            ...codes,
            '',
        ].join('\n');

        const blob = new Blob([content], {
            type: 'text/plain;charset=utf-8',
        });

        const url = URL.createObjectURL(blob);
        const link = document.createElement('a');

        link.href = url;
        link.download = 'esibuy-codigos-recuperacion.txt';
        document.body.appendChild(link);
        link.click();
        link.remove();
        URL.revokeObjectURL(url);
    }

    enableTwoFactor(): void {
        const code = this.twoFactorCode().trim();

        if (this.twoFactorBusy()) return;

        if (!/^\d{6}$/.test(code)) {
            this.twoFactorError.set(
                'Introduce el código de seis cifras de tu aplicación autenticadora.',
            );
            return;
        }

        this.clearTwoFactorMessages();
        this.twoFactorBusy.set(true);

        this.twoFactorService.enable(code).subscribe({
            next: result => {
                this.twoFactorEnabled.set(true);
                this.twoFactorSetup.set(null);
                this.qrImage.set(null);
                this.copiedSecret.set(false);
                this.twoFactorCode.set('');
                this.backupCodes.set(result.backupCodes ?? []);
                this.twoFactorBusy.set(false);
                this.twoFactorSuccess.set(
                    result.message
                    ?? 'La autenticación en dos factores se ha activado correctamente.',
                );
                this.refreshTwoFactorStateInProfile(true);
            },
            error: error => {
                this.twoFactorBusy.set(false);
                this.twoFactorError.set(
                    this.twoFactorService.getErrorMessage(
                        error,
                        'No se ha podido activar la autenticación en dos factores. Comprueba el código e inténtalo de nuevo.',
                    ),
                );
            },
        });
    }

    regenerateBackupCodes(): void {
        const code = this.recoveryCode().trim();

        if (this.twoFactorBusy()) return;

        if (!/^\d{6}$/.test(code)) {
            this.twoFactorError.set(
                'Introduce un código actual de seis cifras para regenerar los códigos de recuperación.',
            );
            return;
        }

        this.clearTwoFactorMessages();
        this.twoFactorBusy.set(true);
        this.backupCodes.set([]);

        this.twoFactorService.regenerateBackupCodes(code).subscribe({
            next: result => {
                this.backupCodes.set(result.backupCodes ?? []);
                this.recoveryCode.set('');
                this.twoFactorBusy.set(false);
                this.twoFactorSuccess.set(
                    result.message
                    ?? 'Se han generado nuevos códigos de recuperación. Los anteriores dejan de ser válidos.',
                );
            },
            error: error => {
                this.twoFactorBusy.set(false);
                this.twoFactorError.set(
                    this.twoFactorService.getErrorMessage(
                        error,
                        'No se han podido regenerar los códigos. Comprueba el código actual e inténtalo de nuevo.',
                    ),
                );
            },
        });
    }

    disableTwoFactor(): void {
        const code = this.twoFactorCode().trim();

        if (this.twoFactorBusy()) return;

        if (!/^\d{6}$/.test(code)) {
            this.twoFactorError.set(
                'Introduce el código de seis cifras de tu aplicación autenticadora para desactivar 2FA.',
            );
            return;
        }

        this.clearTwoFactorMessages();
        this.twoFactorBusy.set(true);

        this.twoFactorService.disable(code).subscribe({
            next: result => {
                this.twoFactorEnabled.set(false);
                this.twoFactorSetup.set(null);
                this.qrImage.set(null);
                this.copiedSecret.set(false);
                this.twoFactorCode.set('');
                this.recoveryCode.set('');
                this.backupCodes.set([]);
                this.twoFactorBusy.set(false);
                this.twoFactorSuccess.set(
                    result.message
                    ?? 'La autenticación en dos factores se ha desactivado.',
                );
                this.refreshTwoFactorStateInProfile(false);
            },
            error: error => {
                this.twoFactorBusy.set(false);
                this.twoFactorError.set(
                    this.twoFactorService.getErrorMessage(
                        error,
                        'No se ha podido desactivar la autenticación en dos factores. Comprueba el código e inténtalo de nuevo.',
                    ),
                );
            },
        });
    }

    clearTwoFactorMessages(): void {
        this.twoFactorError.set(null);
        this.twoFactorSuccess.set(null);
    }

    private refreshTwoFactorStateInProfile(enabled: boolean): void {
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
