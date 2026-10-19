import { Injectable, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap } from 'rxjs';
import { ProductItem } from '../services/products.service'
import { AuthService } from './auth.service';

@Injectable({ providedIn: 'root' })
export class WishlistService {
  private readonly http = inject(HttpClient);
  private readonly authService = inject(AuthService);

  readonly wishlistIds = signal<Set<string>>(new Set());

  constructor() {
    this.refreshIfClient();
  }

  refreshIfClient(): void {
    const user = this.authService.currentUser();
    if (user && user.role === 'CLIENT') {
      this.getWishlistIds().subscribe({
        next: (ids) => this.wishlistIds.set(new Set(ids)),
        error: () => this.wishlistIds.set(new Set())
      });
    } else {
      this.wishlistIds.set(new Set());
    }
  }

  getWishlist(): Observable<ProductItem[]> {
    return this.http.get<ProductItem[]>('/api/wishlist');
  }

  getWishlistIds(): Observable<string[]> {
    return this.http.get<string[]>('/api/wishlist/ids').pipe(
      tap((ids) => this.wishlistIds.set(new Set(ids)))
    );
  }

  addToWishlist(productId: string): Observable<{ message: string; inWishlist: boolean }> {
    return this.http.post<{ message: string; inWishlist: boolean }>(`/api/wishlist/${productId}`, {}).pipe(
      tap(() => {
        const next = new Set(this.wishlistIds());
        next.add(productId);
        this.wishlistIds.set(next);
      })
    );
  }

  removeFromWishlist(productId: string): Observable<{ message: string; inWishlist: boolean }> {
    return this.http.delete<{ message: string; inWishlist: boolean }>(`/api/wishlist/${productId}`).pipe(
      tap(() => {
        const next = new Set(this.wishlistIds());
        next.delete(productId);
        this.wishlistIds.set(next);
      })
    );
  }

  isFavorite(productId: string): boolean {
    return this.wishlistIds().has(productId);
  }

  toggleWishlist(productId: string): Observable<{ message: string; inWishlist: boolean }> {
    if (this.isFavorite(productId)) {
      return this.removeFromWishlist(productId);
    } else {
      return this.addToWishlist(productId);
    }
  }
}

