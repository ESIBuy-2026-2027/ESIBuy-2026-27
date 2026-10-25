import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of } from 'rxjs';
import { describe, beforeEach, it, expect, vi } from 'vitest';
import { ClientWishlist } from './client-wishlist';
import { WishlistService } from '../../../core/services/wishlist.service';
import { ToastService } from '../../../core/services/toast.service';
import { CartService } from '../../../core/services/cart.service';
import { ProductItem } from '../../../core/services/products.service';

describe('ClientWishlist', () => {
  let component: ClientWishlist;
  let fixture: ComponentFixture<ClientWishlist>;
  let wishlistServiceMock: any;
  let toastServiceMock: any;
  let cartServiceMock: any;

  const mockWishlistProducts: ProductItem[] = [
    {
      id: 'p-1',
      name: 'Monitor Gaming',
      categoryIds: ['cat-1'],
      stock: 3,
      description: '144Hz IPS',
      price: 199.99,
      available: true,
      sellerId: 's-1',
    },
    {
      id: 'p-2',
      name: 'Teclado Mecánico (Agotado/Pausado)',
      categoryIds: ['cat-1'],
      stock: 0,
      description: 'Switches rojos',
      price: 59.99,
      available: false,
      sellerId: 's-2',
    },
  ];

  beforeEach(async () => {
    wishlistServiceMock = {
      getWishlist: vi.fn(() => of(mockWishlistProducts)),
      removeFromWishlist: vi.fn(() => of({ message: 'Producto eliminado', inWishlist: false })),
    };

    toastServiceMock = {
      show: vi.fn(),
    };

    cartServiceMock = {
      addItem: vi.fn(() => of({})),
      itemCount: vi.fn(() => 0),
    };

    await TestBed.configureTestingModule({
      imports: [ClientWishlist],
      providers: [
        provideRouter([]),
        { provide: WishlistService, useValue: wishlistServiceMock },
        { provide: ToastService, useValue: toastServiceMock },
        { provide: CartService, useValue: cartServiceMock },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(ClientWishlist);
    component = fixture.componentInstance;
    fixture.detectChanges();
    await fixture.whenStable();
  });

  it('should create and render navigation order in sidebar', () => {
    expect(component).toBeTruthy();
    const el: HTMLElement = fixture.nativeElement;

    expect(el.querySelector('#wishlist-title')?.textContent).toContain('Lista de deseos');

    const navLinks = Array.from(el.querySelectorAll('.navigation-link'));
    expect(navLinks.length).toBe(5);
    expect(navLinks[0].textContent).toContain('Catálogo');
    expect(navLinks[1].textContent).toContain('Lista de deseos');
    expect(navLinks[1].classList.contains('is-active')).toBe(true);
    expect(navLinks[2].textContent).toContain('Mis compras');
    expect(navLinks[3].textContent).toContain('Carrito');
    expect(navLinks[4].textContent).toContain('Perfil');
  });

  it('should toggle sidebar collapsed state', () => {
    expect(component.sidebarCollapsed()).toBe(false);
    component.toggleSidebar();
    expect(component.sidebarCollapsed()).toBe(true);
  });

  it('should add item to cart with selected quantity', () => {
    component.setQuantity('p-1', 2, 3);
    fixture.detectChanges();

    const el: HTMLElement = fixture.nativeElement;
    const buyBtn = el.querySelector('.buy-action-btn') as HTMLButtonElement;
    expect(buyBtn.textContent).toContain('Añadir al carrito');
    buyBtn.click();

    expect(cartServiceMock.addItem).toHaveBeenCalledWith('p-1', 2);
    expect(toastServiceMock.show).toHaveBeenCalledWith('Producto añadido al carrito', 'success');
  });

  it('should render items in wishlist and indicate unavailable products', () => {
    fixture.detectChanges();
    const el: HTMLElement = fixture.nativeElement;

    const cards = el.querySelectorAll('.wishlist-card');
    expect(cards.length).toBe(2);

    // Second product is unavailable
    const secondCard = cards[1];
    expect(secondCard.classList.contains('is-unavailable')).toBe(true);
    expect(secondCard.querySelector('.blocked-badge')?.textContent).toContain('No disponible para compra');
    expect((secondCard.querySelector('.buy-action-btn') as HTMLButtonElement)?.disabled).toBe(true);
  });

  it('should remove item when delete button is clicked', () => {
    fixture.detectChanges();
    const el: HTMLElement = fixture.nativeElement;

    const removeBtn = el.querySelector('.remove-btn') as HTMLButtonElement;
    expect(removeBtn).not.toBeNull();
    removeBtn?.click();

    expect(wishlistServiceMock.removeFromWishlist).toHaveBeenCalledWith('p-1');
    expect(toastServiceMock.show).toHaveBeenCalledWith('Producto eliminado', 'success');
  });
});
