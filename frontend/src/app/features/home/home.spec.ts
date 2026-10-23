import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { signal } from '@angular/core';
import { of } from 'rxjs';
import { describe, beforeEach, it, expect, vi } from 'vitest';
import { Home } from './home';
import { ProductsService, ProductItem, Category } from '../../core/services/products.service';
import { WishlistService } from '../../core/services/wishlist.service';
import { ToastService } from '../../core/services/toast.service';
import { AuthService, AuthenticatedUser } from '../../core/services/auth.service';
import { CartService } from '../../core/services/cart.service';

describe('Home', () => {
  let component: Home;
  let fixture: ComponentFixture<Home>;
  let productsServiceMock: any;
  let wishlistServiceMock: any;
  let toastServiceMock: any;
  let cartServiceMock: any;
  let currentUserSignal = signal<AuthenticatedUser | null>(null);

  const mockCategories: Category[] = [
    { id: 'cat-1', name: 'Libros' },
    { id: 'cat-2', name: 'Material' },
  ];

  const mockProducts: ProductItem[] = [
    {
      id: 'p-1',
      name: 'Libro de Cálculo',
      categoryIds: ['cat-1'],
      stock: 5,
      description: 'Libro universitario',
      price: 25.0,
      available: true,
      sellerId: 's-1',
      rating: 4.7,
    },
  ];

  beforeEach(async () => {
    currentUserSignal = signal<AuthenticatedUser | null>(null);

    productsServiceMock = {
      getCategories: vi.fn(() => of(mockCategories)),
      getPublicCatalog: vi.fn(() => of(mockProducts)),
    };

    wishlistServiceMock = {
      refreshIfClient: vi.fn(),
      isFavorite: vi.fn(() => false),
      toggleWishlist: vi.fn(() => of({ message: 'Añadido a favoritos', inWishlist: true })),
    };

    toastServiceMock = {
      show: vi.fn(),
    };

    cartServiceMock = {
      addItem: vi.fn(() => of({})),
      itemCount: vi.fn(() => 0),
    };

    await TestBed.configureTestingModule({
      imports: [Home],
      providers: [
        provideRouter([]),
        { provide: ProductsService, useValue: productsServiceMock },
        { provide: WishlistService, useValue: wishlistServiceMock },
        { provide: ToastService, useValue: toastServiceMock },
        { provide: AuthService, useValue: { currentUser: currentUserSignal } },
        { provide: CartService, useValue: cartServiceMock },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(Home);
    component = fixture.componentInstance;
    fixture.detectChanges();
    await fixture.whenStable();
  });

  it('should render the public showcase and products', () => {
    expect(component).toBeTruthy();
    const el: HTMLElement = fixture.nativeElement;

    expect(el.querySelector('#hero-title')?.textContent).toContain('Compra con confianza');
    expect(el.querySelector('#public-search')).not.toBeNull();
    expect(el.querySelector('.filters-panel')).not.toBeNull();

    const cards = el.querySelectorAll('.showcase-card');
    expect(cards.length).toBe(1);
    expect(cards[0].querySelector('.card-title')?.textContent).toContain('Libro de Cálculo');
  });

  it('should redirect unauthenticated users to /login when clicking the heart icon', () => {
    const router = TestBed.inject(Router);
    const navigateSpy = vi.spyOn(router, 'navigate').mockResolvedValue(true);

    const el: HTMLElement = fixture.nativeElement;
    const heartBtn = el.querySelector('.center-heart-action') as HTMLButtonElement;
    expect(heartBtn).not.toBeNull();

    heartBtn.click();

    expect(toastServiceMock.show).toHaveBeenCalledWith(
      'Debes iniciar sesión con tu cuenta de cliente para guardar favoritos.',
      'info'
    );
    expect(navigateSpy).toHaveBeenCalledWith(['/login']);
    expect(wishlistServiceMock.toggleWishlist).not.toHaveBeenCalled();
  });

  it('should redirect unauthenticated users to /login when clicking añadir al carrito', () => {
    const router = TestBed.inject(Router);
    const navigateSpy = vi.spyOn(router, 'navigate').mockResolvedValue(true);

    const el: HTMLElement = fixture.nativeElement;
    const addBtn = el.querySelector('.add-to-cart-action-btn') as HTMLButtonElement;
    expect(addBtn).not.toBeNull();
    addBtn.click();

    expect(toastServiceMock.show).toHaveBeenCalledWith(
      'Debes iniciar sesión con tu cuenta de cliente para realizar compras.',
      'info'
    );
    expect(navigateSpy).toHaveBeenCalledWith(['/login']);
  });

  it('should add to cart when authenticated as CLIENT', () => {
    currentUserSignal.set({
      id: 'c-1',
      email: 'client@example.com',
      role: 'CLIENT',
      active: true,
    });
    fixture.detectChanges();

    const el: HTMLElement = fixture.nativeElement;
    const addBtn = el.querySelector('.add-to-cart-action-btn') as HTMLButtonElement;
    addBtn.click();

    expect(cartServiceMock.addItem).toHaveBeenCalledWith('p-1', 1);
    expect(toastServiceMock.show).toHaveBeenCalledWith('Producto añadido al carrito', 'success');
  });

  it('should toggle favorite when authenticated as CLIENT', () => {
    currentUserSignal.set({
      id: 'c-1',
      email: 'client@example.com',
      role: 'CLIENT',
      active: true,
    });
    fixture.detectChanges();

    const el: HTMLElement = fixture.nativeElement;
    const heartBtn = el.querySelector('.center-heart-action') as HTMLButtonElement;
    heartBtn.click();

    expect(wishlistServiceMock.toggleWishlist).toHaveBeenCalledWith('p-1');
    expect(toastServiceMock.show).toHaveBeenCalledWith('Añadido a favoritos', 'success');
  });
});

