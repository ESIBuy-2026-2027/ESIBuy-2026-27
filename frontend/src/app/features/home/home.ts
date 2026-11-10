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
  readonly isPremiumUser = computed(() => this.authService.currentUser()?.customerType === 'PREMIUM');

  private readonly currencyFormatter = new Intl.NumberFormat('es-ES', {
    style: 'currency',
    currency: 'EUR',
  });

  private readonly catalogProducts = signal<ProductItem[]>([]);
  readonly products = computed(() => {
    let result = [...this.catalogProducts()];

    if (this.onlyOffers()) {
      result = result.filter((product) => this.hasAnyDiscount(product));
    }

    switch (this.sortOrder()) {
      case 'rating-desc':
        result.sort((a, b) => (b.rating ?? -1) - (a.rating ?? -1));
        break;
      case 'rating-asc':
        result.sort((a, b) => (a.rating ?? Number.MAX_VALUE) - (b.rating ?? Number.MAX_VALUE));
        break;
      case 'price-asc':
        result.sort((a, b) => this.getDisplayPrice(a) - this.getDisplayPrice(b));
        break;
      case 'price-desc':
        result.sort((a, b) => this.getDisplayPrice(b) - this.getDisplayPrice(a));
        break;
    }

    return result;
  });
  readonly pageIndex = signal(0);
  readonly pageSize = 12;
  readonly pageCount = computed(() => Math.max(1, Math.ceil(this.products().length / this.pageSize)));
  readonly visibleProducts = computed(() => {
    const start = this.pageIndex() * this.pageSize;
    return this.products().slice(start, start + this.pageSize);
  });
  categories = signal<Category[]>([]);
  categoriesLoading = signal(true);
  categoryError = signal(false);
  loading = signal(true);
  error = signal(false);
  readonly favoriteUpdating = signal<Record<string, boolean>>({});
  readonly cartAdding = signal<Record<string, boolean>>({});

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
    this.categoriesLoading.set(true);
    this.categoryError.set(false);
    this.productsService.getCategories().subscribe({
      next: (categories) => {
        this.categories.set(categories);
        this.categoriesLoading.set(false);
      },
      error: () => {
        this.categoriesLoading.set(false);
        this.categoryError.set(true);
      },
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
        this.catalogProducts.set(items);
        this.pageIndex.set(0);
        this.loading.set(false);
      },
      error: () => {
        this.loading.set(false);
        this.error.set(true);
      },
    });
  }

  onFilterChange(): void {
    this.pageIndex.set(0);
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
    this.pageIndex.set(0);
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

    if (this.favoriteUpdating()[productId]) return;
    this.favoriteUpdating.update((states) => ({ ...states, [productId]: true }));
    this.wishlistService.toggleWishlist(productId).subscribe({
      next: (res) => {
        if (res?.message) {
          this.toastService.show(res.message, 'success');
        }
      },
      error: () => {
        this.toastService.show('Error al actualizar tu lista de deseos', 'error');
      },
    }).add(() => this.favoriteUpdating.update((states) => ({ ...states, [productId]: false })));
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

  getTotalDiscount(p: ProductItem): number {
    return Math.min(100, Math.max(0, p.discount ?? 0) + Math.max(0, p.premiumDiscount ?? 0));
  }

  getPremiumPrice(p: ProductItem): number {
    return Math.round(p.price * (1 - this.getTotalDiscount(p) / 100) * 100) / 100;
  }

  hasStandardDiscount(p: ProductItem): boolean {
    return Boolean(p.discount && p.discount > 0);
  }

  hasPremiumDiscount(p: ProductItem): boolean {
    return Boolean(p.premiumDiscount && p.premiumDiscount > 0);
  }

  hasAnyDiscount(p: ProductItem): boolean {
    return this.hasStandardDiscount(p) || this.hasPremiumDiscount(p);
  }

  getDisplayPrice(p: ProductItem): number {
    return this.isPremiumUser() ? this.getPremiumPrice(p) : this.getDiscountedPrice(p);
  }

  stockStatus(itemOrStock: ProductItem | number): string {
    const stock = typeof itemOrStock === 'number' ? itemOrStock : this.getRemainingStock(itemOrStock);
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

    if (this.getRemainingStock(product) === 0) {
      this.toastService.show('El producto no tiene stock disponible.', 'error');
      return;
    }

    if (this.cartAdding()[product.id]) return;
    this.cartAdding.update((states) => ({ ...states, [product.id]: true }));
    this.cartService.addItem(product.id, 1).subscribe({
      next: () => {
        this.toastService.show('Producto añadido al carrito', 'success');
      },
      error: () => {
        this.toastService.show('Error al añadir producto al carrito', 'error');
      },
    }).add(() => this.cartAdding.update((states) => ({ ...states, [product.id]: false })));
  }

  formatPrice(price: number): string {
    return this.currencyFormatter.format(price);
  }

  previousPage(): void {
    this.pageIndex.update((page) => Math.max(0, page - 1));
  }

  nextPage(): void {
    this.pageIndex.update((page) => Math.min(this.pageCount() - 1, page + 1));
  }

  readonly imageErrors = signal<Record<string, boolean>>({});

  onImageError(productId: string): void {
    this.imageErrors.update((prev) => ({ ...prev, [productId]: true }));
  }
}