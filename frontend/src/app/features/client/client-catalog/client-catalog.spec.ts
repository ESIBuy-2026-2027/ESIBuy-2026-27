import { signal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of } from 'rxjs';
import { describe, beforeEach, it, expect, vi } from 'vitest';
import { ClientCatalog } from './client-catalog';
import { ProductsService, ProductItem, Category } from '../../../core/services/products.service';
import { WishlistService } from '../../../core/services/wishlist.service';
import { ToastService } from '../../../core/services/toast.service';
import { CartService } from '../../../core/services/cart.service';
import { AuthService, AuthenticatedUser } from '../../../core/services/auth.service';

describe('ClientCatalog', () => {
  let component: ClientCatalog;
  let fixture: ComponentFixture<ClientCatalog>;
  let productsServiceMock: any;
  let wishlistServiceMock: any;
  let toastServiceMock: any;
  let cartServiceMock: any;
  let currentUserSignal = signal<AuthenticatedUser | null>(null);

  const mockCategories: Category[] = [
    { id: 'cat-1', name: 'Electrónica' },
    { id: 'cat-2', name: 'Hogar' },
  ];

  const mockProducts: ProductItem[] = [
    {
      id: 'p-1',
      name: 'Auriculares Bluetooth',
      categoryIds: ['cat-1'],
      stock: 10,
      description: 'Gran calidad de sonido',
      price: 29.99,
      available: true,
      sellerId: 's-1',
      rating: 4.8,
    },
    {
      id: 'p-2',
      name: 'Cafetera Express',
      categoryIds: ['cat-2'],
      stock: 2,
      description: 'Café rápido',
      price: 89.99,
      available: true,
      sellerId: 's-2',
      rating: 4.5,
    },
  ];

  beforeEach(async () => {
    productsServiceMock = {
      getCategories: vi.fn(() => of(mockCategories)),
      getPublicCatalog: vi.fn(() => of(mockProducts)),
    };

    wishlistServiceMock = {
      refreshIfClient: vi.fn(),
      isFavorite: vi.fn((id: string) => id === 'p-1'),
      toggleWishlist: vi.fn(() => of({ message: 'Lista actualizada', inWishlist: true })),
    };

    const toastSignal = signal<any>(null);
    toastServiceMock = {
      toast: toastSignal,
      show: vi.fn((text, type = 'success') => toastSignal.set({ text, type })),
      clear: vi.fn(() => toastSignal.set(null)),
    };

    currentUserSignal = signal<AuthenticatedUser | null>({
      id: 'client-1',
      email: 'client@example.com',
      role: 'CLIENT',
      active: true,
    });

    cartServiceMock = {
      itemCount: vi.fn(() => 2),
      addItem: vi.fn(() => of({})),
    };

    await TestBed.configureTestingModule({
      imports: [ClientCatalog],
      providers: [
        provideRouter([]),
        { provide: ProductsService, useValue: productsServiceMock },
        { provide: WishlistService, useValue: wishlistServiceMock },
        { provide: ToastService, useValue: toastServiceMock },
        { provide: CartService, useValue: cartServiceMock },
        { provide: AuthService, useValue: { currentUser: currentUserSignal } },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(ClientCatalog);
    component = fixture.componentInstance;
    fixture.detectChanges();
    await fixture.whenStable();
  });

  it('should create and render heading and navigation links in exact order', () => {
    expect(component).toBeTruthy();
    const el: HTMLElement = fixture.nativeElement;

    expect(el.querySelector('#catalog-title')?.textContent).toContain('Catálogo de productos');
    const navLinks = Array.from(el.querySelectorAll('.navigation-link'));
    expect(navLinks.length).toBe(5);
    expect(navLinks[0].textContent).toContain('Catálogo');
    expect(navLinks[0].classList.contains('is-active')).toBe(true);
    expect(navLinks[1].textContent).toContain('Lista de deseos');
    expect(navLinks[2].textContent).toContain('Mis compras');
    expect(navLinks[3].textContent).toContain('Carrito');
    expect(navLinks[4].textContent).toContain('Perfil');
  });

  it('should toggle sidebar collapsed state', () => {
    expect(component.sidebarCollapsed()).toBe(false);
    component.toggleSidebar();
    expect(component.sidebarCollapsed()).toBe(true);
  });

  it('should add product to cart and show toast', () => {
    const el: HTMLElement = fixture.nativeElement;
    const addBtn = el.querySelector('.add-to-cart-action-btn') as HTMLButtonElement;
    expect(addBtn).not.toBeNull();
    addBtn?.click();

    expect(cartServiceMock.addItem).toHaveBeenCalledWith('p-1', 1);
    expect(toastServiceMock.show).toHaveBeenCalledWith('Producto añadido al carrito', 'success');
  });

  it('should render products and allow toggling favorite with feedback', () => {
    fixture.detectChanges();
    const el: HTMLElement = fixture.nativeElement;

    const cards = el.querySelectorAll('.product-card');
    expect(cards.length).toBe(2);

    const firstTitle = cards[0].querySelector('.product-card-title')?.textContent;
    expect(firstTitle).toContain('Auriculares Bluetooth');

    // Click heart on first product
    const heartBtn = cards[0].querySelector('.heart-center-button') as HTMLButtonElement;
    expect(heartBtn).not.toBeNull();
    heartBtn?.click();

    expect(wishlistServiceMock.toggleWishlist).toHaveBeenCalledWith('p-1');
    expect(toastServiceMock.show).toHaveBeenCalledWith('Lista actualizada', 'success');
  });

  it('should reload products when filters change', () => {
    component.searchQuery.set('Cafetera');
    component.onFilterChange();

    expect(productsServiceMock.getPublicCatalog).toHaveBeenCalledWith(
      expect.objectContaining({ query: 'Cafetera' })
    );
  });

  it('should render admin read-only mode without wishlist, add-to-cart or client sidebar for ADMIN', () => {
    currentUserSignal.set({
      id: 'admin-1',
      email: 'admin@example.com',
      role: 'ADMIN',
      active: true,
    });
    fixture.detectChanges();

    const el: HTMLElement = fixture.nativeElement;
    expect(el.querySelector('.admin-readonly-badge, .admin-mode-pill')?.textContent).toContain('Modo Administrador (Solo lectura)');
    expect(el.querySelector('.heart-center-button')).toBeNull();
    expect(el.querySelector('.add-to-cart-action-btn')).toBeNull();

    // The left sidebar must NOT be rendered for admin
    expect(el.querySelector('.sidebar')).toBeNull();
    expect(el.querySelector('.app-sidebar')).toBeNull();
    expect(el.querySelectorAll('.navigation-link').length).toBe(0);
  });
});
