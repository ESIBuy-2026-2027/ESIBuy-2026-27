import { Component, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { AuthService, UpdateUserRequest } from '../../../core/services/auth.service';

@Component({
  selector: 'app-admin-user-detail',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './admin-user-detail.html',
  styleUrl: './admin-user-detail.css'
})
export class AdminUserDetail implements OnInit {
  private readonly formBuilder = inject(FormBuilder);
  readonly cargando = signal(true);
  readonly guardando = signal(false);
  readonly errorMessage = signal('');
  readonly successMessage = signal('');
  readonly credentialMask = '••••••••••••';
  readonly form = this.formBuilder.nonNullable.group({
    firstName: ['', [Validators.required, Validators.minLength(2), Validators.maxLength(25),
      Validators.pattern(/^[\p{L}\p{M}][\p{L}\p{M} '-]*$/u)]],
    lastName: ['', [Validators.required, Validators.minLength(2), Validators.maxLength(50),
      Validators.pattern(/^[\p{L}\p{M}][\p{L}\p{M} '-]*$/u)]],
    phone: ['', [Validators.maxLength(10), Validators.pattern(/^$|^\+?\d{9}$/)]]
  });

  private userId = '';
  private email = '';
  private role = '';

  constructor(
    private readonly route: ActivatedRoute,
    private readonly authService: AuthService
  ) {}

  ngOnInit(): void {
    this.userId = this.route.snapshot.paramMap.get('id') ?? '';
    if (!this.userId) {
      this.cargando.set(false);
      this.errorMessage.set('No se pudo identificar el usuario solicitado.');
      return;
    }

    this.authService.obtenerUsuarioAdmin(this.userId).subscribe({
      next: ({ data }) => {
        this.email = data.email;
        this.role = data.role;
        this.form.patchValue({
          firstName: data.firstName,
          lastName: data.lastName,
          phone: data.phone ?? ''
        });
        this.cargando.set(false);
      },
      error: (error) => {
        this.errorMessage.set(this.errorMessageFor(error));
        this.cargando.set(false);
      }
    });
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
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.guardando.set(true);
    const request: UpdateUserRequest = {
      firstName: this.form.controls.firstName.value.trim(),
      lastName: this.form.controls.lastName.value.trim(),
      phone: this.form.controls.phone.value.trim()
    };
    this.authService.actualizarUsuarioAdmin(this.userId, request).subscribe({
      next: ({ message }) => {
        this.successMessage.set(message);
        this.guardando.set(false);
      },
      error: (error) => {
        this.errorMessage.set(this.errorMessageFor(error));
        this.guardando.set(false);
      }
    });
  }

  private errorMessageFor(error: any): string {
    if (error.status === 401) return 'La sesión ha caducado. Inicie sesión como administrador y vuelva a intentarlo.';
    if (error.status === 403) return 'No tiene permisos para modificar usuarios.';
    if (error.status === 404) return 'El usuario solicitado ya no existe.';
    const fieldErrors = error.error?.errors;
    if (fieldErrors && typeof fieldErrors === 'object') {
      return Object.values(fieldErrors).join(' ');
    }
    return 'No se pudo completar la operación. Compruebe los datos e inténtelo de nuevo.';
  }
}