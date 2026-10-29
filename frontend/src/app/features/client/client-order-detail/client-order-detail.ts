import { Component, OnInit, inject, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { CartService } from '../../../core/services/cart.service';
import {
  CustomerOrder,
  CustomerOrderFulfillmentStatus,
  getCustomerOrderFulfillmentStatus,
  getCustomerOrderPaymentStatus,
  PedidoService,
} from '../../../core/services/pedido.service';
import { ClientShipmentTracking } from '../client-shipment-tracking/client-shipment-tracking';

@Component({
  selector: 'app-client-order-detail',
  standalone: true,
  imports: [RouterLink, ClientShipmentTracking],
  templateUrl: './client-order-detail.html',
  styleUrl: './client-order-detail.css',
})
export class ClientOrderDetail implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly pedidoService = inject(PedidoService);
  private readonly cartService = inject(CartService);
  readonly order = signal<CustomerOrder | null>(null);
  readonly loading = signal(true);
  readonly error = signal<string | null>(null);
  readonly actionError = signal<string | null>(null);
  readonly addingProductId = signal<string | null>(null);
  private readonly currencyFormatter = new Intl.NumberFormat('es-ES', {
    style: 'currency',
    currency: 'EUR',
  });

  ngOnInit(): void {
    const pedidoId = this.route.snapshot.paramMap.get('pedidoId');
    if (!pedidoId) {
      this.error.set('No se ha indicado un pedido.');
      this.loading.set(false);
      return;
    }
    this.pedidoService.obtenerPedidoDelCliente(pedidoId).subscribe({
      next: (order) => {
        this.order.set(order);
        this.loading.set(false);
      },
      error: () => {
        this.error.set('No se pudo cargar el detalle del pedido.');
        this.loading.set(false);
      },
    });
  }

  formatPrice(price: number): string {
    return this.currencyFormatter.format(price);
  }

  formatDate(date: string): string {
    return new Intl.DateTimeFormat('es-ES', { dateStyle: 'medium', timeStyle: 'short' }).format(new Date(date));
  }

  getItemsSubtotal(order: CustomerOrder): number {
    return order.items.reduce((subtotal, item) => subtotal + item.subtotal, 0);
  }

  getFulfillmentStatus(order: CustomerOrder): CustomerOrderFulfillmentStatus {
    return getCustomerOrderFulfillmentStatus(order);
  }

  getFulfillmentStatusLabel(order: CustomerOrder): string {
    const labels: Record<CustomerOrderFulfillmentStatus, string> = {
      PENDIENTE: 'Pendiente',
      PREPARANDO: 'En preparación',
      ENVIADO: 'Enviado',
      ENTREGADO: 'Entregado',
      CANCELADO: 'Cancelado',
    };
    return labels[this.getFulfillmentStatus(order)];
  }

  getPaymentLabel(order: CustomerOrder): string {
    return getCustomerOrderPaymentStatus(order) === 'PAGADO' ? 'Pagado' : 'Pendiente';
  }

  getPaymentClass(order: CustomerOrder): string {
    return getCustomerOrderPaymentStatus(order) === 'PAGADO' ? 'payment-paid' : 'payment-pending';
  }

  buyAgain(productId: string, quantity: number): void {
    if (this.addingProductId()) {
      return;
    }

    this.actionError.set(null);
    this.addingProductId.set(productId);
    this.cartService.addItem(productId, quantity).subscribe({
      next: () => this.addingProductId.set(null),
      error: () => {
        this.actionError.set('No se pudo añadir el producto al carrito. Puede que ya no esté disponible.');
        this.addingProductId.set(null);
      },
    });
  }

  viewProduct(productId: string): void {
    void this.router.navigate(['/products', productId]);
  }
}
