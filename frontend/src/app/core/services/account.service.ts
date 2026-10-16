import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';

@Injectable({ providedIn: 'root' })
export class AccountService {
    private readonly http = inject(HttpClient);

    /** Elimina la cuenta del usuario autenticado (HU "Eliminar cuenta"). */
    deleteOwnAccount(): Observable<void> {
        return this.http.delete<void>('/api/users/me', { withCredentials: true });
    }
}