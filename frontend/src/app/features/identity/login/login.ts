import { HttpErrorResponse } from '@angular/common/http';
import { CommonModule } from '@angular/common';
import { Component } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
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
  forgotPasswordMessage: string | null = null;
  loading = false;
  showPassword = false;

  constructor(
    private fb: FormBuilder,
    private route: ActivatedRoute,
    private router: Router,
    private authService: AuthService
  ) {
    this.loginForm = this.fb.group({
      email: ['', [
        Validators.required,
        Validators.email,
        Validators.maxLength(120)
      ]],
      password: ['', [
        Validators.required,
        Validators.maxLength(128),
      ]]
    });

    if (this.route.snapshot.queryParamMap.get('expired') === 'true') {
      this.serverError = 'Tu sesión ha expirado';
    }
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
        if (response.role === 'ADMIN') {
          void this.router.navigate(['/admin/usuarios']);
        } else if (response.role === 'SELLER') {
          void this.router.navigate(['/products/new']);
        } else {
          void this.router.navigate(['/']);
        }
      },
      error: (error: unknown) => {
        this.loading = false;
        this.serverError = error instanceof HttpErrorResponse
          && typeof error.error?.message === 'string'
          ? error.error.message
          : 'Error en el servidor al iniciar sesión. Revisa tus credenciales.';
      }
    });
  }

  showForgotPasswordMessage() {
    this.forgotPasswordMessage = 'La recuperación de contraseña estará disponible próximamente.';
  }

}