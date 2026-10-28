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

  it('should group cart items by vendor and calculate total based on selected vendor items', async () => {
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
    expect(element.querySelector('.seller-summary-name')?.textContent).toContain('Burger King');
    expect(element.querySelector('.seller-warning-alert')).toBeNull();
    expect(element.querySelector('.total-line')?.textContent).toContain('20,50');

    const checkoutBtn = element.querySelector('.checkout-action') as HTMLButtonElement;
    expect(checkoutBtn.disabled).toBe(false);
  });

  it('should only sum selected vendor items and prevent selecting products from another vendor', async () => {
    const items = [
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
    ];
    const cart = signal({
      id: 'cart-1',
      clienteId: 'client-1',
      items,
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
    // When multiple sellers are in the cart, client selects a vendor first
    const firstGroup = fixture.componentInstance.sellerGroups()[0];
    fixture.componentInstance.toggleSellerSelection(firstGroup);
    fixture.detectChanges();

    // Total only includes selected vendor (Burger King: 8,50 €), NOT the entire cart (16,50 €)
    expect(element.querySelector('.total-line')?.textContent).toContain('8,50');
    expect(element.querySelector('.total-line')?.textContent).not.toContain('16,50');

    // McDonalds seller is disabled because Burger King has active selection
    expect(fixture.componentInstance.isSellerDisabled('seller-mcd')).toBe(true);
    const mcdCheckboxes = element.querySelectorAll('.seller-accordion')[1].querySelectorAll('input[type="checkbox"]');
    expect((mcdCheckboxes[0] as HTMLInputElement).disabled).toBe(true);
    expect((mcdCheckboxes[1] as HTMLInputElement).disabled).toBe(true);

    // Attempting to select product from McDonalds while Burger King is selected
    const secondGroup = fixture.componentInstance.sellerGroups()[1];
    fixture.componentInstance.toggleProductSelection(items[1], secondGroup);
    fixture.detectChanges();

    expect(fixture.componentInstance.isProductSelected('p-2')).toBe(false);
    expect(fixture.componentInstance.actionError()).toContain('No puedes realizar pedidos de diferente vendedor a la vez');

    // If multiple sellers are selected simultaneously, total price is removed (displays —) and checkout is blocked
    fixture.componentInstance.setSelectedProductIds(['p-1', 'p-2']);
    fixture.detectChanges();

    expect(fixture.componentInstance.selectedTotal()).toBeNull();
    expect(element.querySelector('.no-price-indicator')?.textContent).toContain('—');

    const checkoutBtn = element.querySelector('.checkout-action') as HTMLButtonElement;
    expect(checkoutBtn.disabled).toBe(true);
  });
  it('should only show and order selected products in checkout when subset is chosen', async () => {
    const summary = {
      direcciones: [{
        id: 'address-1',
        recipientName: 'Pepe',
        street: 'Pepito, 12',
        additionalInfo: null,
        postalCode: '12222',
        city: 'PEPELAND',
        province: 'PEPELAND',
        phone: '',
        isDefault: true,
      }],
      metodosPago: [{
        id: 'payment-1',
        type: 'CARD',
        cardBrand: 'VISA',
        lastFourDigits: '1236',
        defaultMethod: true,
      }],
      items: [
        { productoId: 'p-1', nombre: 'Auriculares', precioUnitario: 25, cantidad: 2, subtotal: 50, vendedorId: 'v-1', vendedorNombre: 'cc' },
        { productoId: 'p-2', nombre: 'Batería Yamaha', precioUnitario: 300, cantidad: 1, subtotal: 300, vendedorId: 'v-1', vendedorNombre: 'cc' },
        { productoId: 'p-3', nombre: 'Casita de juguete', precioUnitario: 60, cantidad: 1, subtotal: 60, vendedorId: 'v-2', vendedorNombre: 'Messi' },
      ],
      precioTotal: 410,
    };

    const obtenerCheckoutResumen = vi.fn(() => of(summary));
    const crearPedido = vi.fn(() => of({
      pedidoId: 'pedido-auriculares',
      precioTotal: 50,
      estado: 'PAGADO' as const,
      fechaCreacion: '2026-10-27T18:00:00',
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
              precioTotal: 410,
            }),
            loading: signal(false),
            loadError: signal(null),
            itemCount: () => 4,
            clear: vi.fn(),
            load: vi.fn(),
          },
        },
        { provide: PedidoService, useValue: { obtenerCheckoutResumen, crearPedido } },
      ],
    }).compileComponents();

    const fixture = TestBed.createComponent(ClientCart);
    fixture.detectChanges();

    // Client explicitly selects ONLY Auriculares (p-1)
    fixture.componentInstance.setSelectedProductIds(['p-1']);
    fixture.detectChanges();

    expect(fixture.componentInstance.selectedItemCount()).toBe(2);
    expect(fixture.componentInstance.selectedTotal()).toBe(50);

    // Click checkout
    (fixture.nativeElement.querySelector('.checkout-action') as HTMLButtonElement).click();
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();

    const element = fixture.nativeElement as HTMLElement;
    // Checkout panel summary should only display Auriculares (not Batería or Casita)
    const rows = element.querySelectorAll('.checkout-items tbody tr');
    expect(rows.length).toBe(1);
    expect(rows[0].querySelector('th')?.textContent).toBe('Auriculares');
    expect(rows[0].querySelectorAll('td')[0].textContent).toContain('2');
    expect(rows[0].querySelectorAll('td')[1].textContent).toContain('50,00');

    expect(element.querySelector('.checkout-total strong')?.textContent).toContain('50,00');

    // Confirm order
    (element.querySelector('.checkout-actions .primary-action') as HTMLButtonElement).click();
    fixture.detectChanges();

    expect(crearPedido).toHaveBeenCalledWith('address-1', 'payment-1', 'v-1', ['p-1']);
  });
  it('should render discount pills, strikethrough price and total savings when discount is present', async () => {
    const cart = signal({
      id: 'cart-1',
      clienteId: 'client-1',
      items: [
        {
          productoId: 'p-1',
          nombre: 'Auriculares con Descuento',
          precioUnitario: 80,
          precioOriginal: 100,
          descuento: 20,
          esDescuentoPremium: false,
          cantidad: 1,
          subtotal: 80,
        },
      ],
      totalSinDescuento: 100,
      descuentoTotal: 20,
      precioTotal: 80,
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
            itemCount: () => 1,
          },
        },
        { provide: PedidoService, useValue: { obtenerCheckoutResumen: vi.fn(), crearPedido: vi.fn() } },
      ],
    }).compileComponents();

    const fixture = TestBed.createComponent(ClientCart);
    fixture.detectChanges();

    const element = fixture.nativeElement as HTMLElement;
    expect(element.querySelector('.item-original-price')?.textContent).toContain('100,00');
    expect(element.querySelector('.item-discount-tag')?.textContent).toContain('-20%');
    expect(element.querySelector('.discount-saving-line')?.textContent).toContain('20,00');
    expect(element.querySelector('.total-line')?.textContent).toContain('80,00');
  });
});