import { httpErrorMessage } from '../../../core/utils/http-error-message';
import { AppSidebarComponent } from '../../../shared/app-sidebar/app-sidebar';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { Category, ProductCreateRequest, ProductsService } from '../../../core/services/products.service';
import { SellerCatalogService, SellerProduct } from '../../../core/services/seller-catalog.service';

const LOW_STOCK_THRESHOLD = 5;
type InlineField = 'price' | 'stock';

@Component({
  selector: 'app-seller-catalog',
  standalone: true,
  imports: [FormsModule, RouterLink, AppSidebarComponent],
  templateUrl: './seller-catalog.html',
  styleUrl: './seller-catalog.css',
})
export class SellerCatalog implements OnInit {
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
  inlineEdit = signal<{ productId: string; field: InlineField } | null>(null);
  inlineValue = signal<number | null>(null);
  inlineSaving = signal(false);
  inlineError = signal<string | null>(null);
  visibilitySaving = signal<string | null>(null);
  visibilityError = signal<string | null>(null);
  visibleProducts = computed(() => {
    const query = this.productSearch().trim().toLocaleLowerCase();
    if (!query) {
      return this.products();
    }

    return this.products().filter((product) => product.name.toLocaleLowerCase().includes(query));
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
    available: boolean): ProductCreateRequest {
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
    if (product.imageUrl) {
      request.imageUrl = product.imageUrl;
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
