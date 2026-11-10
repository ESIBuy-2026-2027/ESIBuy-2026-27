import {
    ChangeDetectorRef,
    Component,
    OnInit,
    inject,
} from '@angular/core';
import { FormsModule } from '@angular/forms';
import { finalize } from 'rxjs';
import { EmailOtpSettingsService } from '../../core/services/email-otp-settings.service';

type EmailOtpHttpError = {
    status?: number;
    error?: {
        message?: string;
        detail?: string;
        error?: string;
    };
};

@Component({
    selector: 'app-email-otp-settings',
    standalone: true,
    imports: [FormsModule],
    templateUrl: './email-otp-settings.html',
    styleUrl: './email-otp-settings.css',
})
export class EmailOtpSettingsComponent implements OnInit {
    private readonly cdr = inject(ChangeDetectorRef);
    private readonly emailOtpService = inject(EmailOtpSettingsService);

    email = '';
    enabled = false;
    totpEnabled = false;
    loading = true;
    saving = false;
    requestingCode = false;
    awaitingCode = false;
    verificationCode = '';
    error = '';
    success = '';

    ngOnInit(): void {
        this.loadSettings();
    }

    loadSettings(): void {
        this.loading = true;
        this.error = '';
        this.success = '';
        this.emailOtpService.getSettings()
            .pipe(finalize(() => {
                this.loading = false;
                this.cdr.detectChanges();
            }))
            .subscribe({
                next: (settings) => {
                    this.email = settings.email ?? '';
                    this.enabled = settings.enabled;
                    this.totpEnabled = settings.totpEnabled === true;
                    this.cdr.detectChanges();
                },
                error: (err: EmailOtpHttpError) => {
                    this.error = this.getErrorMessage(err, 'No se ha podido cargar la configuración de seguridad.');
                    this.cdr.detectChanges();
                },
            });
    }

    enableEmailOtp(): void {
        if (this.loading || this.saving || this.enabled) return;
        if (!this.totpEnabled) {
            this.error = 'Activa primero la verificación en dos pasos con una aplicación Authenticator.';
            this.success = '';
            return;
        }

        this.error = '';
        this.success = '';
        this.saving = true;
        this.emailOtpService.updateEnabled(true)
            .pipe(finalize(() => {
                this.saving = false;
                this.cdr.detectChanges();
            }))
            .subscribe({
                next: (response) => {
                    this.enabled = response.enabled;
                    this.email = response.email ?? this.email;
                    this.success = response.message || 'La verificación por correo se ha activado correctamente.';
                    this.cdr.detectChanges();
                },
                error: (err: EmailOtpHttpError) => {
                    this.error = this.getErrorMessage(err, 'No se ha podido activar la verificación por correo.');
                    this.cdr.detectChanges();
                },
            });
    }

    disableEmailOtp(): void {
        if (this.loading || this.saving || this.requestingCode || !this.enabled || this.awaitingCode) return;
        this.error = '';
        this.success = '';
        this.requestingCode = true;
        this.emailOtpService.requestDisableChallenge()
            .pipe(finalize(() => {
                this.requestingCode = false;
                this.cdr.detectChanges();
            }))
            .subscribe({
                next: (response) => {
                    this.awaitingCode = true;
                    this.verificationCode = '';
                    this.success = response.message || 'Hemos enviado un código de verificación a tu correo.';
                    this.cdr.detectChanges();
                },
                error: (err: EmailOtpHttpError) => {
                    this.error = this.getErrorMessage(err, 'No se ha podido enviar el código. Inténtalo de nuevo.');
                    this.cdr.detectChanges();
                },
            });
    }

    confirmDisableEmailOtp(): void {
        const code = this.verificationCode.replace(/\s/g, '');
        if (this.saving || !this.awaitingCode || !/^\d{6}$/.test(code)) {
            this.error = 'Introduce un código válido de 6 cifras.';
            return;
        }
        this.error = '';
        this.success = '';
        this.saving = true;
        this.emailOtpService.confirmDisable(code)
            .pipe(finalize(() => {
                this.saving = false;
                this.cdr.detectChanges();
            }))
            .subscribe({
                next: (response) => {
                    this.enabled = response.enabled;
                    this.email = response.email ?? this.email;
                    this.awaitingCode = false;
                    this.verificationCode = '';
                    this.success = response.message || 'La verificación por correo se ha desactivado correctamente.';
                    this.cdr.detectChanges();
                },
                error: (err: EmailOtpHttpError) => {
                    this.error = this.getErrorMessage(err, 'El código no es válido o ha caducado. Comprueba el correo e inténtalo de nuevo.');
                    this.cdr.detectChanges();
                },
            });
    }

    cancelDisableEmailOtp(): void {
        if (this.saving || this.requestingCode) return;
        this.awaitingCode = false;
        this.verificationCode = '';
        this.error = '';
        this.success = '';
    }

    formatVerificationCode(value: string): string {
        const digits = (value ?? '').replace(/\D/g, '').slice(0, 6);
        return digits.length > 3 ? `${digits.slice(0, 3)} ${digits.slice(3)}` : digits;
    }

    private getErrorMessage(error: EmailOtpHttpError, fallback: string): string {
        return error.error?.message ?? error.error?.detail ?? error.error?.error ?? fallback;
    }
}
