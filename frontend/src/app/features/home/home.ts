import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';
import { ProductsService, ProductItem, Category } from '../../core/services/products.service';
import { WishlistService } from '../../core/services/wishlist.service';
import { ToastService } from '../../core/services/toast.service';
import { CartService } from '../../core/services/cart.service';

const LOW_STOCK_THRESHOLD = 5;

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [FormsModule, RouterLink],
  templateUrl: './home.html',
  styleUrl: './home.css',
})
export class Home implements OnInit {
  private readonly router = inject(Router);
  readonly authService = inject(AuthService);
  private readonly productsService = inject(ProductsService);
  readonly wishlistService = inject(WishlistService);
  readonly toastService = inject(ToastService);
  readonly cartService = inject(CartService);

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
  sortOrder = signal('recommended');
  onlyOffers = signal(false);

  // Lógica del Slider de Precios
  priceLimit = signal(4500);

  minPricePercent = computed(() => {
    const min = this.minPrice() ?? 0;
    return (min / this.priceLimit()) * 100;
  });

  maxPricePercent = computed(() => {
    const max = this.maxPrice() ?? this.priceLimit();
    return (max / this.priceLimit()) * 100;
  });

  setMinPrice(val: number | null): void {
    if (val === null) {
      this.minPrice.set(null);
      return;
    }
    const max = this.maxPrice() ?? this.priceLimit();
    this.minPrice.set(Math.min(val, max));
  }

  setMaxPrice(val: number | null): void {
    if (val === null) {
      this.maxPrice.set(null);
      return;
    }
    const min = this.minPrice() ?? 0;
    this.maxPrice.set(Math.max(val, min));
  }

  readonly isAuthenticated = computed(() => {
    return this.authService.currentUser() !== null;
  });

  readonly currentUser = computed(() => {
    return this.authService.currentUser();
  });

  ngOnInit(): void {
    const role = this.authService.currentUser()?.role;
    if (role === 'CLIENT') {
      void this.router.navigate(['/client/catalog'], { replaceUrl: true });
      return;
    }
    if (role === 'SELLER') {
      void this.router.navigate(['/seller/catalog'], { replaceUrl: true });
      return;
    }
    if (role === 'ADMIN') {
      void this.router.navigate(['/client/catalog'], { replaceUrl: true });
      return;
    }
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
    this.sortOrder.set('recommended'); // <-- NUEVO
    this.onlyOffers.set(false);        // <-- NUEVO
    this.loadProducts();
  }

  toggleFavorite(productId: string, event: Event): void {
    event.stopPropagation();
    event.preventDefault();

    const user = this.authService.currentUser();
    if (!user) {
      this.toastService.show('Debes iniciar sesión con tu cuenta de cliente para guardar favoritos.', 'info');
      void this.router.navigate(['/login']);
      return;
    }

    if (user.role !== 'CLIENT') {
      this.toastService.show('Solo los clientes pueden guardar productos en su lista de deseos.', 'info');
      return;
    }

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

  addToCart(product: ProductItem, event: Event): void {
    event.stopPropagation();
    event.preventDefault();

    const user = this.authService.currentUser();
    if (!user) {
      this.toastService.show('Debes iniciar sesión con tu cuenta de cliente para realizar compras.', 'info');
      void this.router.navigate(['/login']);
      return;
    }

    if (user.role !== 'CLIENT') {
      this.toastService.show('Solo los clientes pueden realizar compras.', 'info');
      return;
    }

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

  formatPrice(price: number): string {
    return this.currencyFormatter.format(price);
  }

  readonly imageErrors = signal<Record<string, boolean>>({});

  onImageError(productId: string): void {
    this.imageErrors.update((prev) => ({ ...prev, [productId]: true }));
  }
}