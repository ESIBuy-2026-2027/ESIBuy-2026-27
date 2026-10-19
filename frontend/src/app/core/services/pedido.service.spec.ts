import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { PedidoService } from './pedido.service';

describe('PedidoService', () => {
  let service: PedidoService;
  let httpTesting: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(PedidoService);
    httpTesting = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpTesting.verify());

  it('should request the signed-in customer checkout summary', () => {
    const summary = {
      direcciones: [],
      metodosPago: [],
      items: [],
      precioTotal: 0,
    };
    let response: unknown;

    service.obtenerCheckoutResumen().subscribe((result) => response = result);

    const request = httpTesting.expectOne('/api/pedidos/checkout-resumen');
    expect(request.request.method).toBe('GET');
    request.flush(summary);
    expect(response).toEqual(summary);
  });

  it('should submit only selected saved address and payment method identifiers', () => {
    let response: unknown;

    service.crearPedido('direccion-1', 'pago-1').subscribe((result) => response = result);

    const request = httpTesting.expectOne('/api/pedidos');
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual({
      direccionId: 'direccion-1',
      metodoPagoId: 'pago-1',
    });
    request.flush({
      pedidoId: 'pedido-123',
      precioTotal: 39.98,
      estado: 'PAGADO',
      fechaCreacion: '2026-10-17T12:00:00',
    });
    expect(response).toEqual(expect.objectContaining({ pedidoId: 'pedido-123' }));
  });
});
