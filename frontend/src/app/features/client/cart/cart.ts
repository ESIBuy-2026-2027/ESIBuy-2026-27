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
  readonly userSelectedProductIds = signal<ReadonlySet<string> | null>(null);

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

  readonly selectedProductIds = computed<ReadonlySet<string>>(() => {
    const manual = this.userSelectedProductIds();
    const cart = this.cartService.cart();
    const validIds = new Set(cart?.items.map((i) => i.productoId) ?? []);

    if (manual !== null) {
      return new Set(Array.from(manual).filter((id) => validIds.has(id)));
    }

    const groups = this.sellerGroups();
    // Si solo hay un único vendedor en el carrito, se preseleccionan sus artículos
    if (groups.length === 1) {
      return new Set(groups[0].items.map((item) => item.productoId));
    }

    // Si hay múltiples vendedores, el cliente debe seleccionar manualmente
    return new Set();
  });

  readonly selectedSellerIds = computed<Set<string>>(() => {
    const selected = new Set<string>();
    const selectedIds = this.selectedProductIds();
    for (const group of this.sellerGroups()) {
      if (group.items.some((item) => selectedIds.has(item.productoId))) {
        selected.add(group.vendedorId);
      }
    }
    return selected;
  });

  readonly hasMixedSelection = computed(() => this.selectedSellerIds().size > 1);

  readonly selectedVendor = computed<SellerCartGroup | null>(() => {
    const sellerIds = this.selectedSellerIds();
    if (sellerIds.size !== 1) {
      return null;
    }
    const targetId = Array.from(sellerIds)[0];
    return this.sellerGroups().find((g) => g.vendedorId === targetId) ?? null;
  });

  readonly selectedTotal = computed<number | null>(() => {
    if (this.hasMixedSelection()) {
      return null; // Se quita el precio
    }
    const cart = this.cartService.cart();
    if (!cart?.items) {
      return 0;
    }
    const selectedIds = this.selectedProductIds();
    const sum = cart.items
      .filter((item) => selectedIds.has(item.productoId))
      .reduce((total, item) => total + item.subtotal, 0);
    return Number(sum.toFixed(2));
  });

  readonly selectedItemCount = computed<number | null>(() => {
    if (this.hasMixedSelection()) {
      return null;
    }
    const cart = this.cartService.cart();
    if (!cart?.items) {
      return 0;
    }
    const selectedIds = this.selectedProductIds();
    return cart.items
      .filter((item) => selectedIds.has(item.productoId))
      .reduce((total, item) => total + item.cantidad, 0);
  });

  setSelectedProductIds(ids: Iterable<string>): void {
    this.userSelectedProductIds.set(new Set(ids));
  }

  isProductSelected(productoId: string): boolean {
    return this.selectedProductIds().has(productoId);
  }

  isSellerDisabled(vendedorId: string): boolean {
    const selectedSellers = this.selectedSellerIds();
    if (selectedSellers.size === 0) {
      return false;
    }
    return !selectedSellers.has(vendedorId);
  }

  readonly canCheckout = computed<boolean>(() => {
    const count = this.selectedItemCount();
    const sellerIds = this.selectedSellerIds();
    return (
      !this.checkoutLoading() &&
      !this.processingOrder() &&
      count !== null &&
      count > 0 &&
      sellerIds.size === 1 &&
      !this.hasMixedSelection()
    );
  });

  isSellerFullySelected(group: SellerCartGroup): boolean {
    if (group.items.length === 0) {
      return false;
    }
    const selected = this.selectedProductIds();
    return group.items.every((item) => selected.has(item.productoId));
  }

  isSellerPartiallySelected(group: SellerCartGroup): boolean {
    const selected = this.selectedProductIds();
    const some = group.items.some((item) => selected.has(item.productoId));
    return some && !this.isSellerFullySelected(group);
  }

  toggleProductSelection(item: CartItem, group: SellerCartGroup): void {
    if (this.isSellerDisabled(group.vendedorId)) {
      const currentSellerNames = this.sellerGroups()
        .filter((g) => this.selectedSellerIds().has(g.vendedorId))
        .map((g) => g.vendedorNombre)
        .join(', ');
      this.actionError.set(
        `No puedes realizar pedidos de diferente vendedor a la vez. Deselecciona primero los productos de "${currentSellerNames}" si deseas comprar a "${group.vendedorNombre}".`,
      );
      return;
    }

    const selected = new Set(this.selectedProductIds());
    const isCurrentlySelected = selected.has(item.productoId);

    if (!isCurrentlySelected) {
      this.actionError.set(null);
      selected.add(item.productoId);
    } else {
      this.actionError.set(null);
      selected.delete(item.productoId);
    }
    this.userSelectedProductIds.set(selected);
  }

  toggleSellerSelection(group: SellerCartGroup): void {
    if (this.isSellerDisabled(group.vendedorId)) {
      const currentSellerNames = this.sellerGroups()
        .filter((g) => this.selectedSellerIds().has(g.vendedorId))
        .map((g) => g.vendedorNombre)
        .join(', ');
      this.actionError.set(
        `No puedes realizar pedidos de diferente vendedor a la vez. Deselecciona primero los productos de "${currentSellerNames}" si deseas comprar a "${group.vendedorNombre}".`,
      );
      return;
    }

    const selected = new Set(this.selectedProductIds());
    const isSelected = this.isSellerFullySelected(group);

    if (!isSelected) {
      this.actionError.set(null);
      group.items.forEach((item) => selected.add(item.productoId));
    } else {
      this.actionError.set(null);
      group.items.forEach((item) => selected.delete(item.productoId));
    }
    this.userSelectedProductIds.set(selected);
  }

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
    if (this.hasMixedSelection() || this.selectedSellerIds().size > 1) {
      this.actionError.set(
        'No se puede realizar un pedido con productos de diferentes vendedores en la misma compra. Debes tramitar pedidos independientes para cada vendedor.',
      );
      return;
    }
    const count = this.selectedItemCount();
    if (count === null || count === 0) {
      this.actionError.set('Selecciona al menos un producto para tramitar el pedido.');
      return;
    }

    const rawVendorId = this.selectedVendor()?.vendedorId;
    const vendorId = rawVendorId && rawVendorId !== 'default-seller' ? rawVendorId : undefined;
    this.actionError.set(null);
    this.checkoutLoading.set(true);
    const summary$ = vendorId
      ? this.pedidoService.obtenerCheckoutResumen(vendorId)
      : this.pedidoService.obtenerCheckoutResumen();

    summary$.subscribe({
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
    if (this.hasMixedSelection() || this.selectedSellerIds().size > 1) {
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
    const rawVendorId = this.selectedVendor()?.vendedorId;
    const vendorId = rawVendorId && rawVendorId !== 'default-seller' ? rawVendorId : undefined;
    this.actionError.set(null);
    this.processingOrder.set(true);
    const order$ = vendorId
      ? this.pedidoService.crearPedido(direccionId, metodoPagoId, vendorId)
      : this.pedidoService.crearPedido(direccionId, metodoPagoId);

    order$.subscribe({
      next: (order) => {
        this.orderConfirmation.set(order);
        this.checkoutOpen.set(false);
        this.checkoutSummary.set(null);
        this.processingOrder.set(false);
        this.userSelectedProductIds.set(null);
        if (typeof this.cartService.clear === 'function') {
          this.cartService.clear();
        }
        if (typeof this.cartService.load === 'function') {
          this.cartService.load();
        }
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
