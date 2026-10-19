import { HttpClient } from '@angular/common/http';
import { computed, inject, Injectable, signal } from '@angular/core';
import { Observable, tap } from 'rxjs';

export interface CartItem {
  productoId: string;
  nombre: string;
  precioUnitario: number;
  cantidad: number;
  subtotal: number;
  imageUrl?: string;
}

export interface CartResponse {
  id: string | null;
  clienteId: string;
  items: CartItem[];
  precioTotal: number;
}

@Injectable({ providedIn: 'root' })
export class CartService {
  private readonly http = inject(HttpClient);

  readonly cart = signal<CartResponse | null>(null);
  readonly loading = signal(false);
  readonly loadError = signal<string | null>(null);
  readonly itemCount = computed(() => this.cart()?.items.reduce(
    (total, item) => total + item.cantidad,
    0,
  ) ?? 0);

  load(): void {
    this.cart.set(null);
    this.loading.set(true);
    this.loadError.set(null);
    this.http.get<CartResponse>('/api/carrito').subscribe({
      next: (cart) => {
        this.cart.set(cart);
        this.loading.set(false);
      },
      error: () => {
        this.loading.set(false);
        this.loadError.set('No se pudo cargar el carrito. Inténtalo de nuevo.');
      },
    });
  }

  addItem(productoId: string, cantidad: number): Observable<CartResponse> {
    return this.http.post<CartResponse>('/api/carrito/items', { productoId, cantidad }).pipe(
      tap((cart) => this.cart.set(cart)),
    );
  }

  updateItemQuantity(productoId: string, cantidad: number): Observable<CartResponse> {
    return this.http.put<CartResponse>(
      `/api/carrito/items/${encodeURIComponent(productoId)}`,
      { cantidad },
    ).pipe(tap((cart) => this.cart.set(cart)));
  }

  removeItem(productoId: string): Observable<CartResponse> {
    return this.http.delete<CartResponse>(
      `/api/carrito/items/${encodeURIComponent(productoId)}`,
    ).pipe(tap((cart) => this.cart.set(cart)));
  }

  clear(): void {
    this.cart.set(null);
    this.loading.set(false);
    this.loadError.set(null);
  }
}
