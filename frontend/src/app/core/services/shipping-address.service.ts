import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';

export interface ShippingAddress {
  id: string;
  recipientName: string;
  street: string;
  additionalInfo: string | null;
  postalCode: string;
  city: string;
  province: string;
  phone: string | null;
  isDefault: boolean;
}

/** Datos que envía el cliente al crear una dirección (el id lo genera el servidor). */
export type ShippingAddressRequest = Omit<ShippingAddress, 'id'>;

@Injectable({ providedIn: 'root' })
export class ShippingAddressService {
  private readonly http = inject(HttpClient);
  private readonly url = '/api/users/me/addresses';

  list(): Observable<ShippingAddress[]> {
    return this.http.get<ShippingAddress[]>(this.url, { withCredentials: true });
  }

  add(request: ShippingAddressRequest): Observable<ShippingAddress> {
    return this.http.post<ShippingAddress>(this.url, request, { withCredentials: true });
  }
}
