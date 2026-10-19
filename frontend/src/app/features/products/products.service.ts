import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface Category {
  id: string;
  name: string;
}

export interface ProductItem {
  id: string;
  name: string;
  categoryIds: string[];
  stock: number;
  description?: string | null;
  price: number;
  discount?: number | null;
  premiumDiscount?: number | null;
  imageUrl?: string | null;
  available: boolean;
  sellerId: string;
  rating?: number;
}

export interface ProductCreateRequest {
  name: string;
  categoryIds: string[];
  stock?: number;
  description?: string | null;
  price: number;
  discount?: number | null;
  premiumDiscount?: number | null;
  imageUrl?: string | null;
  available: boolean;
}

export interface CreatedProduct {
  id: string;
}

export interface CatalogFilterParams {
  query?: string;
  categoryId?: string;
  minPrice?: number;
  maxPrice?: number;
  minRating?: number;
  inStock?: boolean;
}

@Injectable({ providedIn: 'root' })
export class ProductsService {
  private readonly http = inject(HttpClient);

  getCategories(): Observable<Category[]> {
    return this.http.get<Category[]>('/api/categorias');
  }

  createProduct(request: ProductCreateRequest): Observable<CreatedProduct> {
    return this.http.post<CreatedProduct>('/api/products', request);
  }

  getPublicCatalog(filters?: CatalogFilterParams): Observable<ProductItem[]> {
    let params = new HttpParams();
    if (filters) {
      if (filters.query?.trim()) params = params.set('query', filters.query.trim());
      if (filters.categoryId?.trim()) params = params.set('categoryId', filters.categoryId.trim());
      if (filters.minPrice !== undefined && filters.minPrice !== null) params = params.set('minPrice', filters.minPrice.toString());
      if (filters.maxPrice !== undefined && filters.maxPrice !== null) params = params.set('maxPrice', filters.maxPrice.toString());
      if (filters.minRating !== undefined && filters.minRating !== null) params = params.set('minRating', filters.minRating.toString());
      if (filters.inStock !== undefined && filters.inStock !== null) params = params.set('inStock', filters.inStock.toString());
    }
    return this.http.get<ProductItem[]>('/api/products', { params });
  }

  getProductById(id: string): Observable<ProductItem> {
    return this.http.get<ProductItem>(`/api/products/${id}`);
  }

  updateProductStatus(id: string, available: boolean): Observable<ProductItem> {
    return this.http.patch<ProductItem>(`/api/products/${id}/status`, null, {
      params: { available: available.toString() }
    });
  }
}
