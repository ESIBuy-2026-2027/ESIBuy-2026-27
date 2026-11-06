import { Component, HostListener, OnInit, inject, signal } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
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
import { finalize } from 'rxjs';

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
  readonly cancellingOrder = signal(false);
  readonly cancellationDialogOpen = signal(false);
  readonly orderToCancel = signal<CustomerOrder | null>(null);
  readonly confirmingReceipt = signal(false);
  readonly receiptDialogOpen = signal(false);
  readonly orderToConfirmReceipt = signal<CustomerOrder | null>(null);
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
    this.loadOrder(pedidoId);
  }

  loadOrder(pedidoId: string): void {
    this.loading.set(true);
    this.error.set(null);
    this.pedidoService.obtenerPedidoDelCliente(pedidoId).subscribe({
      next: (order) => {
        this.order.set(order);
        this.loading.set(false);
      },
      error: () => {
        this.error.set('No se pudo cargar el detalle del pedido.');
        this.loading.set(false);
      },
    }).add(() => this.loading.set(false));
  }

  retryLoadOrder(): void {
    const pedidoId = this.route.snapshot.paramMap.get('pedidoId');
    if (pedidoId) {
      this.loadOrder(pedidoId);
    }
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

  canCancelOrder(order: CustomerOrder): boolean {
    return order.estado === 'PENDIENTE' && getCustomerOrderPaymentStatus(order) === 'PAGADO';
  }

  canConfirmReceipt(order: CustomerOrder): boolean {
    return this.getFulfillmentStatus(order) === 'ENVIADO';
  }

  openReceiptDialog(order: CustomerOrder): void {
    if (this.confirmingReceipt() || !this.canConfirmReceipt(order)) {
      return;
    }

    this.actionError.set(null);
    this.orderToConfirmReceipt.set(order);
    this.receiptDialogOpen.set(true);
  }

  @HostListener('document:keydown.escape')
  closeOpenDialog(): void {
    this.closeCancellationDialog();
    this.closeReceiptDialog();
  }

  closeReceiptDialog(): void {
    if (this.confirmingReceipt()) {
      return;
    }
    this.receiptDialogOpen.set(false);
    this.orderToConfirmReceipt.set(null);
  }

  confirmReceipt(): void {
    const order = this.orderToConfirmReceipt();
    if (this.confirmingReceipt() || !order || !this.canConfirmReceipt(order)) {
      return;
    }

    this.actionError.set(null);
    this.confirmingReceipt.set(true);
    this.pedidoService.confirmarRecepcionPedido(order.pedidoId)
      .pipe(finalize(() => this.confirmingReceipt.set(false)))
      .subscribe({
        next: () => {
          this.order.set({ ...order, estado: 'ENTREGADO' });
          this.receiptDialogOpen.set(false);
          this.orderToConfirmReceipt.set(null);
        },
        error: (error: HttpErrorResponse) => this.actionError.set(
          this.getReceiptConfirmationErrorMessage(error),
        ),
      });
  }

  openCancellationDialog(order: CustomerOrder): void {
    if (this.cancellingOrder() || !this.canCancelOrder(order)) {
      return;
    }

    this.actionError.set(null);
    this.orderToCancel.set(order);
    this.cancellationDialogOpen.set(true);
  }

  closeCancellationDialog(): void {
    if (this.cancellingOrder()) {
      return;
    }
    this.cancellationDialogOpen.set(false);
    this.orderToCancel.set(null);
  }

  confirmCancellation(): void {
    const order = this.orderToCancel();
    if (this.cancellingOrder() || !order || !this.canCancelOrder(order)) {
      return;
    }

    this.actionError.set(null);
    this.cancellingOrder.set(true);
    this.pedidoService.cancelarPedido(order.pedidoId)
      .pipe(finalize(() => this.cancellingOrder.set(false)))
      .subscribe({
        next: () => {
          this.order.set({ ...order, estado: 'CANCELADO' });
          this.cancellationDialogOpen.set(false);
          this.orderToCancel.set(null);
        },
        error: (error: HttpErrorResponse) => this.actionError.set(
          this.getCancellationErrorMessage(error),
        ),
      });
  }

  private getCancellationErrorMessage(error: HttpErrorResponse): string {
    const message = error.error?.message;
    if (typeof message === 'string' && message.trim()) {
      return message;
    }
    return 'No se pudo cancelar el pedido. Puede que ya no sea cancelable.';
  }

  private getReceiptConfirmationErrorMessage(error: HttpErrorResponse): string {
    const message = error.error?.message;
    if (typeof message === 'string' && message.trim()) {
      return message;
    }
    return 'No se pudo confirmar la recepción. Puede que el pedido ya no esté enviado.';
  }

  buyAgain(productId: string, quantity: number): void {
    if (this.addingProductId()) {
      return;
    }

    this.actionError.set(null);
    this.addingProductId.set(productId);
    this.cartService.addItem(productId, quantity).pipe(finalize(() => this.addingProductId.set(null))).subscribe({
      next: () => this.addingProductId.set(null),
      error: () => {
        this.actionError.set('No se pudo añadir el producto al carrito. Puede que ya no esté disponible.');
      },
    });
  }

  viewProduct(productId: string): void {
    void this.router.navigate(['/products', productId]);
  }
}
