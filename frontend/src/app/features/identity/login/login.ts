import { HttpErrorResponse } from '@angular/common/http';
import { CommonModule } from '@angular/common';
import { Component, OnDestroy } from '@angular/core';
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
export class Login implements OnDestroy {
  loginForm: FormGroup;
  serverError: string | null = null;
  forgotPasswordMessage: string | null = null;
  retryAfterSeconds = 0;
  loading = false;
  showPassword = false;
  private retryCountdownId: ReturnType<typeof setInterval> | null = null;
  private retryBaseMessage: string | null = null;

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
    if (this.loginForm.invalid || this.loading || this.retryAfterSeconds > 0) {
      this.loginForm.markAllAsTouched();
      return;
    }

    this.serverError = null;
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
        const message = error instanceof HttpErrorResponse
          && typeof error.error?.message === 'string'
          ? error.error.message
          : 'Error en el servidor al iniciar sesión. Revisa tus credenciales.';

        if (error instanceof HttpErrorResponse && error.status === 429) {
          const retryAfterSeconds = this.getRetryAfterSeconds(error);
          if (retryAfterSeconds !== null) {
            this.startRetryCountdown(message, retryAfterSeconds);
            return;
          }
        }

        this.serverError = message;
      }
    });
  }

  ngOnDestroy(): void {
    this.clearRetryCountdown();
  }

  private getRetryAfterSeconds(error: HttpErrorResponse): number | null {
    const retryAfter = error.headers.get('Retry-After');
    if (!retryAfter) {
      return null;
    }

    const seconds = Number(retryAfter);
    if (Number.isFinite(seconds) && seconds > 0) {
      return Math.ceil(seconds);
    }

    const retryAt = Date.parse(retryAfter);
    if (!Number.isNaN(retryAt)) {
      const remainingSeconds = Math.ceil((retryAt - Date.now()) / 1000);
      return remainingSeconds > 0 ? remainingSeconds : null;
    }

    return null;
  }

  private startRetryCountdown(message: string, seconds: number): void {
    this.clearRetryCountdown();
    this.retryBaseMessage = message;
    this.retryAfterSeconds = seconds;
    this.updateRetryMessage();
    this.retryCountdownId = setInterval(() => {
      this.retryAfterSeconds -= 1;
      if (this.retryAfterSeconds <= 0) {
        this.retryAfterSeconds = 0;
        this.serverError = this.retryBaseMessage;
        this.clearRetryCountdown();
        return;
      }
      this.updateRetryMessage();
    }, 1000);
  }

  private updateRetryMessage(): void {
    this.serverError = `${this.retryBaseMessage} Puedes volver a intentarlo en ${this.retryAfterSeconds} segundos.`;
  }

  private clearRetryCountdown(): void {
    if (this.retryCountdownId !== null) {
      clearInterval(this.retryCountdownId);
      this.retryCountdownId = null;
    }
  }

  showForgotPasswordMessage() {
    this.forgotPasswordMessage = 'La recuperación de contraseña estará disponible próximamente.';
  }

}