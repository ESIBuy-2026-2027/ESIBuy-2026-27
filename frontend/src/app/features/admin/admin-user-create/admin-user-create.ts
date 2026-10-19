import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import {
  AbstractControl,
  FormBuilder,
  FormGroup,
  ReactiveFormsModule,
  ValidationErrors,
  Validators,
} from '@angular/forms';
import { Router, RouterLink, RouterLinkActive } from '@angular/router';

import { AppSidebarComponent } from '../../../shared/app-sidebar/app-sidebar';
import { AuthService, CreateAdminRequest } from '../../../core/services/auth.service';

export function passwordsMatchValidator(
  control: AbstractControl,
): ValidationErrors | null {
  const password = control.get('password')?.value;
  const confirmPassword = control.get('confirmPassword')?.value;

  if (!password || !confirmPassword) {
    return null;
  }

  return password === confirmPassword
    ? null
    : { passwordsMismatch: true };
}

@Component({
  selector: 'app-admin-user-create',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink, RouterLinkActive],
  templateUrl: './admin-user-create.html',
  styleUrls: ['./admin-user-create.css'],
})
export class AdminUserCreate {
  sidebarCollapsed = signal(false);

  toggleSidebar(): void {
    this.sidebarCollapsed.update((collapsed) => !collapsed);
  }
  private readonly fb = inject(FormBuilder);
  private readonly formBuilder = inject(FormBuilder);
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);

  submitting = false;
  showPassword = false;
  errorMessage = '';
  successMessage = '';
  fieldErrors: Record<string, string> = {};

  readonly creationDate = new Date();

  readonly avatars = [
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

  readonly form: FormGroup = this.formBuilder.group(
    {
      nationalId: ['', [Validators.required]],
      firstName: [
        '',
        [
          Validators.required,
          Validators.minLength(2),
          Validators.maxLength(25),
        ],
      ],
      lastName: [
        '',
        [
          Validators.required,
          Validators.minLength(2),
          Validators.maxLength(50),
        ],
      ],
      email: [
        '',
        [
          Validators.required,
          Validators.email,
          Validators.maxLength(120),
        ],
      ],
      password: [
        '',
        [
          Validators.required,
          Validators.minLength(12),
          Validators.maxLength(128),
          Validators.pattern(
            /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[^A-Za-z0-9])\S+$/,
          ),
        ],
      ],
      confirmPassword: ['', [Validators.required]],
      headquarters: ['', [Validators.maxLength(100)]],
      profileImage: ['', [Validators.maxLength(500)]],
    },
    { validators: passwordsMatchValidator },
  );

  selectAvatar(url: string): void {
    if (this.form.get('profileImage')?.value === url) {
      this.form.patchValue({ profileImage: '' });
    } else {
      this.form.patchValue({ profileImage: url });
    }
  }

  cancel(): void {
    this.router.navigate(['/admin/usuarios']);
  }

  onSubmit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.submitting = true;
    this.errorMessage = '';
    this.fieldErrors = {};
    this.successMessage = '';

    const formValue = this.form.getRawValue();

    const payload: CreateAdminRequest = {
      nationalId: formValue.nationalId.trim(),
      firstName: formValue.firstName.trim(),
      lastName: formValue.lastName.trim(),
      email: formValue.email.trim(),
      password: formValue.password,
      confirmPassword: formValue.confirmPassword,
      headquarters: formValue.headquarters.trim() || null,
      profileImage: formValue.profileImage.trim() || null,
    };

    this.authService.createAdmin(payload).subscribe({
      next: () => {
        this.submitting = false;
        this.successMessage = 'Administrador creado con éxito';

        setTimeout(() => {
          this.router.navigate(['/admin/usuarios']);
        }, 1200);
      },
      error: (error: unknown) => {
        this.submitting = false;

        const response = error as {
          error?: {
            errors?: Record<string, string>;
            message?: string;
          };
        };

        if (response.error?.errors) {
          this.fieldErrors = response.error.errors;
        }

        this.errorMessage =
          response.error?.message ??
          'Ha ocurrido un error al crear el administrador';
      },
    });
  }
}