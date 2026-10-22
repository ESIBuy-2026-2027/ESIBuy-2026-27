
import { HttpErrorResponse } from '@angular/common/http';
import { CommonModule } from '@angular/common';
import {
  ChangeDetectorRef,
  Component,
  Input,
  OnDestroy,
  computed
} from '@angular/core';
import {
  FormBuilder,
  FormGroup,
  ReactiveFormsModule,
  Validators
} from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { finalize } from 'rxjs';
import {
  AuthService,
  AuthenticatedUser,
  UserRole
} from '../../../core/services/auth.service';

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
  readonly Math = Math;

  successMessage: string | null = null;
  serverError: string | null = null;
  forgotPasswordMessage: string | null = null;

  retryAfterSeconds = 0;
  loading = false;
  showPassword = false;

  requires2FA = false;
  requires3FA = false;
  otpExpiresInSeconds = 600;

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
        Validators.maxLength(128)
      ]],
      totpCode: [''],
      emailOtpCode: ['', [Validators.pattern(/^\d{6}$/)]]
    });

    if (this.route.snapshot.queryParamMap.get('expired') === 'true') {
      this.serverError = 'Tu sesión ha expirado';
    }

    if (this.route.snapshot.queryParamMap.get('deleted') === 'true') {
      this.successMessage = 'Tu cuenta se ha eliminado correctamente.';
    }
  }

  get otpExpiresInMinutes(): number {
    return Math.ceil(this.otpExpiresInSeconds / 60);
  }

  onSubmit(): void {
    if (this.loading || this.retryAfterSeconds > 0) {
      return;
    }

    // Tercer factor: verificar el código enviado por email.
    if (this.requires3FA) {
      this.submitEmailOtp();
      return;
    }

    if (this.loginForm.invalid) {
      this.loginForm.markAllAsTouched();
      return;
    }

    // Segundo factor: TOTP o código de respaldo.
    if (this.requires2FA) {
      const totpControl = this.loginForm.get('totpCode');
      const code = String(totpControl?.value ?? '').trim();

      if (!code) {
        totpControl?.setErrors({ required: true });
        totpControl?.markAsTouched();
        return;
      }
    }

    this.serverError = null;
    this.successMessage = null;
    this.loading = true;

    const credentials = {
      email: String(this.loginForm.get('email')?.value ?? '').trim(),
      password: String(this.loginForm.get('password')?.value ?? ''),
      totpCode: String(this.loginForm.get('totpCode')?.value ?? '').trim()
    };

    this.authService.login(credentials).pipe(
      finalize(() => {
        this.loading = false;
        this.changeDetectorRef.markForCheck();
      })
    ).subscribe({
      next: (response) => {
        if (response.requires2fa) {
          this.requires2FA = true;
          this.requires3FA = false;
          this.serverError = null;

          const totpControl = this.loginForm.get('totpCode');
          totpControl?.setValidators([Validators.required]);
          totpControl?.updateValueAndValidity();

          this.changeDetectorRef.markForCheck();
          return;
        }

        if (response.requires3fa) {
          this.requires2FA = false;
          this.requires3FA = true;
          this.otpExpiresInSeconds = response.expiresInSeconds ?? 600;
          this.serverError = null;

          this.loginForm.get('totpCode')?.setValue('');
          this.loginForm.get('emailOtpCode')?.reset('');

          this.changeDetectorRef.markForCheck();
          return;
        }

        if (response.id && response.role) {
          this.navigateByRole(response.role);
          return;
        }

        this.serverError = 'El servidor no ha devuelto una respuesta de autenticación válida.';
        this.changeDetectorRef.markForCheck();
      },
      error: (error: unknown) => {
        this.handleLoginError(error);
      }
    });
  }

  private submitEmailOtp(): void {
    const otpControl = this.loginForm.get('emailOtpCode');
    const code = String(otpControl?.value ?? '').trim();

    if (!/^\d{6}$/.test(code)) {
      otpControl?.setErrors({ pattern: true });
      otpControl?.markAsTouched();
      this.serverError = null;
      return;
    }

    this.serverError = null;
    this.loading = true;

    this.authService.verifyLoginOtp(code).pipe(
      finalize(() => {
        this.loading = false;
        this.changeDetectorRef.markForCheck();
      })
    ).subscribe({
      next: (response) => {
        if (response.id && response.role) {
          this.requires3FA = false;
          this.navigateByRole(response.role);
          return;
        }

        this.serverError = 'No se ha podido completar la autenticación.';
        this.changeDetectorRef.markForCheck();
      },
      error: (error: unknown) => {
        this.handleLoginError(error);
      }
    });
  }

  private handleLoginError(error: unknown): void {
    const message = error instanceof HttpErrorResponse
      && typeof error.error?.message === 'string'
      ? error.error.message
      : 'Error en el servidor al iniciar sesión. Revisa tus credenciales.';

    if (error instanceof HttpErrorResponse && error.status === 429) {
      const seconds = this.getRetryAfterSeconds(error);

      if (seconds !== null) {
        this.startRetryCountdown(message, seconds);
        return;
      }
    }

    this.serverError = message;
    this.changeDetectorRef.markForCheck();
  }

  private navigateByRole(role: UserRole): void {
    if (role === 'ADMIN') {
      void this.router.navigate(['/admin/usuarios']);
    } else if (role === 'SELLER') {
      void this.router.navigate(['/seller/catalog']);
    } else if (role === 'CLIENT') {
      void this.router.navigate(['/client/catalog']);
    } else {
      void this.router.navigate(['/']);
    }
  }

  cancel2FA(): void {
    this.requires2FA = false;
    this.requires3FA = false;

    const totpControl = this.loginForm.get('totpCode');
    totpControl?.reset('');
    totpControl?.clearValidators();
    totpControl?.setErrors(null);
    totpControl?.updateValueAndValidity();

    const otpControl = this.loginForm.get('emailOtpCode');
    otpControl?.reset('');
    otpControl?.setErrors(null);
    otpControl?.updateValueAndValidity();

    this.serverError = null;
    this.successMessage = null;
    this.forgotPasswordMessage = null;

    this.changeDetectorRef.markForCheck();
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
    this.serverError =
      `${this.retryBaseMessage} Puedes volver a intentarlo en ${this.retryAfterSeconds} segundos.`;
  }

  private clearRetryCountdown(): void {
    if (this.retryCountdownId !== null) {
      clearInterval(this.retryCountdownId);
      this.retryCountdownId = null;
    }
  }

  showForgotPasswordMessage(): void {
    this.forgotPasswordMessage =
      'La recuperación de contraseña estará disponible próximamente.';
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
        return '/client/catalog';
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
        return 'Ir a mi catálogo';
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
    if (typeof this.authService?.logout !== 'function') {
      return;
    }

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
