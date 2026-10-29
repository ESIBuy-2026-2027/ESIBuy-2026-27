import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, provideRouter } from '@angular/router';
import { describe, beforeEach, it, expect } from 'vitest';
import { of } from 'rxjs';
import { CartService } from '../../../core/services/cart.service';
import { CustomerOrder, PedidoService } from '../../../core/services/pedido.service';
import { ClientOrderDetail } from './client-order-detail';

describe('ClientOrderDetail', () => {
  let fixture: ComponentFixture<ClientOrderDetail>;
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
            obtenerPedidoDelCliente: () => of(order),
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

  it('should show independent payment and order statuses plus actual state history', () => {
    const element: HTMLElement = fixture.nativeElement;

    expect(element.textContent).toContain('En preparación');
    expect(element.textContent).toContain('Estado del pago: Pagado');
    expect(element.textContent).toContain('Subtotal de productos');
    expect(element.textContent).toContain('25,00 €');
    expect(element.textContent).toContain('Descuentos, impuestos y envío: no desglosados');
    expect(element.textContent).toContain('No disponible');
    expect(element.querySelector('.status-history')).toBeTruthy();
    expect(element.querySelector('.progress-steps')).toBeNull();
    expect(element.querySelector('.buy-again-button')).toBeTruthy();
  });
});
