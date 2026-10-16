import { HttpErrorResponse } from '@angular/common/http';
import { CommonModule } from '@angular/common';
import { ChangeDetectorRef, Component, Input, OnDestroy, computed } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { finalize } from 'rxjs';
import { AuthService, AuthenticatedUser, UserRole } from '../../../core/services/auth.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './login.html',
  styleUrl: './login.css'
})
export class Login implements OnDestroy {
  @Input() embedded = false;

  readonly currentUser = computed<AuthenticatedUser | null>(() => {
    if (typeof this.authService?.currentUser === 'function') {
      return this.authService.currentUser();
    }
    return null;
  });

  readonly loginForm: FormGroup;
  successMessage: string | null = null;
  serverError: string | null = null;
  forgotPasswordMessage: string | null = null;
  retryAfterSeconds = 0;
  loading = false;
  showPassword = false;
  requires2FA = false;

  private retryCountdownId: ReturnType<typeof setInterval> | null = null;
  private retryBaseMessage: string | null = null;

  constructor(
    private readonly fb: FormBuilder,
    private readonly route: ActivatedRoute,
    private readonly router: Router,
    private readonly authService: AuthService,
    private readonly changeDetectorRef: ChangeDetectorRef
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
      ]],
      totpCode: ['']
    });

    if (this.route.snapshot.queryParamMap.get('expired') === 'true') {
      this.serverError = 'Tu sesión ha expirado';
    }
    if (this.route.snapshot.queryParamMap.get('deleted') === 'true') {
      this.successMessage = 'Tu cuenta se ha eliminado correctamente.';
    }
  }

  onSubmit() {
    if (this.loginForm.invalid || this.loading || this.retryAfterSeconds > 0) {
      this.loginForm.markAllAsTouched();
      return;
    }

    if (this.requires2FA) {
      const codeVal = this.loginForm.get('totpCode')?.value;
      if (!codeVal || codeVal.trim().length === 0) {
        this.loginForm.get('totpCode')?.setErrors({ required: true });
        this.loginForm.markAllAsTouched();
        return;
      }
    }

    this.serverError = null;
    this.loading = true;

    this.authService.login(this.loginForm.value).pipe(
      finalize(() => {
        this.loading = false;
        this.changeDetectorRef.markForCheck();
      })
    ).subscribe({
      next: (response) => {
        if (response.requires2fa) {
          this.requires2FA = true;
          this.serverError = null;
          const totpCtrl = this.loginForm.get('totpCode');
          totpCtrl?.setValidators([Validators.required]);
          totpCtrl?.updateValueAndValidity();
          this.changeDetectorRef.markForCheck();
          return;
        }

        if (response.role === 'ADMIN') {
          void this.router.navigate(['/admin/usuarios']);
        } else if (response.role === 'SELLER') {
          void this.router.navigate(['/seller/catalog']);
        } else if (response.role === 'CLIENT') {
          void this.router.navigate(['/client/dashboard']);
        } else {
          void this.router.navigate(['/']);
        }
      },
      error: (error: unknown) => {
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
        this.changeDetectorRef.markForCheck();
      }
    });
  }

  cancel2FA() {
    this.requires2FA = false;
    this.loginForm.get('totpCode')?.setValue('');
    this.loginForm.get('totpCode')?.clearValidators();
    this.loginForm.get('totpCode')?.updateValueAndValidity();
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
    this.changeDetectorRef.markForCheck();
    this.retryCountdownId = setInterval(() => {
      this.retryAfterSeconds -= 1;
      if (this.retryAfterSeconds <= 0) {
        this.retryAfterSeconds = 0;
        this.serverError = this.retryBaseMessage;
        this.clearRetryCountdown();
        this.changeDetectorRef.markForCheck();
        return;
      }
      this.updateRetryMessage();
      this.changeDetectorRef.markForCheck();
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

  getUserDisplayName(user: AuthenticatedUser | null): string {
    if (!user?.email) {
      return 'Usuario';
    }
    const namePart = user.email.split('@')[0];
    return namePart.charAt(0).toUpperCase() + namePart.slice(1);
  }

  getDashboardRoute(role?: UserRole): string {
    switch (role) {
      case 'ADMIN':
        return '/admin/usuarios';
      case 'SELLER':
        return '/seller/catalog';
      case 'CLIENT':
        return '/client/dashboard';
      default:
        return '/';
    }
  }

  getDashboardButtonText(role?: UserRole): string {
    switch (role) {
      case 'ADMIN':
        return 'Ir al panel de administración';
      case 'SELLER':
        return 'Ir a mi catálogo de vendedor';
      case 'CLIENT':
        return 'Ir a mi panel de cliente';
      default:
        return 'Ir a mi panel';
    }
  }

  getRoleLabel(role?: UserRole): string {
    switch (role) {
      case 'ADMIN':
        return 'Administrador';
      case 'SELLER':
        return 'Vendedor';
      case 'CLIENT':
        return 'Cliente';
      default:
        return 'Usuario';
    }
  }

  logout(): void {
    if (typeof this.authService?.logout === 'function') {
      this.authService.logout().subscribe({
        next: () => {
          void this.router.navigate(['/']);
          this.changeDetectorRef.markForCheck();
        },
        error: () => {
          this.authService.clearCurrentUser?.();
          void this.router.navigate(['/']);
          this.changeDetectorRef.markForCheck();
        }
      });
    }
  }
}