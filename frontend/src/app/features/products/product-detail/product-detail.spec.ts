import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap, provideRouter } from '@angular/router';
import { signal } from '@angular/core';
import { of } from 'rxjs';
import { describe, beforeEach, it, expect, vi } from 'vitest';
import { ProductDetail } from './product-detail';
import { ProductsService, ProductItem, Category } from '../../../core/services/products.service';
import { WishlistService } from '../../../core/services/wishlist.service';
import { ToastService } from '../../../core/services/toast.service';
import { AuthService, AuthenticatedUser } from '../../../core/services/auth.service';
import { CartService } from '../../../core/services/cart.service';

describe('ProductDetail', () => {
  let component: ProductDetail;
  let fixture: ComponentFixture<ProductDetail>;
  let productsServiceMock: any;
  let wishlistServiceMock: any;
  let toastServiceMock: any;
  let cartServiceMock: any;
  let currentUserSignal = signal<AuthenticatedUser | null>(null);

  const mockProduct: ProductItem = {
    id: 'p-1',
    name: 'Smartphone Pro Max',
    categoryIds: ['cat-1'],
    stock: 8,
    description: 'Pantalla OLED 120Hz',
    price: 899.99,
    discount: 10,
    premiumDiscount: 15,
    imageUrl: 'https://example.com/phone.jpg',
    available: true,
    sellerId: 's-1',
    rating: 4.9,
  };

  const mockCategories: Category[] = [{ id: 'cat-1', name: 'Telefonía' }];

  beforeEach(async () => {
    currentUserSignal = signal<AuthenticatedUser | null>({
      id: 'client-1',
      email: 'client@example.com',
      role: 'CLIENT',
      active: true,
    });

    productsServiceMock = {
      getProductById: vi.fn(() => of(mockProduct)),
      getCategories: vi.fn(() => of(mockCategories)),
    };

    wishlistServiceMock = {
      refreshIfClient: vi.fn(),
      isFavorite: vi.fn((id: string) => id === 'p-1'),
      toggleWishlist: vi.fn(() => of({ message: 'Guardado', inWishlist: true })),
    };

    toastServiceMock = {
      show: vi.fn(),
    };

    cartServiceMock = {
      addItem: vi.fn(() => of({})),
      itemCount: vi.fn(() => 0),
    };

    await TestBed.configureTestingModule({
      imports: [ProductDetail],
      providers: [
        provideRouter([]),
        {
          provide: ActivatedRoute,
          useValue: { snapshot: { paramMap: convertToParamMap({ id: 'p-1' }) } },
        },
        { provide: ProductsService, useValue: productsServiceMock },
        { provide: WishlistService, useValue: wishlistServiceMock },
        { provide: ToastService, useValue: toastServiceMock },
        { provide: AuthService, useValue: { currentUser: currentUserSignal } },
        { provide: CartService, useValue: cartServiceMock },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(ProductDetail);
    component = fixture.componentInstance;
    fixture.detectChanges();
    await fixture.whenStable();
  });

  it('should create and render complete product details', () => {
    expect(component).toBeTruthy();
    const el: HTMLElement = fixture.nativeElement;

    expect(el.querySelector('.product-heading')?.textContent).toContain('Smartphone Pro Max');
    expect(el.querySelector('.price-value')?.textContent).toContain('899,99');
    expect(el.querySelector('.description-text')?.textContent).toContain('Pantalla OLED 120Hz');
    expect(el.querySelector('.status-available')?.textContent).toContain('En stock');
    expect(el.querySelector('.offers-panel')).not.toBeNull();
  });

  it('should block purchase and show warning if product is deactivated', () => {
    component.product.set({
      ...mockProduct,
      available: false,
    });
    fixture.detectChanges();

    const el: HTMLElement = fixture.nativeElement;
    expect(el.querySelector('.deactivated-alert')).not.toBeNull();
    expect(el.querySelector('.purchase-blocked-message')).not.toBeNull();
    const buyBtn = el.querySelector('.primary-buy-btn') as HTMLButtonElement;
    expect(buyBtn?.disabled).toBe(true);
  });

  it('should display admin read-only pill when logged in as admin', () => {
    currentUserSignal.set({
      id: 'admin-1',
      email: 'admin@example.com',
      role: 'ADMIN',
      active: true,
    });
    fixture.detectChanges();

    const el: HTMLElement = fixture.nativeElement;
    expect(el.querySelector('.admin-mode-pill')?.textContent).toContain('Modo Administrador (Solo lectura)');
  });

  it('should toggle favorite when heart button is clicked', () => {
    const el: HTMLElement = fixture.nativeElement;
    const heartBtn = el.querySelector('.heart-toggle-btn') as HTMLButtonElement;
    expect(heartBtn).not.toBeNull();
    heartBtn?.click();

    expect(wishlistServiceMock.toggleWishlist).toHaveBeenCalledWith('p-1');
    expect(toastServiceMock.show).toHaveBeenCalledWith('Guardado', 'success');
  });

  it('should allow changing quantity and adding to cart with success toast', () => {
    component.increaseQuantity();
    expect(component.quantity()).toBe(2);

    component.addToCart();
    expect(cartServiceMock.addItem).toHaveBeenCalledWith('p-1', 2);
    expect(toastServiceMock.show).toHaveBeenCalledWith('Producto añadido al carrito', 'success');
  });

  it('should redirect unauthenticated users to /login on favorite or buy click', () => {
    currentUserSignal.set(null);
    const navigateSpy = vi.spyOn((component as any).router, 'navigate').mockResolvedValue(true);

    component.toggleFavorite();
    expect(toastServiceMock.show).toHaveBeenCalledWith(
      'Debes iniciar sesión con tu cuenta de cliente para guardar favoritos.',
      'info'
    );
    expect(navigateSpy).toHaveBeenCalledWith(['/login']);

    component.onBuy();
    expect(toastServiceMock.show).toHaveBeenCalledWith(
      'Debes iniciar sesión con tu cuenta de cliente para realizar compras.',
      'info'
    );
    expect(navigateSpy).toHaveBeenCalledWith(['/login']);
  });
});
