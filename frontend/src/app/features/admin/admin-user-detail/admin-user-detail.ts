import { httpErrorMessage } from '../../../core/utils/http-error-message';
import { Component, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterLink, RouterLinkActive } from '@angular/router';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { AuthService, UpdateUserRequest, UserResponse } from '../../../core/services/auth.service';
import { isAdultValidator } from '../../identity/register/register';
import { Category, ProductsService } from '../../../core/services/products.service';

@Component({
  selector: 'app-admin-user-detail',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink, RouterLinkActive],
  templateUrl: './admin-user-detail.html',
  styleUrl: './admin-user-detail.css'
})
export class AdminUserDetail implements OnInit {
  sidebarCollapsed = signal(false);

  toggleSidebar(): void {
    this.sidebarCollapsed.update((collapsed) => !collapsed);
  }
  private readonly formBuilder = inject(FormBuilder);
  readonly cargando = signal(true);
  readonly guardando = signal(false);
  readonly errorMessage = signal('');
  readonly successMessage = signal('');
  readonly credentialMask = '••••••••••••';
  readonly categories: string[] = [];
  readonly categoriesLoadError = signal(false);
  readonly categorySelectionUnavailable = signal(false);

  readonly form = this.formBuilder.nonNullable.group({
    firstName: ['', [Validators.required, Validators.minLength(2), Validators.maxLength(25),
    Validators.pattern(/^[\p{L}\p{M}][\p{L}\p{M} '-]*$/u)]],
    lastName: ['', [Validators.required, Validators.minLength(2), Validators.maxLength(50),
    Validators.pattern(/^[\p{L}\p{M}][\p{L}\p{M} '-]*$/u)]],
    nationalId: ['', [Validators.required, Validators.maxLength(20)]],
    birthDate: ['', [isAdultValidator]],
    defaultCategory: ['Electrónica'],
    businessName: ['', [Validators.minLength(2), Validators.maxLength(100)]],
    phone: ['', [Validators.pattern(/^\d{9}$/)]],
    headquarters: ['', [Validators.maxLength(100)]]
  });

  private userId = '';
  private email = '';
  private role = '';

  clientAvatars: string[] = [
    'assets/images/avatars/av_client_1.jpeg',
    'assets/images/avatars/av_client_2.jpeg',
    'assets/images/avatars/av_client_3.jpeg',
    'assets/images/avatars/av_client_4.jpeg',
    'assets/images/avatars/av_client_5.jpeg'
  ];

  sellerAvatars: string[] = [
    'assets/images/avatars/av_seller_1.jpeg',
    'assets/images/avatars/av_seller_2.jpeg',
    'assets/images/avatars/av_seller_3.jpeg',
    'assets/images/avatars/av_seller_4.jpeg',
    'assets/images/avatars/av_seller_5.jpeg'
  ];

  selectedAvatar = '';

  get currentAvatarList(): string[] {
    const r = (this.role || '').toUpperCase();
    let list: string[];
    if (r === 'SELLER' || r === 'VENDEDOR') {
      list = [...this.sellerAvatars];
    } else if (r === 'CLIENT' || r === 'CLIENTE') {
      list = [...this.clientAvatars];
    } else {
      list = [...this.clientAvatars, ...this.sellerAvatars];
    }
    if (this.selectedAvatar && !list.includes(this.selectedAvatar)) {
      list = [this.selectedAvatar, ...list];
    }
    return list;
  }

  selectAvatar(avatar: string): void {
    this.selectedAvatar = avatar;
  }

  private normalizeAvatarPath(path: string | null | undefined): string {
    if (!path) return '';
    return path.replace(/^\/+/, '');
  }

  isAdmin(): boolean {
    const r = (this.role || '').toUpperCase();
    return r === 'ADMIN';
  }

  isSeller(): boolean {
    const r = (this.role || '').toUpperCase();
    return r === 'SELLER' || r === 'VENDEDOR';
  }

  isClient(): boolean {
    const r = (this.role || '').toUpperCase();
    return r === 'CLIENT' || r === 'CLIENTE';
  }

  constructor(
    private readonly route: ActivatedRoute,
    private readonly router: Router,
    private readonly authService: AuthService,
    private readonly productsService: ProductsService
  ) { }

  ngOnInit(): void {
    this.userId = this.route.snapshot.paramMap.get('id') ?? '';
    if (!this.userId) {
      this.cargando.set(false);
      this.errorMessage.set('No se pudo identificar el usuario solicitado.');
      return;
    }

    this.authService.getAdminUser(this.userId).subscribe({
      next: ({ data }) => {
        this.email = data.email;
        this.role = data.role;
        if (this.isSeller()) {
          this.productsService.getCategories().subscribe({
            next: (categories) => {
              this.categories.splice(0, this.categories.length, ...this.categoryNames(categories));
              this.initializeUserForm(data);
            },
            error: () => {
              this.categoriesLoadError.set(true);
              this.initializeUserForm(data);
            },
          });
        } else {
          this.initializeUserForm(data);
        }
      },
      error: (error) => {
        this.errorMessage.set(this.errorMessageFor(error));
        this.cargando.set(false);
      }
    });
  }

  private initializeUserForm(data: UserResponse): void {
    this.email = data.email;
    this.role = data.role;
    const normalized = this.normalizeAvatarPath(data.profileImage);
    this.selectedAvatar = normalized || this.currentAvatarList[0] || this.clientAvatars[0];

    if (this.isClient()) {
      this.form.controls.birthDate.setValidators([Validators.required, isAdultValidator]);
    } else {
      this.form.controls.birthDate.clearValidators();
    }
    this.form.controls.birthDate.updateValueAndValidity();

    if (this.isSeller()) {
      this.form.controls.defaultCategory.setValidators([Validators.required]);
      this.form.controls.businessName.setValidators([
        Validators.required,
        Validators.minLength(2),
        Validators.maxLength(100)
      ]);
    } else {
      this.form.controls.defaultCategory.clearValidators();
      this.form.controls.businessName.clearValidators();
    }
    this.form.controls.defaultCategory.updateValueAndValidity();
    this.form.controls.businessName.updateValueAndValidity();

    if (this.isAdmin()) {
      this.form.controls.phone.clearValidators();
    } else {
      this.form.controls.phone.setValidators([Validators.pattern(/^\d{9}$/)]);
    }
    this.form.controls.phone.updateValueAndValidity();

    const savedCategory = data.defaultCategory?.trim() ?? '';
    const categoryIsCurrent = this.categories.includes(savedCategory);
    this.categorySelectionUnavailable.set(
      this.isSeller() && !this.categoriesLoadError() && Boolean(savedCategory) && !categoryIsCurrent,
    );

    const defaultCategory = this.isSeller() && categoryIsCurrent
      ? savedCategory
      : '';

    this.form.patchValue({
      firstName: data.firstName,
      lastName: data.lastName,
      birthDate: data.birthDate ? String(data.birthDate).slice(0, 10) : '',
      defaultCategory,
      businessName: data.businessName ?? '',
      nationalId: data.nationalId ?? '',
      phone: data.phone ?? '',
      headquarters: data.headquarters ?? ''
    });
    this.cargando.set(false);
  }

  private categoryNames(categories: Category[]): string[] {
    return [...new Set(categories
      .map((category) => category.name.trim())
      .filter((name) => name.length > 0))];
  }

  getEmail(): string {
    return this.email;
  }

  getRole(): string {
    return this.role;
  }

  onSubmit(): void {
    this.errorMessage.set('');
    this.successMessage.set('');
    if (this.isSeller() && this.categoriesLoadError()) {
      this.errorMessage.set('No se pudieron cargar las categorías. Recarga la página antes de guardar los cambios.');
      return;
    }
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.guardando.set(true);
    const request: UpdateUserRequest = {
      firstName: this.form.controls.firstName.value.trim(),
      lastName: this.form.controls.lastName.value.trim(),
      nationalId: this.form.controls.nationalId.value.trim(),
      profileImage: this.selectedAvatar
    };

    if (this.isClient()) {
      request.phone = this.form.controls.phone.value.trim();
      const birth = this.form.controls.birthDate.value;
      if (birth) {
        request.birthDate = birth;
      }
    } else if (this.isSeller()) {
      request.phone = this.form.controls.phone.value.trim();
      request.businessName = this.form.controls.businessName.value.trim();
      request.defaultCategory = this.form.controls.defaultCategory.value;
    } else if (this.isAdmin()) {
      request.headquarters = this.form.controls.headquarters.value.trim();
    } else {
      request.phone = this.form.controls.phone.value.trim();
    }

    this.authService.updateAdminUser(this.userId, request).subscribe({
      next: () => {
        this.guardando.set(false);
        this.router.navigate(['/admin/usuarios'], {
          state: { successMessage: 'Los cambios se han guardado correctamente.' }
        });
      },
      error: (error) => {
        this.errorMessage.set(this.errorMessageFor(error));
        this.guardando.set(false);
      }
    });
  }

  private errorMessageFor(error: any): string {
    if (error.status === 401) {
      return httpErrorMessage(error, {
        401: 'La sesión ha caducado. Inicie sesión como administrador y vuelva a intentarlo.',
      }) ?? 'No se pudo completar la operación. Compruebe los datos e inténtelo de nuevo.';
    }
    if (error.status === 403) return 'No tiene permisos para modificar usuarios.';
    if (error.status === 404) return 'El usuario solicitado ya no existe.';
    if (error.status === 409) return error.error?.message || 'Ya existe un usuario o comercio con esos datos.';
    const fieldErrors = error.error?.errors;
    if (fieldErrors && typeof fieldErrors === 'object') {
      return Object.values(fieldErrors).join(' ');
    }
    return 'No se pudo completar la operación. Compruebe los datos e inténtelo de nuevo.';
  }
}
