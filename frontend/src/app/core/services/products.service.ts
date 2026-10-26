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
  rating?: number;
}

export type AvailableProduct = ProductDetails;

@Injectable({ providedIn: 'root' })
export class ProductsService {
  private readonly http = inject(HttpClient);

  getCategories(): Observable<Category[]> {
    return this.http.get<Category[]>('/api/categorias');
  }

  createProduct(request: ProductCreateRequest): Observable<CreatedProduct> {
    return this.http.post<CreatedProduct>('/api/products', request);
  }

  createProductWithImage(request: ProductCreateRequest, image?: File | null): Observable<CreatedProduct> {
    const formData = new FormData();
    formData.append('product', new Blob([JSON.stringify(request)], { type: 'application/json' }));
    if (image) formData.append('image', image, image.name);
    return this.http.post<CreatedProduct>('/api/products', formData);
  }

  getProduct(id: string): Observable<ProductDetails> {
    return this.http.get<ProductDetails>(`/api/products/${id}`);
  }

  updateProduct(id: string, request: ProductCreateRequest): Observable<ProductDetails> {
    return this.http.put<ProductDetails>(`/api/products/${id}`, request);
  }

  updateProductWithImage(id: string, request: ProductCreateRequest, image?: File | null): Observable<ProductDetails> {
    const formData = new FormData();
    formData.append('product', new Blob([JSON.stringify(request)], { type: 'application/json' }));
    if (image) formData.append('image', image, image.name);
    return this.http.put<ProductDetails>(`/api/products/${id}`, formData);
  }

  uploadProductImage(id: string, image: File): Observable<ProductDetails> {
    const formData = new FormData();
    formData.append('image', image, image.name);
    return this.http.post<ProductDetails>(`/api/products/${id}/image`, formData);
  }

  getAvailableProducts(): Observable<AvailableProduct[]> {
    return this.http.get<AvailableProduct[]>('/api/catalog/products');
  }

  getPublicCatalog(filters?: CatalogFilterParams): Observable<ProductItem[]> {
    const params = this.buildCatalogParams(filters);
    return this.http.get<ProductItem[]>('/api/products', { params });
  }

  private buildCatalogParams(filters?: CatalogFilterParams): HttpParams {
    let params = new HttpParams();

    if (!filters) {
      return params;
    }

    const query = filters.query?.trim();
    const categoryId = filters.categoryId?.trim();

    if (query) {
      params = params.set('query', query);
    }

    if (categoryId) {
      params = params.set('categoryId', categoryId);
    }

    params = this.setNumericParam(params, 'minPrice', filters.minPrice);
    params = this.setNumericParam(params, 'maxPrice', filters.maxPrice);
    params = this.setNumericParam(params, 'minRating', filters.minRating);

    if (filters.inStock !== undefined && filters.inStock !== null) {
      params = params.set('inStock', filters.inStock.toString());
    }

    return params;
  }

  private setNumericParam(
    params: HttpParams,
    name: string,
    value: number | undefined
  ): HttpParams {
    if (value === undefined || value === null) {
      return params;
    }

    return params.set(name, value.toString());
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
