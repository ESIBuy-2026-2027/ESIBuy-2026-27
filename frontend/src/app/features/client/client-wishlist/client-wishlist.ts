import { Component, OnInit, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { WishlistService } from '../../../core/services/wishlist.service';
import { ToastService } from '../../../core/services/toast.service';
import { ProductItem } from '../../products/products.service';

@Component({
  selector: 'app-client-wishlist',
  standalone: true,
  imports: [RouterLink],
  templateUrl: './client-wishlist.html',
  styleUrl: './client-wishlist.css',
})
export class ClientWishlist implements OnInit {
  private readonly wishlistService = inject(WishlistService);
  private readonly toastService = inject(ToastService);

  private readonly currencyFormatter = new Intl.NumberFormat('es-ES', {
    style: 'currency',
    currency: 'EUR',
  });

  items = signal<ProductItem[]>([]);
  loading = signal(true);
  error = signal(false);

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

    this.wishlistService.removeFromWishlist(productId).subscribe({
      next: (res) => {
        this.items.update((current) => current.filter((p) => p.id !== productId));
        this.toastService.show(res.message, 'success');
      },
      error: () => {
        this.toastService.show('Error al eliminar producto de la lista de deseos', 'error');
      },
    });
  }

  formatPrice(price: number): string {
    return this.currencyFormatter.format(price);
  }
}

