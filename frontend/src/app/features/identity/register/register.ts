import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import {
  ReactiveFormsModule,
  FormBuilder,
  FormGroup,
  Validators,
  AbstractControl,
  ValidationErrors
} from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';

export function isAdultValidator(control: AbstractControl): ValidationErrors | null {
  if (!control.value) return null;

  const birthDate = new Date(control.value);
  const today = new Date();

  let age = today.getFullYear() - birthDate.getFullYear();

  const monthDiff = today.getMonth() - birthDate.getMonth();

  if (
    monthDiff < 0 ||
    (monthDiff === 0 && today.getDate() < birthDate.getDate())
  ) {
    age--;
  }

  return age >= 18 ? null : { underAge: true };
}

export function passwordMatchValidator(
  group: AbstractControl
): ValidationErrors | null {
  const password = group.get('password')?.value;
  const repeatedPassword = group.get('repeatedPassword')?.value;

  if (!password || !repeatedPassword) {
    return null;
  }

  return password === repeatedPassword
    ? null
    : { passwordMismatch: true };
}

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './register.html',
  styleUrl: './register.css'
})
export class Register {

  userRole: 'cliente' | 'vendedor' = 'cliente';

  form: FormGroup;

  serverError: string | null = null;

  loading = false;

  showPassword = false;

  clientAvatars = [
    'assets/images/avatars/av_client_1.jpeg',
    'assets/images/avatars/av_client_2.jpeg',
    'assets/images/avatars/av_client_3.jpeg',
    'assets/images/avatars/av_client_4.jpeg',
    'assets/images/avatars/av_client_5.jpeg'
  ];

  sellerAvatars = [
    'assets/images/avatars/av_seller_1.jpeg',
    'assets/images/avatars/av_seller_2.jpeg',
    'assets/images/avatars/av_seller_3.jpeg',
    'assets/images/avatars/av_seller_4.jpeg',
    'assets/images/avatars/av_seller_5.jpeg'
  ];

  selectedAvatar: string = this.clientAvatars[0];

  categories = [
    'Electrónica',
    'Hogar',
    'Moda',
    'Deportes',
    'Libros',
    'Otros'
  ];

  constructor(
    private fb: FormBuilder,
    private router: Router,
    private authService: AuthService
  ) {
    this.form = this.createForm();
  }

  setRole(role: 'cliente' | 'vendedor'): void {
    this.userRole = role;

    this.serverError = null;

    this.selectedAvatar =
      role === 'cliente'
        ? this.clientAvatars[0]
        : this.sellerAvatars[0];

    this.form = this.createForm();
  }

  selectAvatar(avatarPath: string): void {
    this.selectedAvatar = avatarPath;
  }

  get currentAvatarList(): string[] {
    return this.userRole === 'cliente'
      ? this.clientAvatars
      : this.sellerAvatars;
  }

  createForm(): FormGroup {
    const phonePattern = /^\d{9}$/;

    const strongPasswordPattern =
      /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[^A-Za-z0-9])\S+$/;

    if (this.userRole === 'cliente') {

      return this.fb.group({
        firstName: [
          '',
          [
            Validators.required,
            Validators.minLength(2),
            Validators.maxLength(25)
          ]
        ],

        lastName: [
          '',
          [
            Validators.required,
            Validators.minLength(2),
            Validators.maxLength(100)
          ]
        ],

        birthDate: [
          '',
          [
            Validators.required,
            isAdultValidator
          ]
        ],

        nationalId: [
          '',
          [
            Validators.required,
            Validators.maxLength(20)
          ]
        ],

        email: [
          '',
          [
            Validators.required,
            Validators.email,
            Validators.maxLength(120)
          ]
        ],

        phone: [
          '',
          [
            Validators.pattern(phonePattern)
          ]
        ],

        customerType: [
          'STANDARD',
          [
            Validators.required
          ]
        ],

        password: [
          '',
          [
            Validators.required,
            Validators.minLength(12),
            Validators.maxLength(128),
            Validators.pattern(strongPasswordPattern)
          ]
        ],

        repeatedPassword: [
          '',
          [
            Validators.required
          ]
        ]

      }, {
        validators: passwordMatchValidator
      });

    } else {

      return this.fb.group({
        firstName: [
          '',
          [
            Validators.required,
            Validators.minLength(2),
            Validators.maxLength(25)
          ]
        ],

        lastName: [
          '',
          [
            Validators.required,
            Validators.minLength(2),
            Validators.maxLength(100)
          ]
        ],

        defaultCategory: [
          this.categories[0],
          [
            Validators.required
          ]
        ],

        businessName: [
          '',
          [
            Validators.required,
            Validators.minLength(2),
            Validators.maxLength(100)
          ]
        ],

        nationalId: [
          '',
          [
            Validators.required,
            Validators.maxLength(20)
          ]
        ],

        email: [
          '',
          [
            Validators.required,
            Validators.email,
            Validators.maxLength(120)
          ]
        ],

        phone: [
          '',
          [
            Validators.pattern(phonePattern)
          ]
        ],

        password: [
          '',
          [
            Validators.required,
            Validators.minLength(12),
            Validators.maxLength(128),
            Validators.pattern(strongPasswordPattern)
          ]
        ],

        repeatedPassword: [
          '',
          [
            Validators.required
          ]
        ]

      }, {
        validators: passwordMatchValidator
      });
    }
  }

  onSubmit(): void {
    this.serverError = null;

    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.loading = true;

    if (this.userRole === 'cliente') {

      const payload = {
        firstName: this.form.value.firstName,
        lastName: this.form.value.lastName,
        birthDate: this.form.value.birthDate,
        nationalId: this.form.value.nationalId,
        email: this.form.value.email,
        phone: this.form.value.phone || null,
        customerType: this.form.value.customerType,
        profileImage: this.selectedAvatar,
        password: this.form.value.password,
        repeatedPassword: this.form.value.repeatedPassword
      };

      this.authService.registerClient(payload).subscribe({

        next: () => {
          this.loading = false;

          alert(
            'Registro completado exitosamente. Tu cuenta requiere activación por un administrador.'
          );

          this.router.navigate(['/login']);
        },

        error: (err) => {
          this.loading = false;

          this.serverError =
            err?.error?.message ||
            'Error en el servidor durante el registro.';
        }

      });

    } else {

      const payload = {
        firstName: this.form.value.firstName,
        lastName: this.form.value.lastName,
        defaultCategory: this.form.value.defaultCategory,
        businessName: this.form.value.businessName,
        nationalId: this.form.value.nationalId,
        email: this.form.value.email,
        phone: this.form.value.phone || null,
        profileImage: this.selectedAvatar,
        password: this.form.value.password,
        repeatedPassword: this.form.value.repeatedPassword
      };

      this.authService.registerSeller(payload).subscribe({

        next: () => {
          this.loading = false;

          alert(
            'Registro completado exitosamente. Tu cuenta requiere activación por un administrador.'
          );

          this.router.navigate(['/login']);
        },

        error: (err) => {
          this.loading = false;

          this.serverError =
            err?.error?.message ||
            'Error en el servidor durante el registro.';
        }

      });
    }
  }
}