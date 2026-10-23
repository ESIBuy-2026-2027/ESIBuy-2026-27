
import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { ChangeDetectorRef, Component } from '@angular/core';
import {
    AbstractControl,
    FormBuilder,
    ReactiveFormsModule,
    ValidationErrors,
    Validators,
} from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { finalize } from 'rxjs';
import { AuthService } from '../../../core/services/auth.service';

const passwordPattern =
    /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[^A-Za-z0-9])\S+$/;

type RecoveryStep = 'request' | 'token' | 'password' | 'success';

function passwordMatchValidator(
    control: AbstractControl,
): ValidationErrors | null {
    const password = control.get('newPassword')?.value;
    const confirmation = control.get('confirmPassword')?.value;

    if (!password || !confirmation) {
        return null;
    }

    return password === confirmation ? null : { passwordMismatch: true };
}

@Component({
    selector: 'app-password-recovery',
    standalone: true,
    imports: [CommonModule, ReactiveFormsModule, RouterLink],
    templateUrl: './password-recovery.html',
    styleUrl: './password-recovery.css',
})
export class PasswordRecovery {
    readonly requestForm;
    readonly resetForm;

    readonly tokenFromUrl: string;

    step: RecoveryStep = 'request';
    loading = false;
    showPassword = false;
    requestSubmitted = false;

    errorMessage: string | null = null;
    successMessage: string | null = null;

    constructor(
        private readonly fb: FormBuilder,
        private readonly route: ActivatedRoute,
        private readonly authService: AuthService,
        private readonly changeDetectorRef: ChangeDetectorRef,
    ) {
        this.tokenFromUrl =
            this.route.snapshot.queryParamMap.get('token') ?? '';

        // Si el enlace incluye un token, primero se debe validar.
        this.step = this.tokenFromUrl ? 'token' : 'request';

        this.requestForm = this.fb.group({
            email: [
                '',
                [
                    Validators.required,
                    Validators.email,
                    Validators.maxLength(254),
                ],
            ],
        });

        this.resetForm = this.fb.group(
            {
                token: [
                    this.tokenFromUrl,
                    [
                        Validators.required,
                        Validators.maxLength(128),
                    ],
                ],
                newPassword: [
                    '',
                    [
                        Validators.required,
                        Validators.minLength(12),
                        Validators.maxLength(128),
                        Validators.pattern(passwordPattern),
                    ],
                ],
                confirmPassword: ['', [Validators.required]],
            },
            {
                validators: passwordMatchValidator,
            },
        );
    }

    useRecoveryCode(): void {
        this.clearMessages();
        this.step = 'token';

        if (!this.tokenFromUrl) {
            this.resetForm.controls.token.reset('');
        }

        this.changeDetectorRef.markForCheck();
    }

    backToEmailRequest(): void {
        if (this.loading) {
            return;
        }

        this.clearMessages();
        this.step = 'request';
        this.changeDetectorRef.markForCheck();
    }

    backToTokenEntry(): void {
        if (this.loading) {
            return;
        }

        this.clearMessages();
        this.resetForm.controls.newPassword.reset('');
        this.resetForm.controls.confirmPassword.reset('');
        this.step = 'token';
        this.changeDetectorRef.markForCheck();
    }

    togglePasswordVisibility(): void {
        this.showPassword = !this.showPassword;
    }

    requestPasswordReset(): void {
        this.clearMessages();

        if (
            this.requestForm.invalid ||
            this.loading ||
            this.requestSubmitted
        ) {
            this.requestForm.markAllAsTouched();
            return;
        }

        const email = String(
            this.requestForm.controls.email.value ?? '',
        ).trim();

        this.loading = true;

        this.authService
            .requestPasswordReset(email)
            .pipe(
                finalize(() => {
                    this.loading = false;
                    this.changeDetectorRef.markForCheck();
                }),
            )
            .subscribe({
                next: (response) => {
                    this.requestSubmitted = true;
                    this.successMessage =
                        response?.message ||
                        'Si existe una cuenta asociada a ese correo, recibirás instrucciones para recuperar el acceso.';

                    this.changeDetectorRef.markForCheck();
                },
                error: (error: unknown) => {
                    this.errorMessage = this.getErrorMessage(
                        error,
                        'No se ha podido procesar la solicitud. Inténtalo de nuevo más tarde.',
                    );

                    this.changeDetectorRef.markForCheck();
                },
            });
    }

    validateRecoveryToken(): void {
        this.clearMessages();

        const tokenControl = this.resetForm.controls.token;
        tokenControl.markAsTouched();

        if (tokenControl.invalid || this.loading) {
            return;
        }

        const token = String(tokenControl.value ?? '').trim();

        if (!token) {
            this.errorMessage = 'Introduce el código de recuperación.';
            return;
        }

        this.loading = true;

        this.authService
            .validatePasswordResetToken(token)
            .pipe(
                finalize(() => {
                    this.loading = false;
                    this.changeDetectorRef.markForCheck();
                }),
            )
            .subscribe({
                next: (response) => {
                    // Solo se muestran las contraseñas tras validar el token.
                    this.resetForm.controls.token.setValue(token);
                    this.step = 'password';
                    this.successMessage =
                        response?.message ||
                        'El código es válido. Ya puedes establecer una nueva contraseña.';

                    this.changeDetectorRef.markForCheck();
                },
                error: (error: unknown) => {
                    this.errorMessage = this.getErrorMessage(
                        error,
                        'El código no es válido o ha caducado. Compruébalo e inténtalo de nuevo.',
                    );

                    this.changeDetectorRef.markForCheck();
                },
            });
    }

    confirmPasswordReset(): void {
        this.clearMessages();

        if (this.resetForm.invalid || this.loading) {
            this.resetForm.markAllAsTouched();
            return;
        }

        const token = String(
            this.resetForm.controls.token.value ?? '',
        ).trim();

        const newPassword = String(
            this.resetForm.controls.newPassword.value ?? '',
        );

        this.loading = true;

        this.authService
            .confirmPasswordReset(token, newPassword)
            .pipe(
                finalize(() => {
                    this.loading = false;
                    this.changeDetectorRef.markForCheck();
                }),
            )
            .subscribe({
                next: (response) => {
                    this.step = 'success';
                    this.successMessage =
                        response?.message ||
                        'La contraseña se ha actualizado correctamente. Ya puedes iniciar sesión.';

                    this.resetForm.disable();
                    this.changeDetectorRef.markForCheck();
                },
                error: (error: unknown) => {
                    this.errorMessage = this.getErrorMessage(
                        error,
                        'No se ha podido restablecer la contraseña. Comprueba el código e inténtalo de nuevo.',
                    );

                    this.changeDetectorRef.markForCheck();
                },
            });
    }

    private clearMessages(): void {
        this.errorMessage = null;
        this.successMessage = null;
    }

    private getErrorMessage(error: unknown, fallback: string): string {
        if (
            error instanceof HttpErrorResponse &&
            typeof error.error?.message === 'string'
        ) {
            return error.error.message;
        }

        return fallback;
    }
}
