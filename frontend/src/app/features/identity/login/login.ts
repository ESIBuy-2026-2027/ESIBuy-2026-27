import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './login.html',
  styleUrl: './login.css'
})
export class Login {
  loginForm: FormGroup;
  serverError: string | null = null;
  loading = false;
  showPassword = false;

  constructor(
    private fb: FormBuilder,
    private router: Router,
    private authService: AuthService
  ) {
    const strongPasswordPattern = /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[^A-Za-z0-9])\S+$/;

    this.loginForm = this.fb.group({
      email: ['', [
        Validators.required,
        Validators.email,
        Validators.maxLength(120)
      ]],
      password: ['', [
        Validators.required,
        Validators.minLength(12),
        Validators.maxLength(128),
        Validators.pattern(strongPasswordPattern)
      ]]
    });
  }

  onSubmit() {
    this.serverError = null;

    if (this.loginForm.invalid) {
      this.loginForm.markAllAsTouched();
      return;
    }

    this.loading = true;

    this.authService.login(this.loginForm.value).subscribe({
      next: (response) => {
        this.loading = false;
        alert(`¡Bienvenido! Sesión iniciada como ${response.email} (${response.role})`);
        this.router.navigate(['/']);
      },
      error: (err) => {
        this.loading = false;
        this.serverError =
          err?.error?.message ||
          'Error en el servidor al iniciar sesión. Revisa tus credenciales.';
      }
    });
  }

  showForgotPasswordMessage() {
    alert('La recuperación de contraseña estará disponible próximamente.');
  }

}