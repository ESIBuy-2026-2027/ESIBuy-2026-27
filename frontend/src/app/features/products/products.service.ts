import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface Category {
  id: string;
  name: string;
}

export interface ProductCreateRequest {
  name: string;
  categoryIds: string[];
  stock?: number;
  description?: string;
  price: number;
  discount?: number;
  premiumDiscount?: number;
  imageUrl?: string;
  available: boolean;
}

export interface CreatedProduct {
  id: string;
}

export interface ProductDetails {
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
export class ProductsService {
  private readonly http = inject(HttpClient);

  getCategories(): Observable<Category[]> {
    return this.http.get<Category[]>('/api/categorias');
  }

  createProduct(request: ProductCreateRequest): Observable<CreatedProduct> {
    return this.http.post<CreatedProduct>('/api/products', request);
  }

  getProduct(id: string): Observable<ProductDetails> {
    return this.http.get<ProductDetails>(`/api/products/${encodeURIComponent(id)}`);
  }

  updateProduct(id: string, request: ProductCreateRequest): Observable<ProductDetails> {
    return this.http.put<ProductDetails>(`/api/products/${encodeURIComponent(id)}`, request);
  }
}
