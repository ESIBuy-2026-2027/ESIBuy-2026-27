import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface ProductSummary {
  totalProductos: number;
  productosSinStock: number;
  productosStockBajo: number;
  umbralStockBajo: number;
}

@Injectable({ providedIn: 'root' })
export class SellerCatalogService {
  private readonly http = inject(HttpClient);

  getSummary(): Observable<ProductSummary> {
    return this.http.get<ProductSummary>('/api/products/resumen');
  }
}
