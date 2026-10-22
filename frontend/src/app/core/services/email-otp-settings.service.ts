
import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface EmailOtpSettings {
    enabled: boolean;
    email: string;
}

export interface UpdateEmailOtpSettingsResponse {
    enabled: boolean;
    email: string;
    message: string;
}

export interface EmailOtpChallengeResponse {
    message: string;
    expiresInSeconds: number;
}

@Injectable({
    providedIn: 'root',
})
export class EmailOtpSettingsService {
    private readonly http = inject(HttpClient);
    private readonly apiUrl = '/api/users/me/email-otp-settings';

    getSettings(): Observable<EmailOtpSettings> {
        return this.http.get<EmailOtpSettings>(this.apiUrl, {
            withCredentials: true,
        });
    }

    updateEnabled(
        enabled: boolean,
    ): Observable<UpdateEmailOtpSettingsResponse> {
        return this.http.put<UpdateEmailOtpSettingsResponse>(
            this.apiUrl,
            { enabled },
            { withCredentials: true },
        );
    }

    requestDisableChallenge(): Observable<EmailOtpChallengeResponse> {
        return this.http.post<EmailOtpChallengeResponse>(
            `${this.apiUrl}/disable-challenge`,
            {},
            { withCredentials: true },
        );
    }

    confirmDisable(
        code: string,
    ): Observable<UpdateEmailOtpSettingsResponse> {
        return this.http.post<UpdateEmailOtpSettingsResponse>(
            `${this.apiUrl}/disable`,
            { code },
            { withCredentials: true },
        );
    }
}
