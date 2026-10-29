
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

type RecoveryStep = 'request' | 'validating' | 'password' | 'success';

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
                // El token procede exclusivamente del enlace del correo.
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

        if (this.tokenFromUrl) {
            this.step = 'validating';
            this.validateRecoveryToken();
        }
    }

    validateRecoveryToken(): void {
        const token = this.tokenFromUrl.trim();

        if (!token || this.loading) {
            return;
        }

        this.clearMessages();
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
                    this.resetForm.controls.token.setValue(token);
                    this.step = 'password';
                    this.successMessage =
                        response?.message ||
                        'El enlace es válido. Ya puedes establecer una nueva contraseña.';

                    this.changeDetectorRef.markForCheck();
                },
                error: (error: unknown) => {
                    this.step = 'request';
                    this.requestSubmitted = false;
                    this.errorMessage = this.getErrorMessage(
                        error,
                        'El enlace no es válido o ha caducado. Solicita uno nuevo e inténtalo de nuevo.',
                    );

                    this.changeDetectorRef.markForCheck();
                },
            });
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

    allowAnotherRequest(): void {
        if (this.loading) {
            return;
        }

        this.requestSubmitted = false;
        this.clearMessages();
    }

    togglePasswordVisibility(): void {
        this.showPassword = !this.showPassword;
    }

    confirmPasswordReset(): void {
        this.clearMessages();

        if (this.resetForm.invalid || this.loading) {
            this.resetForm.markAllAsTouched();
            return;
        }

        const token = this.tokenFromUrl.trim();
        const newPassword = String(
            this.resetForm.controls.newPassword.value ?? '',
        );

        if (!token) {
            this.step = 'request';
            this.errorMessage =
                'No se ha encontrado un enlace de recuperación válido. Solicita uno nuevo.';
            return;
        }

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
                        'No se ha podido restablecer la contraseña. El enlace puede haber caducado o ya haberse utilizado.',
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
