import { HttpErrorResponse } from '@angular/common/http';
import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of, Subject, throwError } from 'rxjs';
import { CartService } from '../../../core/services/cart.service';
import { PedidoService } from '../../../core/services/pedido.service';
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
        { provide: PedidoService, useValue: { obtenerCheckoutResumen: vi.fn(), crearPedido: vi.fn() } },
      ],
    }).compileComponents();

    const fixture = TestBed.createComponent(ClientCart);
    fixture.detectChanges();

    const element = fixture.nativeElement as HTMLElement;
    expect(element.querySelector('.item-information h2')?.textContent).toBe('<script>alert(1)</script>');
    expect(element.querySelector('.item-information h2 script')).toBeNull();
    expect(element.querySelector('.total-line')?.textContent).toContain('25,00 €');
  });

  it('should submit quantity changes and prevent concurrent updates for the same item', async () => {
    const update = new Subject<never>();
    const updateItemQuantity = vi.fn(() => update.asObservable());

    await TestBed.configureTestingModule({
      imports: [ClientCart],
      providers: [
        provideRouter([]),
        {
          provide: CartService,
          useValue: {
            cart: signal({
              id: 'cart-1',
              clienteId: 'client-1',
              items: [{
                productoId: 'product-1',
                nombre: 'Auriculares',
                precioUnitario: 12.5,
                cantidad: 2,
                subtotal: 25,
              }],
              precioTotal: 25,
            }),
            loading: signal(false),
            loadError: signal(null),
            itemCount: () => 2,
            updateItemQuantity,
            removeItem: vi.fn(() => update.asObservable()),
          },
        },
        { provide: PedidoService, useValue: { obtenerCheckoutResumen: vi.fn(), crearPedido: vi.fn() } },
      ],
    }).compileComponents();

    const fixture = TestBed.createComponent(ClientCart);
    fixture.detectChanges();
    const buttons = fixture.nativeElement.querySelectorAll('.quantity-action') as NodeListOf<HTMLButtonElement>;

    buttons[1].click();
    fixture.detectChanges();
    buttons[1].click();

    expect(updateItemQuantity).toHaveBeenCalledTimes(1);
    expect(updateItemQuantity).toHaveBeenCalledWith('product-1', 3);
    expect(buttons[0].disabled).toBe(true);
    expect(buttons[1].disabled).toBe(true);

    update.complete();
    fixture.detectChanges();
    expect(buttons[0].disabled).toBe(false);
  });

  it('should call the remove endpoint action from the delete control', async () => {
    const removeItem = vi.fn(() => new Subject<never>().asObservable());

    await TestBed.configureTestingModule({
      imports: [ClientCart],
      providers: [
        provideRouter([]),
        {
          provide: CartService,
          useValue: {
            cart: signal({
              id: 'cart-1',
              clienteId: 'client-1',
              items: [{
                productoId: 'product-1',
                nombre: 'Auriculares',
                precioUnitario: 12.5,
                cantidad: 2,
                subtotal: 25,
              }],
              precioTotal: 25,
            }),
            loading: signal(false),
            loadError: signal(null),
            itemCount: () => 2,
            updateItemQuantity: vi.fn(),
            removeItem,
          },
        },
        { provide: PedidoService, useValue: { obtenerCheckoutResumen: vi.fn(), crearPedido: vi.fn() } },
      ],
    }).compileComponents();

    const fixture = TestBed.createComponent(ClientCart);
    fixture.detectChanges();

    const removeButton = fixture.nativeElement.querySelector('.remove-action') as HTMLButtonElement;
    removeButton.click();

    expect(removeItem).toHaveBeenCalledWith('product-1');
  });

  it('should show the generated order identifier and clear cart state on success', async () => {
    const clear = vi.fn();
    const summary = {
      direcciones: [{
        id: 'address-1',
        recipientName: 'Ana López',
        street: 'Calle Mayor 1',
        additionalInfo: null,
        postalCode: '13001',
        city: 'Ciudad Real',
        province: 'Ciudad Real',
        phone: '600123456',
        isDefault: true,
      }],
      metodosPago: [{
        id: 'payment-1',
        type: 'CARD',
        cardBrand: 'VISA',
        lastFourDigits: '1234',
        defaultMethod: true,
      }],
      items: [{
        productoId: 'product-1',
        nombre: 'Auriculares',
        precioUnitario: 19.99,
        cantidad: 2,
        subtotal: 39.98,
      }],
      precioTotal: 39.98,
    };
    const obtenerCheckoutResumen = vi.fn(() => of(summary));
    const crearPedido = vi.fn(() => of({
      pedidoId: 'pedido-123',
      precioTotal: 39.98,
      estado: 'PAGADO' as const,
      fechaCreacion: '2026-10-17T12:00:00',
    }));

    await TestBed.configureTestingModule({
      imports: [ClientCart],
      providers: [
        provideRouter([]),
        {
          provide: CartService,
          useValue: {
            cart: signal({
              id: 'cart-1',
              clienteId: 'client-1',
              items: summary.items,
              precioTotal: summary.precioTotal,
            }),
            loading: signal(false),
            loadError: signal(null),
            itemCount: () => 2,
            clear,
          },
        },
        { provide: PedidoService, useValue: { obtenerCheckoutResumen, crearPedido } },
      ],
    }).compileComponents();

    const fixture = TestBed.createComponent(ClientCart);
    fixture.detectChanges();
    (fixture.nativeElement.querySelector('.checkout-action') as HTMLButtonElement).click();
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();

    expect(obtenerCheckoutResumen).toHaveBeenCalledOnce();
    expect((fixture.nativeElement.querySelector('#shipping-address') as HTMLSelectElement).value)
      .toBe('address-1');
    (fixture.nativeElement.querySelector('.checkout-actions .primary-action') as HTMLButtonElement).click();
    fixture.detectChanges();

    expect(crearPedido).toHaveBeenCalledWith('address-1', 'payment-1');
    expect(clear).toHaveBeenCalledOnce();
    expect(fixture.nativeElement.querySelector('.order-number')?.textContent).toContain('pedido-123');
    expect(fixture.nativeElement.querySelector('.checkout-panel')).toBeNull();
  });

  it('should keep checkout visible and show safe backend errors when order creation fails', async () => {
    const summary = {
      direcciones: [{
        id: 'address-1',
        recipientName: 'Ana',
        street: 'Calle Mayor 1',
        additionalInfo: null,
        postalCode: '13001',
        city: 'Ciudad Real',
        province: 'Ciudad Real',
        phone: '',
        isDefault: true,
      }],
      metodosPago: [{
        id: 'payment-1',
        type: 'CARD',
        cardBrand: 'VISA',
        lastFourDigits: '1234',
        defaultMethod: true,
      }],
      items: [{
        productoId: 'product-1',
        nombre: 'Auriculares',
        precioUnitario: 19.99,
        cantidad: 2,
        subtotal: 39.98,
      }],
      precioTotal: 39.98,
    };
    const crearPedido = vi.fn(() => throwError(() => new HttpErrorResponse({
      status: 402,
      error: { message: 'El pago fue rechazado. No se ha realizado ningún cargo ni modificado tu carrito' },
    })));

    await TestBed.configureTestingModule({
      imports: [ClientCart],
      providers: [
        provideRouter([]),
        {
          provide: CartService,
          useValue: {
            cart: signal({
              id: 'cart-1',
              clienteId: 'client-1',
              items: summary.items,
              precioTotal: summary.precioTotal,
            }),
            loading: signal(false),
            loadError: signal(null),
            itemCount: () => 2,
            clear: vi.fn(),
          },
        },
        {
          provide: PedidoService, useValue: {
            obtenerCheckoutResumen: vi.fn(() => of(summary)),
            crearPedido,
          }
        },
      ],
    }).compileComponents();

    const fixture = TestBed.createComponent(ClientCart);
    fixture.detectChanges();
    (fixture.nativeElement.querySelector('.checkout-action') as HTMLButtonElement).click();
    fixture.detectChanges();
    (fixture.nativeElement.querySelector('.checkout-actions .primary-action') as HTMLButtonElement).click();
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('[role="alert"]')?.textContent)
      .toContain('El pago fue rechazado');
    expect(fixture.nativeElement.querySelector('.checkout-panel')).not.toBeNull();
    expect(fixture.nativeElement.querySelector('.order-confirmation')).toBeNull();
  });

  it('should render product image when imageUrl is provided, and fallback when missing', async () => {
    const cart = signal({
      id: 'cart-1',
      clienteId: 'client-1',
      items: [
        {
          productoId: 'p-1',
          nombre: 'Auriculares Pro',
          precioUnitario: 50,
          cantidad: 1,
          subtotal: 50,
          imageUrl: 'https://example.com/auriculares.jpg',
        },
        {
          productoId: 'p-2',
          nombre: 'Cable USB',
          precioUnitario: 10,
          cantidad: 1,
          subtotal: 10,
        },
      ],
      precioTotal: 60,
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
        { provide: PedidoService, useValue: { obtenerCheckoutResumen: vi.fn(), crearPedido: vi.fn() } },
      ],
    }).compileComponents();

    const fixture = TestBed.createComponent(ClientCart);
    fixture.detectChanges();

    const element = fixture.nativeElement as HTMLElement;
    const items = element.querySelectorAll('.cart-item');
    expect(items.length).toBe(2);

    const img = items[0].querySelector('.product-thumb') as HTMLImageElement;
    expect(img).toBeTruthy();
    expect(img.getAttribute('src')).toBe('https://example.com/auriculares.jpg');

    const fallback = items[1].querySelector('.thumb-fallback');
    expect(fallback).toBeTruthy();
  });

  it('should group cart items by vendor and show single vendor banner when all items belong to the same vendor', async () => {
    const cart = signal({
      id: 'cart-1',
      clienteId: 'client-1',
      items: [
        {
          productoId: 'p-1',
          nombre: 'Whopper',
          precioUnitario: 8.5,
          cantidad: 2,
          subtotal: 17,
          vendedorId: 'seller-bk',
          vendedorNombre: 'Burger King',
        },
        {
          productoId: 'p-2',
          nombre: 'Patatas',
          precioUnitario: 3.5,
          cantidad: 1,
          subtotal: 3.5,
          vendedorId: 'seller-bk',
          vendedorNombre: 'Burger King',
        },
      ],
      precioTotal: 20.5,
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
            itemCount: () => 3,
          },
        },
        { provide: PedidoService, useValue: { obtenerCheckoutResumen: vi.fn(), crearPedido: vi.fn() } },
      ],
    }).compileComponents();

    const fixture = TestBed.createComponent(ClientCart);
    fixture.detectChanges();

    const element = fixture.nativeElement as HTMLElement;
    expect(element.querySelector('.seller-single-banner')?.textContent).toContain('Burger King');
    expect(element.querySelector('.seller-summary-name')?.textContent).toContain('Burger King');
    expect(element.querySelector('.seller-warning-alert')).toBeNull();

    const checkoutBtn = element.querySelector('.checkout-action') as HTMLButtonElement;
    expect(checkoutBtn.disabled).toBe(false);
  });

  it('should display warning alert and disable checkout when cart contains items from multiple vendors', async () => {
    const cart = signal({
      id: 'cart-1',
      clienteId: 'client-1',
      items: [
        {
          productoId: 'p-1',
          nombre: 'Whopper',
          precioUnitario: 8.5,
          cantidad: 1,
          subtotal: 8.5,
          vendedorId: 'seller-bk',
          vendedorNombre: 'Burger King',
        },
        {
          productoId: 'p-2',
          nombre: 'Big Mac',
          precioUnitario: 8.0,
          cantidad: 1,
          subtotal: 8.0,
          vendedorId: 'seller-mcd',
          vendedorNombre: 'McDonalds',
        },
      ],
      precioTotal: 16.5,
    });

    const obtenerCheckoutResumen = vi.fn();

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
        { provide: PedidoService, useValue: { obtenerCheckoutResumen, crearPedido: vi.fn() } },
      ],
    }).compileComponents();

    const fixture = TestBed.createComponent(ClientCart);
    fixture.detectChanges();

    const element = fixture.nativeElement as HTMLElement;
    const warningAlert = element.querySelector('.seller-warning-alert');
    expect(warningAlert).not.toBeNull();
    expect(warningAlert?.textContent).toContain('Burger King, McDonalds');
    expect(warningAlert?.textContent).toContain('No se puede tramitar un pedido con productos de diferentes vendedores');

    const checkoutBtn = element.querySelector('.checkout-action') as HTMLButtonElement;
    expect(checkoutBtn.disabled).toBe(true);

    const disabledNotice = element.querySelector('.checkout-disabled-notice');
    expect(disabledNotice).not.toBeNull();
    expect(disabledNotice?.textContent).toContain('debes realizar pedidos separados');

    fixture.componentInstance.iniciarCheckout();
    expect(obtenerCheckoutResumen).not.toHaveBeenCalled();
    expect(fixture.componentInstance.actionError()).toContain('No se puede realizar un pedido con productos de diferentes vendedores');
  });
});
