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

@Injectable({ providedIn: 'root' })
export class ProductsService {
  private readonly http = inject(HttpClient);

  getCategories(): Observable<Category[]> {
    return this.http.get<Category[]>('/api/categorias');
  }

  createProduct(request: ProductCreateRequest): Observable<CreatedProduct> {
    return this.http.post<CreatedProduct>('/api/products', request);
  }
}
