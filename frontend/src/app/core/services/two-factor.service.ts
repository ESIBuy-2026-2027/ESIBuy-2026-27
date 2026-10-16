
import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface TwoFactorSetupResponse {
    secret: string;
    qrUri: string;
}

export interface TwoFactorEnableResponse {
    message?: string;
    backupCodes: string[];
}

export interface BackupCodesResponse {
    message?: string;
    backupCodes: string[];
}

export interface TwoFactorMessageResponse {
    message?: string;
}

export interface TwoFactorCodeRequest {
    code: string;
}

@Injectable({
    providedIn: 'root',
})
export class TwoFactorService {
    private readonly http = inject(HttpClient);
    private readonly apiUrl = '/api/users/2fa';

    setup(): Observable<TwoFactorSetupResponse> {
        return this.http.post<TwoFactorSetupResponse>(
            `${this.apiUrl}/setup`,
            {},
            { withCredentials: true },
        );
    }

    enable(code: string): Observable<TwoFactorEnableResponse> {
        return this.http.post<TwoFactorEnableResponse>(
            `${this.apiUrl}/enable`,
            { code },
            { withCredentials: true },
        );
    }

    regenerateBackupCodes(
        code: string,
    ): Observable<BackupCodesResponse> {
        return this.http.post<BackupCodesResponse>(
            `${this.apiUrl}/regenerate-backup-codes`,
            { code },
            { withCredentials: true },
        );
    }

    disable(code: string): Observable<TwoFactorMessageResponse> {
        return this.http.post<TwoFactorMessageResponse>(
            `${this.apiUrl}/disable`,
            { code },
            { withCredentials: true },
        );
    }

    getErrorMessage(
        error: {
            status?: number;
            error?: {
                message?: string;
                detail?: string;
                error?: string;
            };
        },
        fallback: string,
    ): string {
        return error.error?.detail
            ?? error.error?.message
            ?? error.error?.error
            ?? (
                error.status === 401
                    ? 'Tu sesión ha caducado. Inicia sesión de nuevo.'
                    : fallback
            );
    }
}
