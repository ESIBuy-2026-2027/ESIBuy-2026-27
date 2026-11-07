import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { WishlistService } from '../../../core/services/wishlist.service';
import { ToastService } from '../../../core/services/toast.service';
import { CartService } from '../../../core/services/cart.service';
import { ProductItem } from '../../../core/services/products.service';

@Component({
  selector: 'app-client-wishlist',
  standalone: true,
  imports: [FormsModule, RouterLink],
  templateUrl: './client-wishlist.html',
  styleUrl: './client-wishlist.css',
})
export class ClientWishlist implements OnInit {
  private readonly wishlistService = inject(WishlistService);
  readonly toastService = inject(ToastService);
  readonly cartService = inject(CartService);

  private readonly currencyFormatter = new Intl.NumberFormat('es-ES', {
    style: 'currency',
    currency: 'EUR',
  });

  items = signal<ProductItem[]>([]);
  loading = signal(true);
  error = signal(false);
  readonly sidebarCollapsed = signal(
    typeof localStorage !== 'undefined' && localStorage.getItem('client-sidebar-collapsed') === 'true'
  );
  animateTransition = false;
  readonly quantities = signal<Record<string, number>>({});
  readonly addingProducts = signal<Record<string, boolean>>({});
  readonly removingProducts = signal<Record<string, boolean>>({});

  toggleSidebar(): void {
    this.animateTransition = true;
    const collapsed = !this.sidebarCollapsed();
    this.sidebarCollapsed.set(collapsed);
    localStorage.setItem('client-sidebar-collapsed', String(collapsed));
  }

  getQuantity(productId: string): number {
    return this.quantities()[productId] ?? 1;
  }

  setQuantity(productId: string, qty: number, maxStock: number): void {
    const val = Math.max(1, Math.min(qty || 1, maxStock));
    this.quantities.update((map) => ({ ...map, [productId]: val }));
  }

  addToCart(product: ProductItem, event?: Event): void {
    if (event) {
      event.stopPropagation();
      event.preventDefault();
    }

    if (!product.available || product.stock === 0) {
      this.toastService.show('El producto no está disponible para compra.', 'error');
      return;
    }

    const qty = this.getQuantity(product.id);
    if (this.addingProducts()[product.id]) return;
    this.addingProducts.update((states) => ({ ...states, [product.id]: true }));
    this.cartService.addItem(product.id, qty).subscribe({
      next: () => {
        this.toastService.show('Producto añadido al carrito', 'success');
      },
      error: () => {
        this.toastService.show('Error al añadir producto al carrito', 'error');
      },
    }).add(() => this.addingProducts.update((states) => ({ ...states, [product.id]: false })));
  }

  ngOnInit(): void {
    this.loadWishlist();
  }

  loadWishlist(): void {
    this.loading.set(true);
    this.error.set(false);

    this.wishlistService.getWishlist().subscribe({
      next: (products) => {
        this.items.set(products);
        this.loading.set(false);
      },
      error: () => {
        this.loading.set(false);
        this.error.set(true);
      },
    });
  }

  removeFromWishlist(productId: string, event: Event): void {
    event.stopPropagation();
    event.preventDefault();

    if (this.removingProducts()[productId]) return;
    this.removingProducts.update((states) => ({ ...states, [productId]: true }));
    this.wishlistService.removeFromWishlist(productId).subscribe({
      next: (res) => {
        this.items.update((current) => current.filter((p) => p.id !== productId));
        this.toastService.show(res.message, 'success');
      },
      error: () => {
        this.toastService.show('Error al eliminar producto de la lista de deseos', 'error');
      },
    }).add(() => this.removingProducts.update((states) => ({ ...states, [productId]: false })));
  }

  formatPrice(price: number): string {
    return this.currencyFormatter.format(price);
  }
}
