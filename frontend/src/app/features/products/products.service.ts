import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface Category {
  id: string;
  nombre: string;
}

export interface ProductCreateRequest {
  nombre: string;
  categoriaIds: string[];
  stock?: number;
  descripcion?: string;
  precio: number;
  descuento?: number;
  descuentoPremium?: number;
  imagen?: string;
  disponible: boolean;
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
