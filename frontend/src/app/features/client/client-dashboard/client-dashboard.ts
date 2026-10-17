import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { AppSidebarComponent } from '../../../shared/app-sidebar/app-sidebar';
import { CartService } from '../cart/cart.service';
import { AvailableProduct, ProductsService } from '../../products/products.service';

@Component({
  selector: 'app-client-dashboard',
  standalone: true,
  imports: [FormsModule, RouterLink, AppSidebarComponent],
  templateUrl: './client-dashboard.html',
  styleUrl: './client-dashboard.css',
})
export class ClientDashboard implements OnInit {
  private readonly productsService = inject(ProductsService);
  readonly cartService = inject(CartService);
  private readonly currencyFormatter = new Intl.NumberFormat('es-ES', {
    style: 'currency',
    currency: 'EUR',
  });

  readonly products = signal<AvailableProduct[]>([]);
  readonly productsLoading = signal(true);
  readonly productsError = signal<string | null>(null);
  readonly quantities = signal<Record<string, number>>({});
  readonly addingProductId = signal<string | null>(null);
  readonly notification = signal<{ type: 'success' | 'error'; text: string } | null>(null);

  ngOnInit(): void {
    this.productsService.getAvailableProducts().subscribe({
      next: (products) => {
        this.products.set(products);
        this.productsLoading.set(false);
      },
      error: () => {
        this.productsLoading.set(false);
        this.productsError.set('No se pudo cargar el catálogo. Inténtalo de nuevo.');
      },
    });
  }

  quantityFor(productId: string): number {
    return this.quantities()[productId] ?? 1;
  }

  setQuantity(productId: string, quantity: number): void {
    this.quantities.update((quantities) => ({ ...quantities, [productId]: quantity }));
  }

  formatPrice(price: number): string {
    return this.currencyFormatter.format(price);
  }

  addToCart(product: AvailableProduct): void {
    const quantity = this.quantityFor(product.id);
    if (!Number.isInteger(quantity) || quantity < 1) {
      this.notification.set({ type: 'error', text: 'La cantidad debe ser como mínimo 1.' });
      return;
    }

    this.addingProductId.set(product.id);
    this.notification.set(null);
    this.cartService.addItem(product.id, quantity).subscribe({
      next: () => {
        this.addingProductId.set(null);
        this.notification.set({ type: 'success', text: 'Producto añadido al carrito.' });
      },
      error: (error: unknown) => {
        this.addingProductId.set(null);
        this.notification.set({ type: 'error', text: this.getCartErrorMessage(error) });
      },
    });
  }

  private getCartErrorMessage(error: unknown): string {
    if (error instanceof HttpErrorResponse) {
      if (error.status === 409 && typeof error.error?.message === 'string') {
        return error.error.message;
      }
      if (error.status === 404) {
        return 'El producto ya no está disponible.';
      }
      if (error.status === 0) {
        return 'No se pudo conectar con el servidor.';
      }
    }
    return 'No se pudo añadir el producto. Inténtalo de nuevo.';
  }
}
