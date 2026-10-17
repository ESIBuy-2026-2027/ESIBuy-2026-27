import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { CartService } from './cart.service';
import { ClientCart } from './cart';

describe('ClientCart', () => {
  it('should render cart item names as text and show the backend total', async () => {
    const cart = signal({
      id: 'cart-1',
      clienteId: 'client-1',
      items: [{
        productoId: 'product-1',
        nombre: '<script>alert(1)</script>',
        precioUnitario: 12.5,
        cantidad: 2,
        subtotal: 25,
      }],
      precioTotal: 25,
    });

    await TestBed.configureTestingModule({
      imports: [ClientCart],
      providers: [
        provideRouter([]),
        {
          provide: CartService,
          useValue: {
            cart,
            loading: signal(false),
            loadError: signal(null),
            itemCount: () => 2,
          },
        },
      ],
    }).compileComponents();

    const fixture = TestBed.createComponent(ClientCart);
    fixture.detectChanges();

    const element = fixture.nativeElement as HTMLElement;
    expect(element.querySelector('.item-information h2')?.textContent).toBe('<script>alert(1)</script>');
    expect(element.querySelector('.item-information h2 script')).toBeNull();
    expect(element.querySelector('.total-line')?.textContent).toContain('25,00 €');
  });
});
