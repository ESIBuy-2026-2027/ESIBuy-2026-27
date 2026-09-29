import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators, AbstractControl, ValidationErrors } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';

export function isAdultValidator(control: AbstractControl): ValidationErrors | null {
  if (!control.value) return null;
  const birthDate = new Date(control.value);
  const today = new Date();
  let age = today.getFullYear() - birthDate.getFullYear();
  const monthDiff = today.getMonth() - birthDate.getMonth();
  if (monthDiff < 0 || (monthDiff === 0 && today.getDate() < birthDate.getDate())) {
    age--;
  }
  return age >= 18 ? null : { underAge: true };
}

export function passwordMatchValidator(group: AbstractControl): ValidationErrors | null {
  const password = group.get('password')?.value;
  const repeatedPassword = group.get('repeatedPassword')?.value;
  return (password && repeatedPassword && password === repeatedPassword) ? null : { passwordMismatch: true };
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
  
  defaultAvatars = {
    cliente: 'https://api.dicebear.com/7.x/bottts/svg?seed=cliente_desconocido',
    vendedor: 'https://api.dicebear.com/7.x/bottts/svg?seed=vendedor_desconocido'
  };

  categories = ['Electrónica', 'Hogar', 'Moda', 'Deportes', 'Libros', 'Otros'];

  constructor(
    private fb: FormBuilder,
    private router: Router,
    private authService: AuthService
  ) {
    this.form = this.createForm();
  }

  setRole(role: 'cliente' | 'vendedor') {
    this.userRole = role;
    this.serverError = null;
    this.form = this.createForm();
  }

  createForm(): FormGroup {
    const phonePattern = /^[0-9]{9}$/;
    const strongPasswordPattern = /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[^A-Za-z0-9])\S+$/;

    if (this.userRole === 'cliente') {
      return this.fb.group({
        firstName: ['', [Validators.required, Validators.minLength(2), Validators.maxLength(50)]],
        lastName: ['', [Validators.required, Validators.minLength(2), Validators.maxLength(100)]],
        birthDate: ['', [Validators.required, isAdultValidator]],
        nationalId: ['', [Validators.required, Validators.maxLength(20)]],
        email: ['', [Validators.required, Validators.email, Validators.maxLength(120)]],
        phone: ['', [Validators.pattern(phonePattern)]],
        customerType: ['STANDARD', [Validators.required]],
        password: ['', [Validators.required, Validators.minLength(12), Validators.maxLength(128), Validators.pattern(strongPasswordPattern)]],
        repeatedPassword: ['', [Validators.required]]
      }, { validators: passwordMatchValidator });
    } else {
      return this.fb.group({
        firstName: ['', [Validators.required, Validators.minLength(2), Validators.maxLength(50)]],
        lastName: ['', [Validators.required, Validators.minLength(2), Validators.maxLength(100)]],
        defaultCategory: [this.categories[0], [Validators.required]],
        businessName: ['', [Validators.required, Validators.minLength(2), Validators.maxLength(100)]],
        nationalId: ['', [Validators.required, Validators.maxLength(20)]],
        email: ['', [Validators.required, Validators.email, Validators.maxLength(120)]],
        phone: ['', [Validators.pattern(phonePattern)]],
        password: ['', [Validators.required, Validators.minLength(12), Validators.maxLength(128), Validators.pattern(strongPasswordPattern)]],
        repeatedPassword: ['', [Validators.required]]
      }, { validators: passwordMatchValidator });
    }
  }

  get profileImage(): string {
    return this.defaultAvatars[this.userRole];
  }

  onSubmit() {
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
        profileImage: this.profileImage,
        password: this.form.value.password,
        repeatedPassword: this.form.value.repeatedPassword
      };

      this.authService.registerClient(payload).subscribe({
        next: () => {
          this.loading = false;
          alert('Registro completado exitosamente. Tu cuenta requiere activación por un administrador.');
          this.router.navigate(['/login']);
        },
        error: (err) => {
          this.loading = false;
          this.serverError = err?.error?.message || 'Error en el servidor durante el registro.';
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
        profileImage: this.profileImage,
        password: this.form.value.password,
        repeatedPassword: this.form.value.repeatedPassword
      };

      this.authService.registerSeller(payload).subscribe({
        next: () => {
          this.loading = false;
          alert('Registro completado exitosamente. Tu cuenta requiere activación por un administrador.');
          this.router.navigate(['/login']);
        },
        error: (err) => {
          this.loading = false;
          this.serverError = err?.error?.message || 'Error en el servidor durante el registro.';
        }
      });
    }
  }
}
