import { Component, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { Observable } from 'rxjs';
import { CartResponse, CartService } from './cart.service';

@Component({
  selector: 'app-client-cart',
  standalone: true,
  imports: [RouterLink],
  templateUrl: './cart.html',
  styleUrl: './cart.css',
})
export class ClientCart {
  readonly cartService = inject(CartService);
  readonly busyItemIds = signal<ReadonlySet<string>>(new Set());
  readonly actionError = signal<string | null>(null);
  private readonly currencyFormatter = new Intl.NumberFormat('es-ES', {
    style: 'currency',
    currency: 'EUR',
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
