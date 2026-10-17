import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { CartService } from './cart.service';

describe('CartService', () => {
  let service: CartService;
  let httpTesting: HttpTestingController;

  const cart = {
    id: 'cart-1',
    clienteId: 'client-1',
    items: [{
      productoId: 'product-1',
      nombre: '<Auriculares>',
      precioUnitario: 19.99,
      cantidad: 2,
      subtotal: 39.98,
    }],
    precioTotal: 39.98,
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(CartService);
    httpTesting = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpTesting.verify());

  it('should load cart contents from the backend', () => {
    service.load();

    httpTesting.expectOne('/api/carrito').flush(cart);

    expect(service.cart()).toEqual(cart);
    expect(service.itemCount()).toBe(2);
    expect(service.loading()).toBe(false);
  });

  it('should clear prior cart data while reloading', () => {
    service.cart.set(cart);

    service.load();

    expect(service.cart()).toBeNull();
    httpTesting.expectOne('/api/carrito').flush({
      id: 'cart-2',
      clienteId: 'client-2',
      items: [],
      precioTotal: 0,
    });
  });

  it('should submit product id and quantity and update the cart', () => {
    let result: unknown;

    service.addItem('product-1', 2).subscribe((response) => result = response);

    const request = httpTesting.expectOne('/api/carrito/items');
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual({ productoId: 'product-1', cantidad: 2 });
    request.flush(cart);

    expect(result).toEqual(cart);
    expect(service.cart()).toEqual(cart);
  });

  it('should reset cart state when cleared', () => {
    service.cart.set(cart);

    service.clear();

    expect(service.cart()).toBeNull();
    expect(service.itemCount()).toBe(0);
  });
});
