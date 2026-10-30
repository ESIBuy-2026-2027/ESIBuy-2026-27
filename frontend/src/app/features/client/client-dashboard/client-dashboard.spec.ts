import { HttpErrorResponse } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { describe, beforeEach, it, expect, vi } from 'vitest';
import { of, throwError } from 'rxjs';
import { CartService } from '../../../core/services/cart.service';
import { ProductsService } from '../../../core/services/products.service';
import { ClientDashboard } from './client-dashboard';

describe('ClientDashboard', () => {
  const product = {
    id: 'product-1',
    name: '<img src=x onerror=alert(1)>',
    categoryIds: [],
    stock: 4,
    description: null,
    price: 10,
    discount: null,
    premiumDiscount: null,
    imageUrl: null,
    available: true,
    sellerId: 'seller-1',
  };
  let getAvailableProducts: ReturnType<typeof vi.fn>;
  let addItem: ReturnType<typeof vi.fn>;

  beforeEach(async () => {
    getAvailableProducts = vi.fn().mockReturnValue(of([product]));
    addItem = vi.fn().mockReturnValue(of({
      id: 'cart-1',
      clienteId: 'client-1',
      items: [],
      precioTotal: 0,
    }));
    await TestBed.configureTestingModule({
      imports: [ClientDashboard],
      providers: [
        provideRouter([]),
        { provide: ProductsService, useValue: { getAvailableProducts } },
        { provide: CartService, useValue: { itemCount: () => 0, addItem } },
      ],
    }).compileComponents();
  });

  it('should render navigation links with Catálogo active', () => {
    const fixture = TestBed.createComponent(ClientDashboard);
    fixture.detectChanges();

    const el: HTMLElement = fixture.nativeElement;
    const navLinks = Array.from(el.querySelectorAll('.navigation-link'));
    expect(navLinks.length).toBe(5);
    expect(navLinks[0].textContent).toContain('Catálogo');
    expect(navLinks[0].classList.contains('is-active')).toBe(true);
    expect(navLinks[1].textContent).toContain('Lista de deseos');
    expect(navLinks[2].textContent).toContain('Mis pedidos');
    expect(navLinks[3].textContent).toContain('Carrito');
    expect(navLinks[4].textContent).toContain('Perfil');
  });

  it('should render product names as text and not create markup from them', () => {
    const fixture = TestBed.createComponent(ClientDashboard);
    fixture.detectChanges();

    const element = fixture.nativeElement as HTMLElement;
    expect(element.querySelector('.product-details h2')?.textContent).toBe(product.name);
    expect(element.querySelector('.product-details h2 img')).toBeNull();
    expect(element.querySelector('input[type="number"]')?.getAttribute('max')).toBe('4');
  });

  it('should submit the selected product quantity', () => {
    const fixture = TestBed.createComponent(ClientDashboard);
    fixture.detectChanges();
    fixture.componentInstance.setQuantity('product-1', 3);

    const button = (fixture.nativeElement as HTMLElement).querySelector<HTMLButtonElement>('.add-to-cart');
    button?.click();

    expect(addItem).toHaveBeenCalledWith('product-1', 3);
    expect(fixture.componentInstance.notification()?.text).toBe('Producto añadido al carrito.');
  });

  it('should display stock conflict messages returned by the API', () => {
    addItem.mockReturnValue(throwError(() => new HttpErrorResponse({
      status: 409,
      error: { message: 'No hay stock suficiente para la cantidad solicitada' },
    })));
    const fixture = TestBed.createComponent(ClientDashboard);
    fixture.detectChanges();

    fixture.componentInstance.addToCart(product);

    expect(fixture.componentInstance.notification()?.text)
      .toBe('No hay stock suficiente para la cantidad solicitada');
    expect(fixture.componentInstance.notification()?.type).toBe('error');
  });
});
