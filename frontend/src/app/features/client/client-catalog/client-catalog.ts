import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { ProductsService, ProductItem, Category } from '../../../core/services/products.service';
import { WishlistService } from '../../../core/services/wishlist.service';
import { ToastService } from '../../../core/services/toast.service';
import { CartService } from '../../../core/services/cart.service';
import { AuthService } from '../../../core/services/auth.service';

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
  readonly authService = inject(AuthService);
  readonly isAdmin = computed(() => this.authService.currentUser()?.role === 'ADMIN');
  readonly wishlistService = inject(WishlistService);
  readonly toastService = inject(ToastService);
  readonly cartService = inject(CartService);
  readonly isPremiumUser = computed(() => this.authService.currentUser()?.customerType === 'PREMIUM');

  readonly sidebarCollapsed = signal(false);

  toggleSidebar(): void {
    this.sidebarCollapsed.update((collapsed) => !collapsed);
  }

  addToCart(product: ProductItem, event: Event): void {
    event.stopPropagation();
    event.preventDefault();

    if (this.isAdmin()) {
      return;
    }

    if (this.getRemainingStock(product) === 0) {
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

  private readonly catalogProducts = signal<ProductItem[]>([]);
  categories = signal<Category[]>([]);
  loading = signal(true);
  error = signal(false);

  products = computed(() => {
    let result = [...this.catalogProducts()];

    if (this.onlyOffers()) {
      result = result.filter(
        (product) =>
          (product.discount ?? 0) > 0 ||
          (product.premiumDiscount ?? 0) > 0
      );
    }

    switch (this.sortOrder()) {
      case 'rating-desc':
        result.sort((a, b) => (b.rating ?? 0) - (a.rating ?? 0));
        break;
      case 'rating-asc':
        result.sort((a, b) => (a.rating ?? 0) - (b.rating ?? 0));
        break;
      case 'price-asc':
        result.sort((a, b) => a.price - b.price);
        break;
      case 'price-desc':
        result.sort((a, b) => b.price - a.price);
        break;
    }

    return result;
  });

  // Filtros del catálogo
  searchQuery = signal('');
  selectedCategory = signal('');
  minPrice = signal<number | null>(null);
  maxPrice = signal<number | null>(null);
  minRating = signal<number | null>(null);
  onlyInStock = signal(false);
  onlyOffers = signal(false);

  sortOrder = signal<
    'recommended' | 'rating-desc' | 'rating-asc' | 'price-asc' | 'price-desc'
  >('recommended');

  // Límite superior del rango de precios
  priceLimit = signal(1);
  private priceLimitInitialized = false;

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
        if (!this.priceLimitInitialized) {
          const highestPrice = items.reduce(
            (highest, product) => Math.max(highest, product.price),
            0
          );

          this.priceLimit.set(Math.max(1, Math.ceil(highestPrice)));
          this.priceLimitInitialized = true;
        }

        this.catalogProducts.set(items);
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

  minPricePercent(): number {
    return ((this.minPrice() ?? 0) / this.priceLimit()) * 100;
  }

  maxPricePercent(): number {
    return ((this.maxPrice() ?? this.priceLimit()) / this.priceLimit()) * 100;
  }

  setMinPrice(value: number): void {
    const normalized = Math.max(
      0,
      Math.min(Math.trunc(Number(value) || 0), this.priceLimit())
    );

    const currentMax = this.maxPrice() ?? this.priceLimit();
    const newMin = Math.min(normalized, currentMax);

    this.minPrice.set(newMin === 0 ? null : newMin);
  }

  setMaxPrice(value: number): void {
    const normalized = Math.max(
      0,
      Math.min(Math.trunc(Number(value) || 0), this.priceLimit())
    );

    const currentMin = this.minPrice() ?? 0;
    const newMax = Math.max(normalized, currentMin);

    this.maxPrice.set(
      newMax >= this.priceLimit() ? null : newMax
    );
  }

  resetFilters(): void {
    this.searchQuery.set('');
    this.selectedCategory.set('');
    this.minPrice.set(null);
    this.maxPrice.set(null);
    this.minRating.set(null);
    this.onlyInStock.set(false);
    this.onlyOffers.set(false);
    this.sortOrder.set('recommended');

    this.loadProducts();
  }

  toggleFavorite(productId: string, event: Event): void {
    event.stopPropagation();
    event.preventDefault();

    if (this.isAdmin()) {
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

  getRemainingStock(product: ProductItem): number {
    const cartData = typeof this.cartService?.cart === 'function' ? this.cartService.cart() : null;
    const inCart = cartData?.items?.find((item) => item.productoId === product.id)?.cantidad ?? 0;
    return Math.max(0, product.stock - inCart);
  }

  getDiscountedPrice(p: ProductItem): number {
    if (p.discount && p.discount > 0) {
      const factor = (100 - p.discount) / 100;
      return Math.round(p.price * factor * 100) / 100;
    }
    return p.price;
  }

  getPremiumPrice(p: ProductItem): number {
    const standardDiscount = Math.max(0, p.discount ?? 0);
    const premiumDiscount = Math.max(0, p.premiumDiscount ?? 0);

    const totalDiscount = standardDiscount + premiumDiscount;
    const factor = Math.max(0, 100 - totalDiscount) / 100;

    return Math.round(p.price * factor * 100) / 100;
  }

  hasStandardDiscount(p: ProductItem): boolean {
    return Boolean(p.discount && p.discount > 0);
  }

  hasPremiumDiscount(p: ProductItem): boolean {
    return Boolean(p.premiumDiscount && p.premiumDiscount > 0);
  }

  stockStatus(itemOrStock: ProductItem | number): string {
    const stock = typeof itemOrStock === 'number' ? itemOrStock : this.getRemainingStock(itemOrStock);
    if (stock === 0) return 'Agotado';
    return stock <= LOW_STOCK_THRESHOLD ? 'Stock bajo' : 'En stock';
  }

  formatPrice(price: number): string {
    return this.currencyFormatter.format(price);
  }

  readonly imageErrors = signal<Record<string, boolean>>({});

  onImageError(productId: string): void {
    this.imageErrors.update((prev) => ({ ...prev, [productId]: true }));
  }
}

