import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { CartItem } from './cart.service';

export interface ShippingAddressSummary {
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

export interface PaymentMethodSummary {
  id: string;
  type: string;
  cardBrand: string | null;
  lastFourDigits: string | null;
  defaultMethod: boolean;
}

export interface CheckoutSummary {
  direcciones: ShippingAddressSummary[];
  metodosPago: PaymentMethodSummary[];
  items: CartItem[];
  precioTotal: number;
}

export interface ConfirmedOrder {
  pedidoId: string;
  precioTotal: number;
  estado: 'PAGADO' | 'PENDIENTE' | 'CANCELADO';
  fechaCreacion: string;
}

export interface SellerOrderItem {
  productoId: string;
  nombre: string;
  cantidad: number;
  precioUnitario: number;
  subtotal: number;
  imageUrl: string | null;
}

export type SellerOrderStatus = 'PAGADO' | 'PENDIENTE' | 'PREPARANDO' | 'ENVIADO' | 'ENTREGADO' | 'CANCELADO';

export interface SellerOrder {
  pedidoId: string;
  fechaCreacion: string;
  clienteNombre: string;
  clienteEmail: string | null;
  direccionEnvio: string;
  items: SellerOrderItem[];
  total: number;
  estado: SellerOrderStatus;
}

@Injectable({ providedIn: 'root' })
export class PedidoService {
  private readonly http = inject(HttpClient);

  obtenerCheckoutResumen(): Observable<CheckoutSummary> {
    return this.http.get<CheckoutSummary>('/api/pedidos/checkout-resumen');
  }

  crearPedido(direccionId: string, metodoPagoId: string): Observable<ConfirmedOrder> {
    return this.http.post<ConfirmedOrder>('/api/pedidos', { direccionId, metodoPagoId });
  }

  obtenerPedidosDelVendedor(): Observable<SellerOrder[]> {
    return this.http.get<SellerOrder[]>('/api/pedidos/vendedor');
  }

  actualizarEstadoPedido(
    pedidoId: string,
    estado: SellerOrderStatus,
    justificacion?: string,
  ): Observable<void> {
    return this.http.patch<void>(`/api/pedidos/vendedor/${pedidoId}/estado`, { estado, justificacion });
  }
}
