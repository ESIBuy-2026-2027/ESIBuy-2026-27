import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { signal } from '@angular/core';
import { describe, beforeEach, afterEach, it, expect, vi } from 'vitest';
import { WishlistService } from './wishlist.service';
import { AuthService, AuthenticatedUser } from './auth.service';

describe('WishlistService', () => {
  let service: WishlistService;
  let httpTesting: HttpTestingController;
  let currentUserSignal = signal<AuthenticatedUser | null>(null);

  beforeEach(() => {
    currentUserSignal = signal<AuthenticatedUser | null>({
      id: 'client-1',
      email: 'client@example.com',
      role: 'CLIENT',
      active: true,
    });

    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        {
          provide: AuthService,
          useValue: { currentUser: currentUserSignal },
        },
      ],
    });

    service = TestBed.inject(WishlistService);
    httpTesting = TestBed.inject(HttpTestingController);

    // Initial refresh request in constructor
    const initReq = httpTesting.match('/api/wishlist/ids');
    if (initReq.length > 0) {
      initReq[0].flush(['prod-init']);
    }
  });

  afterEach(() => httpTesting.verify());

  it('should fetch user wishlist products', () => {
    const mockProducts = [
      {
        id: 'p-1',
        name: 'Producto 1',
        categoryIds: ['cat-1'],
        stock: 5,
        price: 99.99,
        available: true,
        sellerId: 's-1',
      },
    ];

    service.getWishlist().subscribe((data) => {
      expect(data).toEqual(mockProducts);
    });

    const req = httpTesting.expectOne('/api/wishlist');
    expect(req.request.method).toBe('GET');
    req.flush(mockProducts);
  });

  it('should add item to wishlist and update signal', () => {
    service.addToWishlist('p-new').subscribe((res) => {
      expect(res.inWishlist).toBe(true);
      expect(service.isFavorite('p-new')).toBe(true);
    });

    const req = httpTesting.expectOne('/api/wishlist/p-new');
    expect(req.request.method).toBe('POST');
    req.flush({ message: 'Añadido', inWishlist: true });
  });

  it('should remove item from wishlist and update signal', () => {
    service.removeFromWishlist('prod-init').subscribe((res) => {
      expect(res.inWishlist).toBe(false);
      expect(service.isFavorite('prod-init')).toBe(false);
    });

    const req = httpTesting.expectOne('/api/wishlist/prod-init');
    expect(req.request.method).toBe('DELETE');
    req.flush({ message: 'Eliminado', inWishlist: false });
  });

  it('should toggle item correctly', () => {
    // prod-init is already favorite, so toggle should remove
    expect(service.isFavorite('prod-init')).toBe(true);

    service.toggleWishlist('prod-init').subscribe((res) => {
      expect(res.inWishlist).toBe(false);
    });

    const req = httpTesting.expectOne('/api/wishlist/prod-init');
    expect(req.request.method).toBe('DELETE');
    req.flush({ message: 'Eliminado', inWishlist: false });
  });
});

