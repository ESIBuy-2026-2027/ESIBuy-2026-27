import { Injectable, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, map, of, tap } from 'rxjs';
import { resolveImageUrl } from '../utils/image.util';
import { ProductItem } from '../services/products.service';
import { AuthService } from './auth.service';

export interface WishlistInfo {
  id: string;
  name: string;
  itemCount: number;
  productIds: string[];
  createdAt?: string;
}

@Injectable({ providedIn: 'root' })
export class WishlistService {
  private readonly http = inject(HttpClient);
  private readonly authService = inject(AuthService);

  readonly wishlistIds = signal<Set<string>>(new Set());
  readonly wishlists = signal<WishlistInfo[]>([]);

  // Estado del modal de selección de lista de deseos
  readonly selectModalOpen = signal(false);
  readonly targetProductId = signal<string | null>(null);
  readonly targetProductName = signal<string | null>(null);

  constructor() {
    this.refreshIfClient();
  }

  refreshIfClient(): void {
    const user = this.authService.currentUser();
    if (user && user.role === 'CLIENT') {
      this.getWishlists().subscribe({
        error: () => {
          this.wishlists.set([]);
          this.wishlistIds.set(new Set());
        },
      });
    } else {
      this.wishlists.set([]);
      this.wishlistIds.set(new Set());
    }
  }

  getWishlists(): Observable<WishlistInfo[]> {
    return this.http.get<WishlistInfo[]>('/api/wishlist/lists').pipe(
      tap((lists) => {
        this.wishlists.set(lists);
        this.recalculateWishlistIds(lists);
      }),
    );
  }

  createWishlist(name: string): Observable<WishlistInfo> {
    return this.http.post<WishlistInfo>('/api/wishlist/lists', { name }).pipe(
      tap((created) => {
        const next = [...this.wishlists(), created];
        this.wishlists.set(next);
      }),
    );
  }

  updateWishlistName(listId: string, name: string): Observable<WishlistInfo> {
    return this.http.put<WishlistInfo>(`/api/wishlist/lists/${listId}`, { name }).pipe(
      tap((updated) => {
        const next = this.wishlists().map((w) =>
          w.id === listId ? { ...w, name: updated.name } : w,
        );
        this.wishlists.set(next);
      }),
    );
  }

  deleteWishlist(listId: string): Observable<{ message: string }> {
    return this.http.delete<{ message: string }>(`/api/wishlist/lists/${listId}`).pipe(
      tap(() => {
        const remaining = this.wishlists().filter((w) => w.id !== listId);
        this.wishlists.set(remaining);
        this.recalculateWishlistIds(remaining);
      }),
    );
  }

  getWishlist(listId?: string): Observable<ProductItem[]> {
    const url = listId ? `/api/wishlist/lists/${listId}/products` : '/api/wishlist';
    return this.http.get<ProductItem[]>(url).pipe(
      map((products) =>
        products.map((p) => {
          if (p?.imageUrl === undefined) {
            return p;
          }
          return {
            ...p,
            imageUrl: resolveImageUrl(p.imageUrl),
          };
        }),
      ),
    );
  }

  getWishlistIds(): Observable<string[]> {
    return this.http
      .get<string[]>('/api/wishlist/ids')
      .pipe(tap((ids) => this.wishlistIds.set(new Set(ids))));
  }

  addToWishlist(
    productId: string,
    listId?: string,
  ): Observable<{ message: string; inWishlist: boolean }> {
    const url = listId
      ? `/api/wishlist/lists/${listId}/products/${productId}`
      : `/api/wishlist/${productId}`;
    return this.http.post<{ message: string; inWishlist: boolean }>(url, {}).pipe(
      tap(() => {
        const nextIds = new Set(this.wishlistIds());
        nextIds.add(productId);
        this.wishlistIds.set(nextIds);

        if (listId) {
          const updatedLists = this.wishlists().map((w) => {
            if (w.id === listId) {
              const pids = w.productIds.includes(productId)
                ? w.productIds
                : [...w.productIds, productId];
              return { ...w, productIds: pids, itemCount: pids.length };
            }
            return w;
          });
          this.wishlists.set(updatedLists);
        } else if (this.wishlists().length > 0) {
          const updatedLists = this.wishlists().map((w, idx) => {
            if (idx === 0) {
              const pids = w.productIds.includes(productId)
                ? w.productIds
                : [...w.productIds, productId];
              return { ...w, productIds: pids, itemCount: pids.length };
            }
            return w;
          });
          this.wishlists.set(updatedLists);
        }
      }),
    );
  }

  removeFromWishlist(
    productId: string,
    listId?: string,
  ): Observable<{ message: string; inWishlist: boolean }> {
    const url = listId
      ? `/api/wishlist/lists/${listId}/products/${productId}`
      : `/api/wishlist/${productId}`;
    return this.http.delete<{ message: string; inWishlist: boolean }>(url).pipe(
      tap(() => {
        let updatedLists: WishlistInfo[];
        if (listId) {
          updatedLists = this.wishlists().map((w) => {
            if (w.id === listId) {
              const pids = w.productIds.filter((id) => id !== productId);
              return { ...w, productIds: pids, itemCount: pids.length };
            }
            return w;
          });
        } else {
          updatedLists = this.wishlists().map((w) => {
            const pids = w.productIds.filter((id) => id !== productId);
            return { ...w, productIds: pids, itemCount: pids.length };
          });
        }
        this.wishlists.set(updatedLists);
        this.recalculateWishlistIds(updatedLists);
      }),
    );
  }

  isFavorite(productId: string): boolean {
    return this.wishlistIds().has(productId);
  }

  openWishlistSelector(productId: string, productName?: string): void {
    this.targetProductId.set(productId);
    this.targetProductName.set(productName ?? null);
    this.selectModalOpen.set(true);
  }

  closeWishlistSelector(): void {
    this.selectModalOpen.set(false);
    this.targetProductId.set(null);
    this.targetProductName.set(null);
  }

  toggleWishlist(
    productId: string,
    productName?: string,
  ): Observable<{ message: string; inWishlist: boolean }> {
    if (this.isFavorite(productId)) {
      return this.removeFromWishlist(productId);
    }

    const lists = this.wishlists();
    if (lists.length > 1) {
      this.openWishlistSelector(productId, productName);
      return of({ message: '', inWishlist: false });
    }

    const defaultListId = lists.length === 1 ? lists[0].id : undefined;
    return this.addToWishlist(productId, defaultListId);
  }

  private recalculateWishlistIds(lists: WishlistInfo[]): void {
    const set = new Set<string>();
    for (const list of lists) {
      if (list.productIds) {
        for (const pid of list.productIds) {
          set.add(pid);
        }
      }
    }
    this.wishlistIds.set(set);
  }
}
