import { isPlatformBrowser } from '@angular/common';
import { httpErrorMessage } from '../../../core/utils/http-error-message';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit, PLATFORM_ID, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { Category, ProductCreateRequest, ProductsService } from '../../../core/services/products.service';
import { SellerCatalogService, SellerProduct } from '../../../core/services/seller-catalog.service';

const LOW_STOCK_THRESHOLD = 5;
type InlineField = 'price' | 'stock';
type SortOrder = 'recommended' | 'name-asc' | 'price-asc' | 'price-desc' | 'stock-asc' | 'stock-desc';
type VisibilityFilter = 'all' | 'published' | 'hidden';

@Component({
  selector: 'app-seller-catalog',
  standalone: true,
  imports: [FormsModule, RouterLink, RouterLinkActive],
  templateUrl: './seller-catalog.html',
  styleUrl: './seller-catalog.css',
})
export class SellerCatalog implements OnInit {
  private readonly platformId = inject(PLATFORM_ID);
  private readonly catalogService = inject(SellerCatalogService);
  private readonly productsService = inject(ProductsService);
  private readonly currencyFormatter = new Intl.NumberFormat('es-ES', {
    style: 'currency',
    currency: 'EUR',
  });

  products = signal<SellerProduct[]>([]);
  productsLoading = signal(true);
  productsError = signal(false);
  categories = signal<Category[]>([]);
  categoriesLoading = signal(true);
  categoriesError = signal(false);
  productSearch = signal('');
  filtersOpen = signal(false);
  selectedCategory = signal('');
  minPrice = signal<number | null>(null);
  maxPrice = signal<number | null>(null);
  selectedVisibility = signal<VisibilityFilter>('all');
  onlyInStock = signal(false);
  onlyOffers = signal(false);
  sortOrder = signal<SortOrder>('recommended');
  priceLimit = signal(1);
  inlineEdit = signal<{ productId: string; field: InlineField } | null>(null);
  inlineValue = signal<number | null>(null);
  inlineSaving = signal(false);
  inlineError = signal<string | null>(null);
  visibilitySaving = signal<string | null>(null);
  visibilityError = signal<string | null>(null);
  sidebarCollapsed = signal(this.readSidebarPreference());
  visibleProducts = computed(() => {
    const query = this.productSearch().trim().toLocaleLowerCase();
    const minPrice = this.minPrice();
    const maxPrice = this.maxPrice();
    const selectedCategory = this.selectedCategory();
    const selectedVisibility = this.selectedVisibility();
    const filtered = this.products().filter((product) => {
      const matchesSearch = !query
        || product.name.toLocaleLowerCase().includes(query)
        || (product.description ?? '').toLocaleLowerCase().includes(query);
      const matchesCategory = !selectedCategory || product.categoryIds.includes(selectedCategory);
      const matchesMinPrice = minPrice === null || product.price >= minPrice;
      const matchesMaxPrice = maxPrice === null || product.price <= maxPrice;
      const matchesVisibility = selectedVisibility === 'all'
        || (selectedVisibility === 'published' && product.available)
        || (selectedVisibility === 'hidden' && !product.available);
      const matchesStock = !this.onlyInStock() || product.stock > 0;
      const matchesOffers = !this.onlyOffers()
        || (product.discount ?? 0) > 0
        || (product.premiumDiscount ?? 0) > 0;
      return matchesSearch && matchesCategory && matchesMinPrice && matchesMaxPrice
        && matchesVisibility && matchesStock && matchesOffers;
    });

    return filtered.sort((first, second) => {
      switch (this.sortOrder()) {
        case 'name-asc':
          return first.name.localeCompare(second.name, 'es', { sensitivity: 'base' });
        case 'stock-asc':
          return first.stock - second.stock;
        case 'stock-desc':
          return second.stock - first.stock;
        case 'price-asc':
          return first.price - second.price;
        case 'price-desc':
          return second.price - first.price;
        default:
          return 0;
      }
    });
  });

  ngOnInit(): void {
    this.loadProducts();
    this.loadCategories();
  }

  loadProducts(): void {
    this.productsLoading.set(true);
    this.productsError.set(false);

    this.catalogService.getProducts().subscribe({
      next: (products) => {
        this.products.set(products);
        const highestPrice = products.reduce(
          (highest, product) => Math.max(highest, product.price),
          0,
        );
        this.priceLimit.set(Math.max(1, Math.ceil(highestPrice)));
        this.productsLoading.set(false);
      },
      error: () => {
        this.productsLoading.set(false);
        this.productsError.set(true);
      },
    });
  }

  loadCategories(): void {
    this.categoriesLoading.set(true);
    this.categoriesError.set(false);

    this.productsService.getCategories().subscribe({
      next: (categories) => {
        this.categories.set(categories);
        this.categoriesLoading.set(false);
      },
      error: () => {
        this.categoriesLoading.set(false);
        this.categoriesError.set(true);
      },
    });
  }

  categoryName(categoryId: string): string {
    return this.categories().find((category) => category.id === categoryId)?.name
      ?? 'Categoría no disponible';
  }

  toggleFilters(): void {
    this.filtersOpen.update((isOpen) => !isOpen);
  }

  clearFilters(): void {
    this.productSearch.set('');
    this.selectedCategory.set('');
    this.minPrice.set(null);
    this.maxPrice.set(null);
    this.selectedVisibility.set('all');
    this.onlyInStock.set(false);
    this.onlyOffers.set(false);
    this.sortOrder.set('recommended');
  }

  toggleSidebar(): void {
    const collapsed = !this.sidebarCollapsed();
    this.sidebarCollapsed.set(collapsed);

    if (isPlatformBrowser(this.platformId)) {
      localStorage.setItem('seller-sidebar-collapsed', String(collapsed));
    }
  }

  hasActiveFilters(): boolean {
    return Boolean(this.productSearch().trim())
      || Boolean(this.selectedCategory())
      || this.minPrice() !== null
      || this.maxPrice() !== null
      || this.selectedVisibility() !== 'all'
      || this.onlyInStock()
      || this.onlyOffers()
      || this.sortOrder() !== 'recommended';
  }

  minPricePercent(): number {
    return ((this.minPrice() ?? 0) / this.priceLimit()) * 100;
  }

  maxPricePercent(): number {
    return ((this.maxPrice() ?? this.priceLimit()) / this.priceLimit()) * 100;
  }

  setMinPrice(value: number): void {
    const normalized = Math.max(0, Math.min(Math.trunc(Number(value) || 0), this.priceLimit()));
    const currentMax = this.maxPrice() ?? this.priceLimit();
    const newMin = Math.min(normalized, currentMax);
    this.minPrice.set(newMin === 0 ? null : newMin);
  }

  setMaxPrice(value: number): void {
    const normalized = Math.max(0, Math.min(Math.trunc(Number(value) || 0), this.priceLimit()));
    const currentMin = this.minPrice() ?? 0;
    const newMax = Math.max(normalized, currentMin);
    this.maxPrice.set(newMax >= this.priceLimit() ? null : newMax);
  }

  private readSidebarPreference(): boolean {
    if (!isPlatformBrowser(this.platformId)) {
      return false;
    }

    return localStorage.getItem('seller-sidebar-collapsed') === 'true';
  }

  stockStatus(stock: number): string {
    if (stock === 0) {
      return 'Sin stock';
    }
    return stock <= LOW_STOCK_THRESHOLD ? 'Stock bajo' : 'En stock';
  }

  formatPrice(price: number): string {
    return this.currencyFormatter.format(price);
  }

  toggleVisibility(product: SellerProduct): void {
    if (this.inlineSaving() || this.visibilitySaving()) {
      return;
    }

    this.visibilitySaving.set(product.id);
    this.visibilityError.set(null);
    this.productsService.updateProduct(
      product.id,
      this.toUpdateRequest(product, null, null, !product.available),
    ).subscribe({
      next: (updatedProduct) => {
        this.products.update((products) => products.map((currentProduct) =>
          currentProduct.id === updatedProduct.id ? updatedProduct : currentProduct));
        this.visibilitySaving.set(null);
      },
      error: (error: unknown) => {
        this.visibilitySaving.set(null);
        this.visibilityError.set(this.getInlineErrorMessage(error));
      },
    });
  }

  startInlineEdit(product: SellerProduct, field: InlineField): void {
    if (this.inlineSaving()) {
      return;
    }
    this.inlineEdit.set({ productId: product.id, field });
    this.inlineValue.set(field === 'price' ? product.price : product.stock);
    this.inlineError.set(null);
  }

  cancelInlineEdit(): void {
    if (!this.inlineSaving()) {
      this.inlineEdit.set(null);
      this.inlineValue.set(null);
      this.inlineError.set(null);
    }
  }

  saveInlineEdit(product: SellerProduct): void {
    const edit = this.inlineEdit();
    const value = this.inlineValue();
    if (!edit || edit.productId !== product.id || value === null || !Number.isFinite(value)) {
      this.inlineError.set(edit?.field === 'price'
        ? 'Introduce un precio válido.'
        : 'Introduce un stock válido.');
      return;
    }
    if (edit.field === 'price' && value <= 0) {
      this.inlineError.set('El precio debe ser mayor que 0.');
      return;
    }
    if (edit.field === 'stock' && (!Number.isInteger(value) || value < 0)) {
      this.inlineError.set('El stock debe ser un número entero no negativo.');
      return;
    }

    const request = this.toUpdateRequest(product, edit.field, value, product.available);
    this.inlineSaving.set(true);
    this.inlineError.set(null);
    this.productsService.updateProduct(product.id, request).subscribe({
      next: (updatedProduct) => {
        this.products.update((products) => products.map((currentProduct) =>
          currentProduct.id === updatedProduct.id ? updatedProduct : currentProduct));
        this.inlineSaving.set(false);
        this.inlineEdit.set(null);
        this.inlineValue.set(null);
      },
      error: (error: unknown) => {
        this.inlineSaving.set(false);
        this.inlineError.set(this.getInlineErrorMessage(error));
      },
    });
  }

  isEditing(productId: string, field: InlineField): boolean {
    const edit = this.inlineEdit();
    return edit?.productId === productId && edit.field === field;
  }

  private toUpdateRequest(
    product: SellerProduct,
    field: InlineField | null,
    value: number | null,
    available: boolean,
  ): ProductCreateRequest {
    const request: ProductCreateRequest = {
      name: product.name,
      categoryIds: product.categoryIds,
      price: field === 'price' && value !== null ? value : product.price,
      available,
    };

    if (product.stock !== null) {
      request.stock = field === 'stock' && value !== null ? value : product.stock;
    }

    if (product.description) {
      request.description = product.description;
    }

    if (product.discount !== null) {
      request.discount = product.discount;
    }

    if (product.premiumDiscount !== null) {
      request.premiumDiscount = product.premiumDiscount;
    }

    return request;
  }


  private getInlineErrorMessage(error: unknown): string {
    if (error instanceof HttpErrorResponse) {
      if (error.status === 401) {
        return httpErrorMessage(error) ?? 'No se pudo guardar el cambio. Inténtalo de nuevo.';
      }
      if (error.status === 403) {
        return 'No tienes permiso para modificar este producto.';
      }
      if (error.status === 404) {
        return 'El producto ya no existe o no tienes permiso para modificarlo.';
      }
      if (error.status === 0) {
        return httpErrorMessage(error) ?? 'No se pudo guardar el cambio. Inténtalo de nuevo.';
      }
      if (typeof error.error?.message === 'string') {
        return error.error.message;
      }
    }
    return 'No se pudo guardar el cambio. Inténtalo de nuevo.';
  }
}