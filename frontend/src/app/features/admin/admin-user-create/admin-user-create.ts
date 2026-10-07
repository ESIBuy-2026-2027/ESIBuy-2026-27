import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import {
  AbstractControl,
  FormBuilder,
  FormGroup,
  ReactiveFormsModule,
  ValidationErrors,
  Validators,
} from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';

export function passwordsMatchValidator(control: AbstractControl): ValidationErrors | null {
  const contrasena = control.get('contrasena')?.value;
  const repetirContrasena = control.get('repetirContrasena')?.value;
  if (!contrasena || !repetirContrasena) {
    return null;
  }
  return contrasena === repetirContrasena ? null : { passwordsMismatch: true };
}

@Component({
  selector: 'app-admin-user-create',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './admin-user-create.html',
  styleUrls: ['./admin-user-create.css'],
})
export class AdminUserCreate {
  private readonly fb = inject(FormBuilder);
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);

  submitting = false;
  errorMessage = '';
  successMessage = '';
  fieldErrors: Record<string, string> = {};

  readonly avatars = [
    'https://api.dicebear.com/7.x/bottts/svg?seed=Admin1',
    'https://api.dicebear.com/7.x/bottts/svg?seed=Admin2',
    'https://api.dicebear.com/7.x/bottts/svg?seed=Admin3',
    'https://api.dicebear.com/7.x/bottts/svg?seed=Admin4',
  ];

  readonly form: FormGroup = this.fb.group(
    {
      dni: [
        '',
        [
          Validators.required,
          Validators.pattern(/^[0-9]{8}[A-Za-z]$/),
        ],
      ],
      nombre: [
        '',
        [
          Validators.required,
          Validators.minLength(2),
          Validators.maxLength(25),
        ],
      ],
      apellidos: [
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
      contrasena: [
        '',
        [
          Validators.required,
          Validators.minLength(12),
          Validators.maxLength(128),
          Validators.pattern(/^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[^A-Za-z0-9])\S+$/),
        ],
      ],
      repetirContrasena: ['', [Validators.required]],
      sede: ['', [Validators.maxLength(100)]],
      imagenPerfil: ['', [Validators.maxLength(500)]],
    },
    { validators: passwordsMatchValidator },
  );

  selectAvatar(url: string): void {
    if (this.form.get('imagenPerfil')?.value === url) {
      this.form.patchValue({ imagenPerfil: '' });
    } else {
      this.form.patchValue({ imagenPerfil: url });
    }
  }

  cancelar(): void {
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

    const payload = {
      dni: this.form.value.dni?.trim(),
      nombre: this.form.value.nombre?.trim(),
      apellidos: this.form.value.apellidos?.trim(),
      email: this.form.value.email?.trim(),
      contrasena: this.form.value.contrasena,
      repetirContrasena: this.form.value.repetirContrasena,
      sede: this.form.value.sede?.trim() || null,
      imagenPerfil: this.form.value.imagenPerfil?.trim() || null,
    };

    this.authService.crearAdministrador(payload).subscribe({
      next: () => {
        this.submitting = false;
        this.successMessage = 'Administrador creado con éxito';
        setTimeout(() => {
          this.router.navigate(['/admin/usuarios']);
        }, 1200);
      },
      error: (error: any) => {
        this.submitting = false;
        if (error?.error?.errors) {
          this.fieldErrors = error.error.errors;
        }
        this.errorMessage =
          error?.error?.message || 'Ha ocurrido un error al crear el administrador';
      },
    });
  }
}