import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

export const ADMIN_READONLY_ERROR_MESSAGE =
  'Operación no permitida: el perfil de administrador solo tiene permisos de lectura sobre productos.';

export interface AdminProductSummary {
  id: string;
  name: string;
  description?: string | null;
  price: number;
  stock: number;
  category: string;
  sellerId: string;
  sellerName: string;
  active: boolean;
}

export interface AdminProductDetail {
  id: string;
  name: string;
  description?: string | null;
  price: number;
  stock: number;
  category: string;
  sellerId: string;
  sellerName: string;
  active: boolean;
}

export interface AdminProductFilterParams {
  sellerId?: string;
  categoryId?: string;
  inStock?: boolean;
}

@Injectable({ providedIn: 'root' })
export class AdminProductService {
  private readonly http = inject(HttpClient);
  private readonly adminApiUrl = '/api/admin/products';

  getProducts(filters?: AdminProductFilterParams): Observable<AdminProductSummary[]> {
    let params = new HttpParams();

    if (filters?.sellerId?.trim()) {
      params = params.set('sellerId', filters.sellerId.trim());
    }

    if (filters?.categoryId?.trim()) {
      params = params.set('categoryId', filters.categoryId.trim());
    }

    if (filters?.inStock !== undefined && filters.inStock !== null) {
      params = params.set('inStock', filters.inStock.toString());
    }

    return this.http.get<AdminProductSummary[]>(this.adminApiUrl, {
      params,
      withCredentials: true,
    });
  }

  getProductById(id: string): Observable<AdminProductDetail> {
    return this.http.get<AdminProductDetail>(`${this.adminApiUrl}/${encodeURIComponent(id)}`, {
      withCredentials: true,
    });
  }

  deleteProductAsAdmin(id: string): Observable<void> {
    return this.http.delete<void>(`/api/products/${encodeURIComponent(id)}`, {
      withCredentials: true,
    });
  }
}

