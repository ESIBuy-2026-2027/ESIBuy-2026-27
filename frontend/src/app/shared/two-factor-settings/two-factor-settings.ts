
import { Component, EventEmitter, inject, Input, Output } from '@angular/core';
import { FormsModule } from '@angular/forms';
import QRCode from 'qrcode';
import {
    TwoFactorService,
    TwoFactorSetupResponse,
} from '../../core/services/two-factor.service';

@Component({
    selector: 'app-two-factor-settings',
    standalone: true,
    imports: [FormsModule],
    templateUrl: './two-factor-settings.html',
    styleUrl: './two-factor-settings.css',
})
export class TwoFactorSettingsComponent {
    private readonly twoFactorService = inject(TwoFactorService);

    @Input()
    set enabled(value: boolean) {
        this.twoFactorEnabled = value;
    }

    @Output() enabledChange = new EventEmitter<boolean>();

    twoFactorEnabled = false;
    twoFactorSetup: TwoFactorSetupResponse | null = null;

    twoFactorCode = '';
    recoveryCode = '';
    backupCodes: string[] = [];

    twoFactorBusy = false;
    twoFactorError = '';
    twoFactorSuccess = '';

    qrImage = '';
    qrGenerating = false;
    copiedSecret = false;

    /**
     * Formatea el código para mostrarlo en grupos de tres dígitos.
     * Ejemplo: 123456 -> 123 456.
     */
    formatVerificationCode(value: string): string {
        const digits = (value ?? '')
            .replace(/\D/g, '')
            .slice(0, 6);

        return digits.length > 3
            ? `${digits.slice(0, 3)} ${digits.slice(3)}`
            : digits;
    }

    /**
     * Elimina los espacios para enviar los seis dígitos al servicio.
     */
    private getVerificationCode(value: string): string {
        return value.replace(/\s/g, '');
    }

    private isValidVerificationCode(code: string): boolean {
        return /^\d{6}$/.test(code);
    }

    async setupTwoFactor(): Promise<void> {
        this.clearMessages();
        this.twoFactorBusy = true;
        this.qrGenerating = true;
        this.twoFactorSetup = null;
        this.qrImage = '';

        this.twoFactorService.setup().subscribe({
            next: (result) => {
                this.twoFactorSetup = result;

                void QRCode.toDataURL(result.qrUri, {
                    errorCorrectionLevel: 'M',
                    margin: 2,
                    width: 240,
                })
                    .then((qrImage) => {
                        this.qrImage = qrImage;
                    })
                    .catch(() => {
                        this.twoFactorError =
                            'No se ha podido generar el código QR. Puedes utilizar la clave manual.';
                    })
                    .finally(() => {
                        this.qrGenerating = false;
                        this.twoFactorBusy = false;
                    });
            },
            error: (error) => {
                this.twoFactorError = this.twoFactorService.getErrorMessage(
                    error,
                    'No se ha podido iniciar la configuración de la verificación en dos pasos.',
                );

                this.qrGenerating = false;
                this.twoFactorBusy = false;
            },
        });
    }

    enableTwoFactor(): void {
        const code = this.getVerificationCode(this.twoFactorCode);

        if (!this.isValidVerificationCode(code)) {
            this.twoFactorError =
                'Introduce los seis dígitos de tu aplicación de autenticación.';
            return;
        }

        this.clearMessages();
        this.twoFactorBusy = true;

        this.twoFactorService.enable(code).subscribe({
            next: (result) => {
                this.twoFactorEnabled = true;
                this.enabledChange.emit(true);
                this.backupCodes = result.backupCodes ?? [];
                this.twoFactorSetup = null;
                this.twoFactorCode = '';

                this.twoFactorSuccess =
                    result.message ??
                    'La verificación en dos pasos se ha activado correctamente.';

                this.twoFactorBusy = false;
            },
            error: (error) => {
                this.twoFactorError = this.twoFactorService.getErrorMessage(
                    error,
                    'No se ha podido activar la verificación en dos pasos.',
                );

                this.twoFactorBusy = false;
            },
        });
    }

    regenerateBackupCodes(): void {
        const code = this.getVerificationCode(this.recoveryCode);

        if (!this.isValidVerificationCode(code)) {
            this.twoFactorError =
                'Introduce los seis dígitos de tu aplicación de autenticación.';
            return;
        }

        this.clearMessages();
        this.twoFactorBusy = true;

        this.twoFactorService.regenerateBackupCodes(code).subscribe({
            next: (result) => {
                this.backupCodes = result.backupCodes ?? [];
                this.recoveryCode = '';

                this.twoFactorSuccess =
                    result.message ??
                    'Se han generado nuevos códigos de recuperación.';

                this.twoFactorBusy = false;
            },
            error: (error) => {
                this.twoFactorError = this.twoFactorService.getErrorMessage(
                    error,
                    'No se han podido regenerar los códigos de recuperación.',
                );

                this.twoFactorBusy = false;
            },
        });
    }

    disableTwoFactor(): void {
        const code = this.getVerificationCode(this.twoFactorCode);

        if (!this.isValidVerificationCode(code)) {
            this.twoFactorError =
                'Introduce los seis dígitos de tu aplicación de autenticación.';
            return;
        }

        this.clearMessages();
        this.twoFactorBusy = true;

        this.twoFactorService.disable(code).subscribe({
            next: (result) => {
                this.twoFactorEnabled = false;
                this.enabledChange.emit(false);
                this.twoFactorSetup = null;
                this.backupCodes = [];
                this.twoFactorCode = '';
                this.recoveryCode = '';

                this.twoFactorSuccess =
                    result.message ??
                    'La verificación en dos pasos se ha desactivado.';

                this.twoFactorBusy = false;
            },
            error: (error) => {
                this.twoFactorError = this.twoFactorService.getErrorMessage(
                    error,
                    'No se ha podido desactivar la verificación en dos pasos.',
                );

                this.twoFactorBusy = false;
            },
        });
    }

    async copyTwoFactorSecret(): Promise<void> {
        if (!this.twoFactorSetup?.secret) {
            return;
        }

        try {
            await navigator.clipboard.writeText(
                this.twoFactorSetup.secret,
            );

            this.copiedSecret = true;
            this.twoFactorSuccess = 'Clave copiada al portapapeles.';
        } catch {
            this.twoFactorError =
                'No se ha podido copiar la clave. Puedes copiarla manualmente.';
        }
    }

    downloadBackupCodes(): void {
        if (this.backupCodes.length === 0) {
            return;
        }

        const content = [
            'ESIBUY - Códigos de recuperación',
            '',
            'Guarda estos códigos en un lugar seguro.',
            'Cada código debe tratarse como una credencial secreta.',
            '',
            ...this.backupCodes,
            '',
        ].join('\n');

        const blob = new Blob([content], {
            type: 'text/plain;charset=utf-8',
        });

        const url = URL.createObjectURL(blob);
        const link = document.createElement('a');

        link.href = url;
        link.download = 'esibuy-codigos-recuperacion.txt';
        link.click();

        URL.revokeObjectURL(url);
    }

    private clearMessages(): void {
        this.twoFactorError = '';
        this.twoFactorSuccess = '';
        this.copiedSecret = false;
    }
}
