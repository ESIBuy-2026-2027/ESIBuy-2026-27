import { httpErrorMessage } from '../../../core/utils/http-error-message';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { Observable } from 'rxjs';
import { CartItem, CartResponse, CartService } from '../../../core/services/cart.service';
import { CheckoutSummary, ConfirmedOrder, PedidoService } from '../../../core/services/pedido.service';

export interface SellerCartGroup {
  vendedorId: string;
  vendedorNombre: string;
  items: CartItem[];
  subtotal: number;
  totalCount: number;
}

@Component({
  selector: 'app-client-cart',
  standalone: true,
  imports: [FormsModule, RouterLink],
  templateUrl: './cart.html',
  styleUrl: './cart.css',
})
export class ClientCart {
  readonly cartService = inject(CartService);
  private readonly pedidoService = inject(PedidoService);
  readonly busyItemIds = signal<ReadonlySet<string>>(new Set());
  readonly actionError = signal<string | null>(null);
  readonly checkoutOpen = signal(false);
  readonly checkoutLoading = signal(false);
  readonly processingOrder = signal(false);
  readonly checkoutSummary = signal<CheckoutSummary | null>(null);
  readonly orderConfirmation = signal<ConfirmedOrder | null>(null);
  readonly selectedAddressId = signal('');
  readonly selectedPaymentMethodId = signal('');
  private readonly currencyFormatter = new Intl.NumberFormat('es-ES', {
    style: 'currency',
    currency: 'EUR',
  });

  readonly sellerGroups = computed<SellerCartGroup[]>(() => {
    const cart = this.cartService.cart();
    if (!cart || !Array.isArray(cart.items) || cart.items.length === 0) {
      return [];
    }
    const map = new Map<string, SellerCartGroup>();
    for (const item of cart.items) {
      const sellerId = item.vendedorId || 'default-seller';
      const sellerName =
        item.vendedorNombre ||
        (item.vendedorId ? `Vendedor ${item.vendedorId}` : 'Vendedor general');
      let group = map.get(sellerId);
      if (!group) {
        group = {
          vendedorId: sellerId,
          vendedorNombre: sellerName,
          items: [],
          subtotal: 0,
          totalCount: 0,
        };
        map.set(sellerId, group);
      }
      group.items.push(item);
      group.subtotal = Number((group.subtotal + item.subtotal).toFixed(2));
      group.totalCount += item.cantidad;
    }
    return Array.from(map.values());
  });

  readonly hasMultipleSellers = computed(() => this.sellerGroups().length > 1);

  readonly sellerNamesList = computed(() =>
    this.sellerGroups()
      .map((group) => group.vendedorNombre)
      .join(', '),
  );

  readonly singleSellerName = computed(() => {
    const groups = this.sellerGroups();
    return groups.length === 1 ? groups[0].vendedorNombre : null;
  });

  formatPrice(price: number): string {
    return this.currencyFormatter.format(price);
  }

  changeQuantity(productoId: string, cantidad: number): void {
    this.runItemAction(
      productoId,
      () => this.cartService.updateItemQuantity(productoId, cantidad),
    );
  }

  removeItem(productoId: string): void {
    this.runItemAction(productoId, () => this.cartService.removeItem(productoId));
  }

  iniciarCheckout(): void {
    if (this.checkoutLoading() || this.processingOrder()) {
      return;
    }
    if (this.hasMultipleSellers()) {
      this.actionError.set(
        'No se puede realizar un pedido con productos de diferentes vendedores en la misma compra. Debes tramitar pedidos independientes para cada vendedor.',
      );
      return;
    }
    this.actionError.set(null);
    this.checkoutLoading.set(true);
    this.pedidoService.obtenerCheckoutResumen().subscribe({
      next: (summary) => {
        this.checkoutSummary.set(summary);
        this.selectedAddressId.set(
          summary.direcciones.find((address) => address.isDefault)?.id
          ?? summary.direcciones[0]?.id
          ?? '',
        );
        this.selectedPaymentMethodId.set(
          summary.metodosPago.find((method) => method.defaultMethod)?.id
          ?? summary.metodosPago[0]?.id
          ?? '',
        );
        this.checkoutOpen.set(true);
        this.checkoutLoading.set(false);
      },
      error: (error: unknown) => {
        this.actionError.set(this.getOrderErrorMessage(error));
        this.checkoutLoading.set(false);
      },
    });
  }

  cancelarCheckout(): void {
    if (this.processingOrder()) {
      return;
    }
    this.checkoutOpen.set(false);
    this.checkoutSummary.set(null);
    this.actionError.set(null);
  }

  confirmarPedido(): void {
    if (this.processingOrder()) {
      return;
    }
    if (this.hasMultipleSellers()) {
      this.actionError.set(
        'No se puede realizar un pedido con productos de diferentes vendedores en la misma compra. Debes tramitar pedidos independientes para cada vendedor.',
      );
      return;
    }
    const direccionId = this.selectedAddressId();
    const metodoPagoId = this.selectedPaymentMethodId();
    if (!direccionId || !metodoPagoId) {
      this.actionError.set('Selecciona una dirección de envío y un método de pago guardados.');
      return;
    }
    this.actionError.set(null);
    this.processingOrder.set(true);
    this.pedidoService.crearPedido(direccionId, metodoPagoId).subscribe({
      next: (order) => {
        this.orderConfirmation.set(order);
        this.checkoutOpen.set(false);
        this.checkoutSummary.set(null);
        this.processingOrder.set(false);
        this.cartService.clear();
      },
      error: (error: unknown) => {
        this.actionError.set(this.getOrderErrorMessage(error));
        this.processingOrder.set(false);
      },
    });
  }

  private getOrderErrorMessage(error: unknown): string {
    if (error instanceof HttpErrorResponse) {
      if (typeof error.error?.message === 'string') {
        return error.error.message;
      }
      if (error.status === 0) {
        return httpErrorMessage(error) ?? 'No se pudo completar el pedido. Inténtalo de nuevo.';
      }
    }
    return 'No se pudo completar el pedido. Inténtalo de nuevo.';
  }

  private runItemAction(productoId: string, action: () => Observable<CartResponse>): void {
    if (this.busyItemIds().has(productoId)) {
      return;
    }

    this.actionError.set(null);
    this.setItemBusy(productoId, true);
    action().subscribe({
      error: () => {
        this.actionError.set('No se pudo actualizar el carrito. Inténtalo de nuevo.');
        this.setItemBusy(productoId, false);
      },
      complete: () => this.setItemBusy(productoId, false),
    });
  }

  private setItemBusy(productoId: string, busy: boolean): void {
    const busyItemIds = new Set(this.busyItemIds());
    if (busy) {
      busyItemIds.add(productoId);
    } else {
      busyItemIds.delete(productoId);
    }
    this.busyItemIds.set(busyItemIds);
  }
}
