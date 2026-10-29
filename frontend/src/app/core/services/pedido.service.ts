import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable, map } from 'rxjs';
import { CartItem } from './cart.service';
import { resolveImageUrl } from '../utils/image.util';

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

export interface CustomerOrderItem {
  productoId: string;
  nombre: string;
  cantidad: number;
  precioUnitario: number;
  subtotal: number;
  imageUrl: string | null;
}

export type CustomerOrderStatus = SellerOrderStatus | 'PAGADO';

export interface CustomerOrder {
  pedidoId: string;
  fechaCreacion: string;
  items: CustomerOrderItem[];
  total: number;
  estado: CustomerOrderStatus;
  direccionEnvio: CustomerShippingAddress | null;
  metodoPago: CustomerPaymentMethod | null;
}

export interface CustomerShippingAddress {
  recipientName: string;
  street: string;
  additionalInfo: string | null;
  postalCode: string;
  city: string;
  province: string;
  phone: string | null;
}

export interface CustomerPaymentMethod {
  tipo: 'CARD' | 'PAYPAL' | 'BIZUM';
  lastFourDigits: string | null;
}

export interface EstadoPedidoHistorial {
  estado: CustomerOrderStatus;
  fecha: string;
}

export interface SeguimientoEnvio {
  historialEstados: EstadoPedidoHistorial[];
  estimatedDeliveryDate: string | null;
}

@Injectable({ providedIn: 'root' })
export class PedidoService {
  private readonly http = inject(HttpClient);

  obtenerCheckoutResumen(vendedorId?: string, productoIds?: string[]): Observable<CheckoutSummary> {
    const params: Record<string, string | string[]> = {};
    if (vendedorId) {
      params['vendedorId'] = vendedorId;
    }
    if (productoIds && productoIds.length > 0) {
      params['productoIds'] = productoIds;
    }
    const options = Object.keys(params).length > 0 ? { params } : {};
    return this.http.get<CheckoutSummary>('/api/pedidos/checkout-resumen', options).pipe(
      map((summary) => this.normalizeSummary(summary)),
    );
  }

  crearPedido(
    direccionId: string,
    metodoPagoId: string,
    vendedorId?: string,
    productoIds?: string[],
  ): Observable<ConfirmedOrder> {
    const payload: {
      direccionId: string;
      metodoPagoId: string;
      vendedorId?: string;
      productoIds?: string[];
    } = {
      direccionId,
      metodoPagoId,
    };
    if (vendedorId) {
      payload.vendedorId = vendedorId;
    }
    if (productoIds && productoIds.length > 0) {
      payload.productoIds = productoIds;
    }
    return this.http.post<ConfirmedOrder>('/api/pedidos', payload);
  }

  obtenerPedidosDelCliente(): Observable<CustomerOrder[]> {
    return this.http.get<CustomerOrder[]>('/api/pedidos');
  }

  obtenerPedidoDelCliente(pedidoId: string): Observable<CustomerOrder> {
    return this.http.get<CustomerOrder>(`/api/pedidos/${pedidoId}`);
  }

  obtenerHistorialEstadosEnvio(pedidoId: string): Observable<SeguimientoEnvio> {
    return this.http.get<SeguimientoEnvio>(`/api/pedidos/${pedidoId}/historial-estados`);
  }

  obtenerPedidosDelVendedor(): Observable<SellerOrder[]> {
    return this.http.get<SellerOrder[]>('/api/pedidos/vendedor').pipe(
      map((orders) =>
        orders.map((order) => ({
          ...order,
          items: order.items.map((item) => ({
            ...item,
            imageUrl: resolveImageUrl(item.imageUrl),
          })),
        }))
      )
    );
  }

  actualizarEstadoPedido(
    pedidoId: string,
    estado: SellerOrderStatus,
    justificacion?: string,
  ): Observable<void> {
    return this.http.patch<void>(`/api/pedidos/vendedor/${pedidoId}/estado`, { estado, justificacion });
  }

  private normalizeSummary(summary: CheckoutSummary): CheckoutSummary {
    if (!summary || !Array.isArray(summary.items)) {
      return summary;
    }
    return {
      ...summary,
      items: summary.items.map((item) => ({
        ...item,
        imageUrl: resolveImageUrl(item.imageUrl) ?? undefined,
      })),
    };
  }
}
