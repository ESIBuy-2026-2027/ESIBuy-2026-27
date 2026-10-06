import { Component, OnInit, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { Category, ProductsService } from '../products.service';
import { SellerCatalogService, SellerProduct } from './seller-catalog.service';

const LOW_STOCK_THRESHOLD = 5;

@Component({
  selector: 'app-seller-catalog',
  standalone: true,
  imports: [RouterLink],
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
}
