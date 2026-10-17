import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { Subject } from 'rxjs';
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
      ],
    }).compileComponents();

    const fixture = TestBed.createComponent(ClientCart);
    fixture.detectChanges();

    const removeButton = fixture.nativeElement.querySelector('.remove-action') as HTMLButtonElement;
    removeButton.click();

    expect(removeItem).toHaveBeenCalledWith('product-1');
  });
});
