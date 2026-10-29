import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, map } from 'rxjs';
import { resolveImageUrl } from '../utils/image.util';

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
  rating?: number;
}

@Injectable({ providedIn: 'root' })
export class SellerCatalogService {
  private readonly http = inject(HttpClient);

  getProducts(): Observable<SellerProduct[]> {
    return this.http.get<SellerProduct[]>('/api/products/mine').pipe(
      map((products) =>
        products.map((p) => ({
          ...p,
          imageUrl: resolveImageUrl(p.imageUrl),
        }))
      )
    );
  }
}
