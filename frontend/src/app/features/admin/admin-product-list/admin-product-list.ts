import { ChangeDetectorRef, Component, OnInit, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { FormsModule } from '@angular/forms';
import {
  AdminProductService,
  AdminProductSummary,
  ADMIN_READONLY_ERROR_MESSAGE,
} from '../../../core/services/admin-product.service';
import { ProductsService, Category } from '../../../core/services/products.service';

@Component({
  selector: 'app-admin-product-list',
  standalone: true,
  imports: [CommonModule, RouterLink, RouterLinkActive, FormsModule],
  templateUrl: './admin-product-list.html',
  styleUrls: ['./admin-product-list.css'],
})
export class AdminProductList implements OnInit {
  private readonly adminProductService = inject(AdminProductService);
  private readonly productsService = inject(ProductsService);
  private readonly cdr = inject(ChangeDetectorRef);

  private readonly currencyFormatter = new Intl.NumberFormat('es-ES', {
    style: 'currency',
    currency: 'EUR',
  });

  sidebarCollapsed = signal(false);
  products = signal<AdminProductSummary[]>([]);
  categories = signal<Category[]>([]);
  loading = signal(true);
  error = signal('');
  successMessage = signal('');

  searchTerm = signal('');
  stockFilter = signal<string>('ALL');
  categoryFilter = signal<string>('ALL');
  sellerSort = signal<'ASC' | 'DESC' | null>(null);

  sellerSortTitle = computed(() => {
    const sort = this.sellerSort();
    if (sort === 'ASC') {
      return 'Ordenar por vendedor (actual: A-Z, clic para Z-A)';
    }
    if (sort === 'DESC') {
      return 'Ordenar por vendedor (actual: Z-A, clic para A-Z)';
    }
    return 'Ordenar por vendedor alfabéticamente (clic para ordenar)';
  });

  filteredProducts = computed(() => {
    const term = this.searchTerm().trim().toLowerCase();
    const stock = this.stockFilter();
    const cat = this.categoryFilter();

    const filtered = this.products().filter((p) => {
      const matchesSearch =
        !term ||
        p.name.toLowerCase().includes(term) ||
        p.sellerName.toLowerCase().includes(term);

      const matchesStock =
        stock === 'ALL' ||
        (stock === 'IN_STOCK' && p.stock > 0) ||
        (stock === 'OUT_OF_STOCK' && p.stock <= 0);

      const matchesCategory =
        cat === 'ALL' ||
        p.category === cat;

      return matchesSearch && matchesStock && matchesCategory;
    });

    const sort = this.sellerSort();
    if (sort) {
      return [...filtered].sort((a, b) => {
        const nameA = (a.sellerName || '').trim();
        const nameB = (b.sellerName || '').trim();
        const comparison = nameA.localeCompare(nameB, 'es', { sensitivity: 'base', numeric: true });
        return sort === 'ASC' ? comparison : -comparison;
      });
    }

    return filtered;
  });

  toggleSellerSort(): void {
    const current = this.sellerSort();
    if (current === 'ASC') {
      this.sellerSort.set('DESC');
    } else {
      this.sellerSort.set('ASC');
    }
  }

  ngOnInit(): void {
    this.loadCategories();
    this.loadProducts();
  }

  toggleSidebar(): void {
    this.sidebarCollapsed.update((v) => !v);
  }

  loadCategories(): void {
    this.productsService.getCategories().subscribe({
      next: (data) => this.categories.set(data),
      error: () => { },
    });
  }

  loadProducts(): void {
    this.loading.set(true);
    this.error.set('');
    this.cdr.markForCheck();

    let inStock: boolean | undefined;

    if (this.stockFilter() === 'IN_STOCK') {
      inStock = true;
    } else if (this.stockFilter() === 'OUT_OF_STOCK') {
      inStock = false;
    }

    const categoryId = this.categoryFilter() === 'ALL' ? undefined : this.categoryFilter();

    this.adminProductService.getProducts({ inStock, categoryId }).subscribe({
      next: (data) => {
        this.products.set(data);
        this.loading.set(false);
        this.cdr.markForCheck();
      },
      error: (err: unknown) => {
        if (err instanceof HttpErrorResponse && err.status === 403) {
          this.error.set(ADMIN_READONLY_ERROR_MESSAGE);
        } else if (err instanceof HttpErrorResponse && err.status === 401) {
          this.error.set('Tu sesión no es válida. Inicia sesión de nuevo.');
        } else {
          this.error.set('No se pudo cargar la lista de productos para supervisión.');
        }
        this.loading.set(false);
        this.cdr.markForCheck();
      },
    });
  }

  applyFilters(): void {
    this.loadProducts();
  }

  formatPrice(price: number): string {
    return this.currencyFormatter.format(price);
  }

  attemptUnauthorizedAction(productId: string): void {
    this.adminProductService.deleteProductAsAdmin(productId).subscribe({
      next: () => { },
      error: (err: unknown) => {
        if (err instanceof HttpErrorResponse && err.status === 403) {
          this.error.set(ADMIN_READONLY_ERROR_MESSAGE);
        } else {
          this.error.set('Error al procesar la solicitud.');
        }
        this.cdr.markForCheck();
      },
    });
  }
}

