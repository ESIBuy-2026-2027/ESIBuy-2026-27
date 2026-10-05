import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface ProductSummary {
  totalProducts: number;
  outOfStockProducts: number;
  lowStockProducts: number;
  lowStockThreshold: number;
}

export interface SellerProduct {
  id: string;
  name: string;
  categoryIds: string[];
  stock: number;
  description: string | null;
  price: number;
  discount: number | null;
  premiumDiscount: number | null;
  imageUrl: string | null;
  available: boolean;
  sellerId: string;
}

@Injectable({ providedIn: 'root' })
export class SellerCatalogService {
  private readonly http = inject(HttpClient);

  getSummary(): Observable<ProductSummary> {
    return this.http.get<ProductSummary>('/api/products/resumen');
  }

  getProducts(): Observable<SellerProduct[]> {
    return this.http.get<SellerProduct[]>('/api/products/mine');
  }
}
