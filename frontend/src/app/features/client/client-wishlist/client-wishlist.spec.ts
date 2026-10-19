import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of } from 'rxjs';
import { describe, beforeEach, it, expect, vi } from 'vitest';
import { ClientWishlist } from './client-wishlist';
import { WishlistService } from '../../../core/services/wishlist.service';
import { ToastService } from '../../../core/services/toast.service';
import { ProductItem } from '../../products/products.service';

describe('ClientWishlist', () => {
  let component: ClientWishlist;
  let fixture: ComponentFixture<ClientWishlist>;
  let wishlistServiceMock: any;
  let toastServiceMock: any;

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

    await TestBed.configureTestingModule({
      imports: [ClientWishlist],
      providers: [
        provideRouter([]),
        { provide: WishlistService, useValue: wishlistServiceMock },
        { provide: ToastService, useValue: toastServiceMock },
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

    const navLinks = Array.from(el.querySelectorAll('.navigation-link'));
    expect(navLinks.length).toBe(5);
    expect(navLinks[0].textContent).toContain('Mi catálogo');
    expect(navLinks[1].textContent).toContain('Mi panel');
    expect(navLinks[2].textContent).toContain('Lista de deseos');
    expect(navLinks[2].classList.contains('is-active')).toBe(true);
    expect(navLinks[3].textContent).toContain('Mis compras');
    expect(navLinks[4].textContent).toContain('Mi perfil');
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
