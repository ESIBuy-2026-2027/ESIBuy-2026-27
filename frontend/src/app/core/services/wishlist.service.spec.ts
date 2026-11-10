import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { signal } from '@angular/core';
import { describe, beforeEach, afterEach, it, expect } from 'vitest';
import { WishlistService, WishlistInfo } from './wishlist.service';
import { AuthService, AuthenticatedUser } from './auth.service';

describe('WishlistService', () => {
  let service: WishlistService;
  let httpTesting: HttpTestingController;
  let currentUserSignal = signal<AuthenticatedUser | null>(null);

  const initialWishlists: WishlistInfo[] = [
    { id: 'list-1', name: 'Mi lista de deseos', itemCount: 1, productIds: ['prod-init'] },
  ];

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
    const initReq = httpTesting.match('/api/wishlist/lists');
    if (initReq.length > 0) {
      initReq[0].flush(initialWishlists);
    }
  });

  afterEach(() => {
    httpTesting.verify();
    TestBed.resetTestingModule();
  });

  it('should fetch user wishlists and set signals', () => {
    service.getWishlists().subscribe((lists) => {
      expect(lists.length).toBe(1);
      expect(lists[0].name).toBe('Mi lista de deseos');
      expect(service.isFavorite('prod-init')).toBe(true);
    });

    const req = httpTesting.expectOne('/api/wishlist/lists');
    expect(req.request.method).toBe('GET');
    req.flush(initialWishlists);
  });

  it('should fetch user wishlist products', () => {
    const mockProducts = [
      {
        id: 'prod-init',
        name: 'Producto 1',
        categoryIds: ['cat-1'],
        stock: 5,
        price: 99.99,
        available: true,
        sellerId: 's-1',
      },
    ];

    service.getWishlist('list-1').subscribe((data) => {
      expect(data).toEqual(mockProducts);
    });

    const req = httpTesting.expectOne('/api/wishlist/lists/list-1/products');
    expect(req.request.method).toBe('GET');
    req.flush(mockProducts);
  });

  it('should create new wishlist and update signal', () => {
    const newList: WishlistInfo = {
      id: 'list-2',
      name: 'Tecnología',
      itemCount: 0,
      productIds: [],
    };

    service.createWishlist('Tecnología').subscribe((res) => {
      expect(res.name).toBe('Tecnología');
      expect(service.wishlists().length).toBe(2);
    });

    const req = httpTesting.expectOne('/api/wishlist/lists');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ name: 'Tecnología' });
    req.flush(newList);
  });

  it('should update wishlist name', () => {
    service.updateWishlistName('list-1', 'Renombrada').subscribe((res) => {
      expect(res.name).toBe('Renombrada');
      expect(service.wishlists().find((w) => w.id === 'list-1')?.name).toBe('Renombrada');
    });

    const req = httpTesting.expectOne('/api/wishlist/lists/list-1');
    expect(req.request.method).toBe('PUT');
    req.flush({ id: 'list-1', name: 'Renombrada', itemCount: 1, productIds: ['prod-init'] });
  });

  it('should delete wishlist and keep product in favorites if present in another list', () => {
    // Setup 2 lists: List 1 has [p1, p2], List 2 has [p2, p3]
    service.wishlists.set([
      { id: 'list-1', name: 'L1', itemCount: 2, productIds: ['p1', 'p2'] },
      { id: 'list-2', name: 'L2', itemCount: 2, productIds: ['p2', 'p3'] },
    ]);
    service.wishlistIds.set(new Set(['p1', 'p2', 'p3']));

    service.deleteWishlist('list-1').subscribe();

    const req = httpTesting.expectOne('/api/wishlist/lists/list-1');
    expect(req.request.method).toBe('DELETE');
    req.flush({ message: 'Eliminada' });

    expect(service.wishlists().length).toBe(1);
    expect(service.wishlists()[0].id).toBe('list-2');
    // p1 was only in list-1 -> removed from favorites!
    expect(service.isFavorite('p1')).toBe(false);
    // p2 was in list-1 AND list-2 -> still favorite!
    expect(service.isFavorite('p2')).toBe(true);
    // p3 was in list-2 -> still favorite!
    expect(service.isFavorite('p3')).toBe(true);
  });

  it('should add item to specific wishlist and update signal', () => {
    service.addToWishlist('p-new', 'list-1').subscribe((res) => {
      expect(res.inWishlist).toBe(true);
      expect(service.isFavorite('p-new')).toBe(true);
    });

    const req = httpTesting.expectOne('/api/wishlist/lists/list-1/products/p-new');
    expect(req.request.method).toBe('POST');
    req.flush({ message: 'Añadido', inWishlist: true });
  });

  it('should remove item from wishlist and update signal', () => {
    service.removeFromWishlist('prod-init', 'list-1').subscribe((res) => {
      expect(res.inWishlist).toBe(false);
      expect(service.isFavorite('prod-init')).toBe(false);
    });

    const req = httpTesting.expectOne('/api/wishlist/lists/list-1/products/prod-init');
    expect(req.request.method).toBe('DELETE');
    req.flush({ message: 'Eliminado', inWishlist: false });
  });

  it('should toggle item directly when user has single wishlist', () => {
    // Only 1 wishlist exists
    expect(service.wishlists().length).toBe(1);

    // Toggle non-favorite -> directly calls addToWishlist
    service.toggleWishlist('p-brand-new').subscribe();

    const req = httpTesting.expectOne('/api/wishlist/lists/list-1/products/p-brand-new');
    expect(req.request.method).toBe('POST');
    req.flush({ message: 'Añadido', inWishlist: true });
  });

  it('should prompt modal when user has more than 1 wishlist and toggles new product', () => {
    service.wishlists.set([
      { id: 'list-1', name: 'L1', itemCount: 1, productIds: ['prod-init'] },
      { id: 'list-2', name: 'L2', itemCount: 0, productIds: [] },
    ]);

    service.toggleWishlist('p-unfavorite', 'Mi Producto').subscribe();

    expect(service.selectModalOpen()).toBe(true);
    expect(service.targetProductId()).toBe('p-unfavorite');
    expect(service.targetProductName()).toBe('Mi Producto');

    // No HTTP request should be sent immediately
    httpTesting.expectNone('/api/wishlist/lists/list-1/products/p-unfavorite');
    httpTesting.expectNone('/api/wishlist/p-unfavorite');
  });
});
