
import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';

export type PaymentMethodType = 'CARD' | 'PAYPAL' | 'BIZUM';

export interface PaymentMethod {
    id: string;
    type: PaymentMethodType;
    label: string | null;
    cardholderName: string | null;
    cardBrand: string | null;
    lastFourDigits: string | null;
    expirationMonth: number | null;
    expirationYear: number | null;
    paypalEmail: string | null;
    bizumPhone: string | null;
    isDefault: boolean;
}

export interface PaymentMethodRequest {
    type: PaymentMethodType;
    label?: string | null;
    cardholderName?: string | null;
    cardBrand?: string | null;
    lastFourDigits?: string | null;
    expirationMonth?: number | null;
    expirationYear?: number | null;
    paypalEmail?: string | null;
    bizumPhone?: string | null;
    isDefault: boolean;
}

@Injectable({ providedIn: 'root' })
export class PaymentMethodService {
    private readonly http = inject(HttpClient);
    private readonly url = '/api/users/me/payment-methods';

    list(): Observable<PaymentMethod[]> {
        return this.http.get<PaymentMethod[]>(this.url, {
            withCredentials: true,
        });
    }

    add(request: PaymentMethodRequest): Observable<PaymentMethod> {
        return this.http.post<PaymentMethod>(this.url, request, {
            withCredentials: true,
        });
    }

    update(
        id: string,
        request: PaymentMethodRequest,
    ): Observable<PaymentMethod> {
        return this.http.put<PaymentMethod>(
            `${this.url}/${encodeURIComponent(id)}`,
            request,
            { withCredentials: true },
        );
    }

    setDefault(id: string): Observable<PaymentMethod> {
        return this.http.put<PaymentMethod>(
            `${this.url}/${encodeURIComponent(id)}/default`,
            {},
            { withCredentials: true },
        );
    }

    delete(id: string): Observable<void> {
        return this.http.delete<void>(
            `${this.url}/${encodeURIComponent(id)}`,
            { withCredentials: true },
        );
    }
}
