import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { ProductsService, ProductItem, Category } from '../../../core/services/products.service';
import { WishlistService } from '../../../core/services/wishlist.service';
import { ToastService } from '../../../core/services/toast.service';
import { CartService } from '../../../core/services/cart.service';

const LOW_STOCK_THRESHOLD = 5;

@Component({
  selector: 'app-client-catalog',
  standalone: true,
  imports: [FormsModule, RouterLink],
  templateUrl: './client-catalog.html',
  styleUrl: './client-catalog.css',
})
export class ClientCatalog implements OnInit {
  private readonly productsService = inject(ProductsService);
  readonly wishlistService = inject(WishlistService);
  readonly toastService = inject(ToastService);
  readonly cartService = inject(CartService);

  readonly sidebarCollapsed = signal(false);

  toggleSidebar(): void {
    this.sidebarCollapsed.update((collapsed) => !collapsed);
  }

  addToCart(product: ProductItem, event: Event): void {
    event.stopPropagation();
    event.preventDefault();

    if (product.stock === 0) {
      this.toastService.show('El producto no tiene stock disponible.', 'error');
      return;
    }

    this.cartService.addItem(product.id, 1).subscribe({
      next: () => {
        this.toastService.show('Producto añadido al carrito', 'success');
      },
      error: () => {
        this.toastService.show('Error al añadir producto al carrito', 'error');
      },
    });
  }

  private readonly currencyFormatter = new Intl.NumberFormat('es-ES', {
    style: 'currency',
    currency: 'EUR',
  });

  products = signal<ProductItem[]>([]);
  categories = signal<Category[]>([]);
  loading = signal(true);
  error = signal(false);

  // Filters
  searchQuery = signal('');
  selectedCategory = signal('');
  minPrice = signal<number | null>(null);
  maxPrice = signal<number | null>(null);
  minRating = signal<number | null>(null);
  onlyInStock = signal(false);

  ngOnInit(): void {
    this.loadCategories();
    this.loadProducts();
    this.wishlistService.refreshIfClient();
  }

  loadCategories(): void {
    this.productsService.getCategories().subscribe({
      next: (categories) => this.categories.set(categories),
      error: () => { },
    });
  }

  loadProducts(): void {
    this.loading.set(true);
    this.error.set(false);

    this.productsService.getPublicCatalog({
      query: this.searchQuery() || undefined,
      categoryId: this.selectedCategory() || undefined,
      minPrice: this.minPrice() ?? undefined,
      maxPrice: this.maxPrice() ?? undefined,
      minRating: this.minRating() ?? undefined,
      inStock: this.onlyInStock() || undefined,
    }).subscribe({
      next: (items) => {
        this.products.set(items);
        this.loading.set(false);
      },
      error: () => {
        this.loading.set(false);
        this.error.set(true);
      },
    });
  }



  onFilterChange(): void {
    this.loadProducts();
  }

  resetFilters(): void {
    this.searchQuery.set('');
    this.selectedCategory.set('');
    this.minPrice.set(null);
    this.maxPrice.set(null);
    this.minRating.set(null);
    this.onlyInStock.set(false);
    this.loadProducts();
  }

  toggleFavorite(productId: string, event: Event): void {
    event.stopPropagation();
    event.preventDefault();

    this.wishlistService.toggleWishlist(productId).subscribe({
      next: (res) => {
        this.toastService.show(res.message, 'success');
      },
      error: () => {
        this.toastService.show('Error al actualizar tu lista de deseos', 'error');
      },
    });
  }

  categoryName(categoryId: string): string {
    return this.categories().find((c) => c.id === categoryId)?.name ?? 'Categoría';
  }

  stockStatus(stock: number): string {
    if (stock === 0) return 'Agotado';
    return stock <= LOW_STOCK_THRESHOLD ? 'Stock bajo' : 'En stock';
  }

  formatPrice(price: number): string {
    return this.currencyFormatter.format(price);
  }
}

