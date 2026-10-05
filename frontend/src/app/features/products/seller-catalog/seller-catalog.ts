import { Component, OnInit, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { ProductSummary, SellerCatalogService, SellerProduct } from './seller-catalog.service';

@Component({
  selector: 'app-seller-catalog',
  standalone: true,
  imports: [RouterLink],
  templateUrl: './seller-catalog.html',
  styleUrl: './seller-catalog.css',
})
export class SellerCatalog implements OnInit {
  private readonly catalogService = inject(SellerCatalogService);
  private readonly currencyFormatter = new Intl.NumberFormat('es-ES', {
    style: 'currency',
    currency: 'EUR',
  });

  summary = signal<ProductSummary | null>(null);
  loading = signal(true);
  loadError = signal(false);
  products = signal<SellerProduct[]>([]);
  productsLoading = signal(true);
  productsError = signal(false);

  ngOnInit(): void {
    this.loadSummary();
    this.loadProducts();
  }

  loadSummary(): void {
    this.loading.set(true);
    this.loadError.set(false);
    this.summary.set(null);

    this.catalogService.getSummary().subscribe({
      next: (summary) => {
        this.summary.set(summary);
        this.loading.set(false);
      },
      error: () => {
        this.loading.set(false);
        this.loadError.set(true);
      },
    });
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

  stockStatus(stock: number): string {
    if (stock === 0) {
      return 'Sin stock';
    }
    return stock <= (this.summary()?.lowStockThreshold ?? 5) ? 'Stock bajo' : 'En stock';
  }

  formatPrice(price: number): string {
    return this.currencyFormatter.format(price);
  }
}
