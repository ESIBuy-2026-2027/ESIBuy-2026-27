import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap, provideRouter, Router } from '@angular/router';
import { signal } from '@angular/core';
import { of } from 'rxjs';
import { describe, beforeEach, it, expect, vi } from 'vitest';
import { ProductDetail } from './product-detail';
import { ProductsService, ProductItem, Category } from '../../../core/services/products.service';
import { WishlistService } from '../../../core/services/wishlist.service';
import { ToastService } from '../../../core/services/toast.service';
import { AuthService, AuthenticatedUser } from '../../../core/services/auth.service';
import { CartService } from '../../../core/services/cart.service';
import { ADMIN_READONLY_ERROR_MESSAGE } from '../../../core/services/admin-product.service';

describe('ProductDetail', () => {
  let component: ProductDetail;
  let fixture: ComponentFixture<ProductDetail>;
  let productsServiceMock: any;
  let wishlistServiceMock: any;
  let toastServiceMock: any;
  let cartServiceMock: any;
  let currentUserSignal = signal<AuthenticatedUser | null>(null);
  let cartSignal = signal<any>(null);

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
    sellerName: 'Móviles del Sur SL',
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

    const toastSignal = signal<any>(null);
    toastServiceMock = {
      toast: toastSignal,
      show: vi.fn((text, type = 'success') => toastSignal.set({ text, type })),
      clear: vi.fn(() => toastSignal.set(null)),
    };

    cartSignal = signal<any>(null);
    cartServiceMock = {
      cart: cartSignal,
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
    expect(el.querySelector('.price-value')?.textContent).toContain('809,99');
    expect(el.querySelector('.detail-strikethrough')?.textContent).toContain('899,99');
    expect(el.querySelector('.description-text')?.textContent).toContain('Pantalla OLED 120Hz');
    expect(el.querySelector('.status-available')?.textContent).toContain('En stock');
    expect(el.querySelector('.offers-panel')).not.toBeNull();
  });

  it('renders provider name between rating row and price section', () => {
    fixture.detectChanges();
    const el: HTMLElement = fixture.nativeElement;
    const vendorRow = el.querySelector('.product-vendor-row');
    expect(vendorRow).not.toBeNull();
    expect(vendorRow?.textContent).toContain('Proveedor:');
    expect(vendorRow?.textContent).toContain('Móviles del Sur SL');
  });

  it('shows low positive stock with the warning color', () => {
    component.product.set({ ...mockProduct, stock: 1 });
    fixture.detectChanges();

    const status = (fixture.nativeElement as HTMLElement).querySelector('.status-pill');
    expect(status?.classList.contains('status-low')).toBe(true);
    expect(status?.classList.contains('status-available')).toBe(false);
    expect(status?.textContent).toContain('En stock (1 disponibles)');
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

  it('should display admin read-only pill and hide purchase options when logged in as admin and hide interactive action buttons', () => {
    currentUserSignal.set({
      id: 'admin-1',
      email: 'admin@example.com',
      role: 'ADMIN',
      active: true,
    });
    fixture.detectChanges();

    const el: HTMLElement = fixture.nativeElement;
    expect(el.querySelector('.heart-toggle-btn')).toBeNull();
    expect(el.querySelector('.admin-readonly-box')).not.toBeNull();
    expect(el.querySelector('.admin-readonly-box')?.textContent).toContain('Modo supervisión: solo lectura');
    expect(el.querySelector('.primary-buy-btn')).toBeNull();
    expect(el.querySelector('.detail-quantity-picker')).toBeNull();
  });

  it('should reject buy and favorite mutations when logged in as admin with clear error message', () => {
    currentUserSignal.set({
      id: 'admin-1',
      email: 'admin@example.com',
      role: 'ADMIN',
      active: true,
    });
    fixture.detectChanges();

    component.addToCart();
    expect(toastServiceMock.show).toHaveBeenCalledWith(ADMIN_READONLY_ERROR_MESSAGE, 'error');
    expect(cartServiceMock.addItem).not.toHaveBeenCalled();

    component.toggleFavorite();
    expect(toastServiceMock.show).toHaveBeenCalledWith(ADMIN_READONLY_ERROR_MESSAGE, 'error');
    expect(wishlistServiceMock.toggleWishlist).not.toHaveBeenCalled();
    const el: HTMLElement = fixture.nativeElement;
    expect(el.querySelector('.heart-toggle-btn')).toBeNull();
    expect(el.querySelector('.primary-buy-btn')).toBeNull();
  });

  it('should navigate to /client/catalog on goBack when CLIENT or ADMIN', () => {
    const router = TestBed.inject(Router);
    const navigateSpy = vi.spyOn(router, 'navigate').mockResolvedValue(true);

    component.goBack();
    expect(navigateSpy).toHaveBeenCalledWith(['/client/catalog']);
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
  it('caps keyboard input to available stock and updates quantity', () => {
    const input = document.createElement('input');
    input.value = '999';
    component.onQuantityInput({ target: input } as any);
    expect(input.value).toBe('8');
    expect(component.quantity()).toBe(8);

    input.value = '-5';
    component.onQuantityInput({ target: input } as any);
    expect(input.value).toBe('1');
    expect(component.quantity()).toBe(1);
  });

  it('updates available stock dynamically subtracting items already in cart', () => {
    cartSignal.set({
      id: 'cart-1',
      clienteId: 'client-1',
      items: [{ productoId: 'p-1', cantidad: 5 }],
      total: 4049.95,
    });
    fixture.detectChanges();

    expect(component.availableStock()).toBe(3);

    // When all stock is in cart
    cartSignal.set({
      id: 'cart-1',
      clienteId: 'client-1',
      items: [{ productoId: 'p-1', cantidad: 8 }],
      total: 6479.92,
    });
    fixture.detectChanges();

    expect(component.availableStock()).toBe(0);
    const el: HTMLElement = fixture.nativeElement;
    expect(el.querySelector('.status-out')?.textContent).toContain('Agotado');
  });


  it('shows the Premium price and additional savings to a standard client', () => {
    fixture.detectChanges();

    const el: HTMLElement = fixture.nativeElement;

    expect(el.querySelector('.price-value')?.textContent).toContain('809,99');
    expect(el.querySelector('.detail-premium-value')?.textContent).toContain('674,99');
    expect(el.querySelector('.premium-savings')?.textContent).toContain('135,00');
    expect(el.querySelector('.premium-savings')?.textContent).toContain('Ahorrarías');
  });

  it('shows the Premium price as the main price for a Premium client', () => {
    currentUserSignal.set({
      id: 'premium-client-1',
      email: 'premium@example.com',
      role: 'CLIENT',
      active: true,
      customerType: 'PREMIUM',
    });

    fixture.detectChanges();

    const el: HTMLElement = fixture.nativeElement;

    expect(el.querySelector('.price-value')?.textContent).toContain('674,99');
    expect(el.querySelector('.detail-premium-price-row')).toBeNull();
    expect(el.querySelector('.premium-savings')).toBeNull();
    expect(el.querySelector('.premium-active-note')).not.toBeNull();
  });


});