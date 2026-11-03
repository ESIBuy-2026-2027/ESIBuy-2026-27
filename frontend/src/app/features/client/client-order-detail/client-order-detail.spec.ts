import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, provideRouter } from '@angular/router';
import { describe, beforeEach, it, expect, vi } from 'vitest';
import { of, throwError } from 'rxjs';
import { CartService } from '../../../core/services/cart.service';
import { CustomerOrder, PedidoService } from '../../../core/services/pedido.service';
import { ClientOrderDetail } from './client-order-detail';

describe('ClientOrderDetail', () => {
  let fixture: ComponentFixture<ClientOrderDetail>;
  let obtenerPedidoDelCliente: ReturnType<typeof vi.fn>;
  let confirmarRecepcionPedido: ReturnType<typeof vi.fn>;
  const order: CustomerOrder = {
    pedidoId: 'pedido-123',
    fechaCreacion: '2026-10-28T12:00:00',
    items: [{
      productoId: 'producto-1',
      nombre: 'Auriculares',
      cantidad: 2,
      precioUnitario: 12.5,
      subtotal: 25,
      imageUrl: null,
    }],
    total: 25,
    estado: 'PREPARANDO',
    estadoPago: 'PAGADO',
    direccionEnvio: null,
    metodoPago: null,
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ClientOrderDetail],
      providers: [
        provideRouter([]),
        {
          provide: ActivatedRoute,
          useValue: { snapshot: { paramMap: { get: () => 'pedido-123' } } },
        },
        {
          provide: PedidoService,
          useValue: {
            obtenerPedidoDelCliente: obtenerPedidoDelCliente = vi.fn(() => of(order)),
            confirmarRecepcionPedido: confirmarRecepcionPedido = vi.fn(() => of(undefined)),
            obtenerHistorialEstadosEnvio: () => of({
              historialEstados: [
                { estado: 'PENDIENTE', fecha: '2026-10-28T12:00:00' },
                { estado: 'PREPARANDO', fecha: '2026-10-28T13:00:00' },
              ],
              estimatedDeliveryDate: null,
            }),
          },
        },
        { provide: CartService, useValue: { addItem: () => of(undefined) } },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(ClientOrderDetail);
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();
  });

  it('should show independent payment and order statuses plus the progress bar in order details', () => {
    const element: HTMLElement = fixture.nativeElement;

    expect(element.textContent).toContain('En preparación');
    expect(element.textContent).toContain('Estado del pago: Pagado');
    expect(element.textContent).toContain('Subtotal de productos');
    expect(element.textContent).toContain('25,00 €');
    expect(element.textContent).toContain('Descuentos, impuestos y envío: no desglosados');
    expect(element.textContent).toContain('No disponible');
    expect(element.querySelector('.progress-steps')).toBeTruthy();
    expect(element.querySelector('.status-history')).toBeNull();
    expect(element.querySelector('.progress-step[aria-current="step"]')?.textContent).toContain('En preparación');
    expect(element.querySelector('.progress-step[aria-current="step"]')?.classList.contains('is-current'))
      .toBe(true);
    expect(element.querySelectorAll('.progress-marker svg')).toHaveLength(4);
    expect(element.querySelector('.progress-marker.is-current')).toBeTruthy();
    expect(Array.from(element.querySelectorAll('.progress-marker svg')).map((icon) => icon.getAttribute('viewBox')))
      .toEqual(['0 0 24 24', '0 0 24 24', '0 0 24 24', '0 0 24 24']);
    expect(element.querySelector('.buy-again-button')).toBeTruthy();
  });

  it('should retry loading the order after an error', () => {
    obtenerPedidoDelCliente
      .mockReturnValueOnce(throwError(() => new Error('network error')))
      .mockReturnValueOnce(of(order));

    fixture.componentInstance.loadOrder('pedido-123');
    expect(fixture.componentInstance.error()).toBe('No se pudo cargar el detalle del pedido.');

    fixture.componentInstance.retryLoadOrder();

    expect(fixture.componentInstance.error()).toBeNull();
    expect(fixture.componentInstance.order()).toEqual(order);
  });
});
