import { HttpClient } from '@angular/common/http';
import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Location } from '@angular/common';
import { Router } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';
import { DeleteAccount } from '../../../shared/delete-account/delete-account';
import { Category, ProductsService } from '../../products/products.service';
import { TwoFactorSettingsComponent } from '../../../shared/two-factor-settings/two-factor-settings';

interface SellerProfileResponse {
    firstName: string;
    lastName: string;
    email: string;
    phone: string | null;
    maskedNationalId: string | null;
    profileImage: string | null;
    businessName: string | null;
    defaultCategory: string | null;
    totpEnabled: boolean;
}

interface UpdateSellerProfileRequest {
    firstName: string;
    lastName: string;
    phone: string;
    profileImage: string;
    businessName: string;
    defaultCategory: string;
    nationalId?: string;
}

@Component({
    selector: 'app-seller-profile',
    standalone: true,
    imports: [FormsModule, DeleteAccount, TwoFactorSettingsComponent],
    templateUrl: './seller-profile.html',
    styleUrl: './seller-profile.css',
})
export class SellerProfile implements OnInit {
    private readonly authService = inject(AuthService);
    private readonly router = inject(Router);
    private readonly http = inject(HttpClient);
    private readonly location = inject(Location);
    private readonly productsService = inject(ProductsService);
    private readonly apiUrl = '/api/users/me/profile';

    readonly sellerAvatars: string[] = [
        'assets/images/avatars/av_seller_1.jpeg',
        'assets/images/avatars/av_seller_2.jpeg',
        'assets/images/avatars/av_seller_3.jpeg',
        'assets/images/avatars/av_seller_4.jpeg',
        'assets/images/avatars/av_seller_5.jpeg',
    ];

    readonly selectedAvatar = signal(this.sellerAvatars[0]);

    readonly categories = signal<Category[]>([]);
    readonly categoriesLoading = signal(true);
    readonly categoriesError = signal(false);

    readonly profile = signal<SellerProfileResponse | null>(null);
    readonly loading = signal(true);
    readonly profileError = signal<string | null>(null);
    readonly logoutError = signal<string | null>(null);

    readonly editing = signal(false);
    readonly saving = signal(false);
    readonly editError = signal<string | null>(null);
    readonly editSuccess = signal<string | null>(null);

    editForm: UpdateSellerProfileRequest = this.emptyForm();

    readonly fullName = computed(() => {
        const profile = this.profile();
        return profile
            ? [profile.firstName, profile.lastName].filter(Boolean).join(' ')
            : '';
    });

    readonly initials = computed(() => {
        const profile = this.profile();
        if (!profile) return 'V';

        const first = profile.firstName?.trim().charAt(0) ?? '';
        const last = profile.lastName?.trim().charAt(0) ?? '';

        return `${first}${last}`.toUpperCase() || 'V';
    });

    readonly sellerEmail = computed(
        () => this.profile()?.email ?? this.authService.currentUser()?.email ?? '',
    );

    // Solo permite imágenes predefinidas de vendedor.
    readonly avatarImage = computed(() => {
        const path = this.normalizeAvatarPath(this.profile()?.profileImage);
        return this.sellerAvatars.includes(path) ? path : '';
    });

    // Durante la edición se muestra el avatar seleccionado.
    // En consulta se muestra el guardado, o el primero si no es válido.
    readonly displayAvatar = computed(() => {
        if (this.editing()) {
            return this.selectedAvatar();
        }

        return this.avatarImage() || this.sellerAvatars[0];
    });

    ngOnInit(): void {
        this.loadProfile();
        this.loadCategories();
    }

    readonly activeSection = signal<'personal' | 'security'>('personal');

    readonly twoFactorEnabled = signal(false);

    selectSection(section: 'personal' | 'security'): void {
        this.activeSection.set(section);
    }

    onTwoFactorEnabledChange(enabled: boolean): void {
        this.twoFactorEnabled.set(enabled);

        const currentProfile = this.profile();

        if (currentProfile) {
            this.profile.set({
                ...currentProfile,
                totpEnabled: enabled,
            });
        }
    }

    private emptyForm(): UpdateSellerProfileRequest {
        return {
            firstName: '',
            lastName: '',
            phone: '',
            profileImage: this.sellerAvatars[0],
            businessName: '',
            defaultCategory: '',
            nationalId: '',
        };
    }

    private normalizeAvatarPath(path: string | null | undefined): string {
        return path?.replace(/^\/+/, '') ?? '';
    }

    selectAvatar(avatar: string): void {
        if (!this.editing() || this.saving()) return;

        if (this.sellerAvatars.includes(avatar)) {
            this.selectedAvatar.set(avatar);
            this.editForm.profileImage = avatar;
        }
    }

    loadProfile(): void {
        this.loading.set(true);
        this.profileError.set(null);

        this.http.get<SellerProfileResponse>(this.apiUrl, {
            withCredentials: true,
        }).subscribe({
            next: profile => {
                this.profile.set(profile);

                // Igual que en el perfil del cliente.
                this.twoFactorEnabled.set(profile.totpEnabled === true);

                const savedAvatar = this.avatarImage();
                const avatar = savedAvatar || this.sellerAvatars[0];

                this.selectedAvatar.set(avatar);

                this.editForm = {
                    firstName: profile.firstName ?? '',
                    lastName: profile.lastName ?? '',
                    phone: profile.phone ?? '',
                    profileImage: avatar,
                    businessName: profile.businessName ?? '',
                    defaultCategory: profile.defaultCategory ?? '',
                    nationalId: '',
                };

                this.loading.set(false);
            },
            error: () => {
                this.loading.set(false);
                this.profileError.set(
                    'No se ha podido cargar el perfil. Inténtalo de nuevo.',
                );
            },
        });
    }

    startEditing(): void {
        const profile = this.profile();
        if (!profile) return;

        const avatar = this.avatarImage() || this.sellerAvatars[0];

        this.selectedAvatar.set(avatar);

        this.editForm = {
            firstName: profile.firstName ?? '',
            lastName: profile.lastName ?? '',
            phone: profile.phone ?? '',
            profileImage: avatar,
            businessName: profile.businessName ?? '',
            defaultCategory: profile.defaultCategory ?? '',
            nationalId: '',
        };

        this.editError.set(null);
        this.editSuccess.set(null);
        this.editing.set(true);
    }

    cancelEditing(): void {
        const profile = this.profile();
        const avatar = this.avatarImage() || this.sellerAvatars[0];

        this.selectedAvatar.set(avatar);
        this.editing.set(false);
        this.editError.set(null);
        this.editForm = this.emptyForm();

        // No se pierden los datos del perfil al cancelar.
        if (profile) {
            this.editForm = {
                firstName: profile.firstName ?? '',
                lastName: profile.lastName ?? '',
                phone: profile.phone ?? '',
                profileImage: avatar,
                businessName: profile.businessName ?? '',
                defaultCategory: profile.defaultCategory ?? '',
                nationalId: '',
            };
        }
    }

    saveProfile(): void {
        if (this.saving()) return;

        this.editError.set(null);
        this.editSuccess.set(null);

        if (!this.selectedAvatar() || !this.sellerAvatars.includes(this.selectedAvatar())) {
            this.editError.set('Selecciona una imagen de perfil válida.');
            return;
        }

        const payload: UpdateSellerProfileRequest = {
            firstName: this.editForm.firstName.trim(),
            lastName: this.editForm.lastName.trim(),
            phone: this.editForm.phone.trim(),
            profileImage: this.selectedAvatar(),
            businessName: this.editForm.businessName.trim(),
            defaultCategory: this.editForm.defaultCategory.trim(),
        };

        const newNationalId = this.editForm.nationalId?.trim();
        if (newNationalId) {
            payload.nationalId = newNationalId;
        }

        this.saving.set(true);

        this.http.put<SellerProfileResponse>(this.apiUrl, payload, {
            withCredentials: true,
        }).subscribe({
            next: response => {
                // Si el backend no devuelve profileImage, conservamos
                // el avatar que acabamos de guardar.
                const profile: SellerProfileResponse = {
                    ...response,
                    profileImage: response.profileImage || payload.profileImage,
                };

                this.profile.set(profile);
                this.selectedAvatar.set(
                    this.normalizeAvatarPath(profile.profileImage)
                        ? this.normalizeAvatarPath(profile.profileImage)
                        : payload.profileImage,
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

                let fallbackError = 'No se han podido guardar los cambios. Inténtalo de nuevo.';
                if (error.status === 409) {
                    fallbackError = 'Ya existe un vendedor con ese nombre comercial.';
                } else if (error.status === 400) {
                    fallbackError = 'Revisa los campos introducidos. Comprueba también que el teléfono tenga 9 dígitos.';
                }

                this.editError.set(
                    validationErrors
                    ?? error.error?.detail
                    ?? error.error?.message
                    ?? fallbackError,
                );
            },
        });
    }

    private loadCategories(): void {
        this.categoriesLoading.set(true);
        this.categoriesError.set(false);

        this.productsService.getCategories().subscribe({
            next: categories => {
                this.categories.set(categories);
                this.categoriesLoading.set(false);

                const currentCategory = this.editForm.defaultCategory;

                if (
                    currentCategory &&
                    categories.some(category => category.name === currentCategory)
                ) {
                    return;
                }

                if (categories.length > 0) {
                    this.editForm.defaultCategory = categories[0].name;
                }
            },
            error: () => {
                this.categoriesLoading.set(false);
                this.categoriesError.set(true);
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
            next: () => void this.router.navigate(['/']),
            error: () => this.logoutError.set(
                'No se pudo cerrar la sesión. Inténtalo de nuevo.',
            ),
        });
    }
}