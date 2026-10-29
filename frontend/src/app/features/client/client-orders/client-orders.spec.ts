import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { describe, beforeEach, it, expect } from 'vitest';
import { ClientOrders } from './client-orders';
import { CartService } from '../../../core/services/cart.service';
import { PedidoService } from '../../../core/services/pedido.service';
import { of } from 'rxjs';

describe('ClientOrders', () => {
  let component: ClientOrders;
  let fixture: ComponentFixture<ClientOrders>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ClientOrders],
      providers: [
        provideRouter([]),
        { provide: CartService, useValue: { itemCount: () => 0 } },
        {
          provide: PedidoService,
          useValue: {
            obtenerPedidosDelCliente: () => of([]),
            obtenerHistorialEstadosEnvio: () => of({ historialEstados: [], estimatedDeliveryDate: null }),
          },
        },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(ClientOrders);
    component = fixture.componentInstance;
    fixture.detectChanges();
    await fixture.whenStable();
  });

  it('should create and render navigation order in sidebar with Mis compras active', () => {
    expect(component).toBeTruthy();
    const el: HTMLElement = fixture.nativeElement;

    expect(el.querySelector('#orders-title')?.textContent).toContain('Mis pedidos');
    expect(el.querySelector('#orders-search-input')).toBeTruthy();
    expect(el.querySelector('.orders-tabs')?.textContent).toContain('Pendiente de envío');

    const navLinks = Array.from(el.querySelectorAll('.navigation-link'));
    expect(navLinks.length).toBe(5);
    expect(navLinks[0].textContent).toContain('Catálogo');
    expect(navLinks[1].textContent).toContain('Lista de deseos');
    expect(navLinks[2].textContent).toContain('Mis compras');
    expect(navLinks[2].classList.contains('is-active')).toBe(true);
    expect(navLinks[3].textContent).toContain('Carrito');
    expect(navLinks[4].textContent).toContain('Perfil');
  });

  it('should toggle sidebar collapsed state', () => {
    expect(component.sidebarCollapsed()).toBe(false);
    component.toggleSidebar();
    expect(component.sidebarCollapsed()).toBe(true);
    component.toggleSidebar();
    expect(component.sidebarCollapsed()).toBe(false);
  });

  it('should show shipment progress only in the pending shipment tab', async () => {
    component.orders.set([{
      pedidoId: 'pedido-123',
      fechaCreacion: '2026-10-28T12:00:00',
      items: [{
        productoId: 'product-1',
        nombre: 'Auriculares',
        cantidad: 1,
        precioUnitario: 19.99,
        subtotal: 19.99,
        imageUrl: null,
      }],
      total: 19.99,
      estado: 'ENVIADO',
      direccionEnvio: null,
      metodoPago: null,
    }]);
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('app-client-shipment-tracking')).toBeNull();
    fixture.nativeElement.querySelector('.orders-tabs button:nth-child(2)').click();
    fixture.detectChanges();
    await fixture.whenStable();

    expect(fixture.nativeElement.querySelector('app-client-shipment-tracking')).toBeTruthy();
  });

  it('should provide a clear details link without a reorder button on the order list', () => {
    component.orders.set([{
      pedidoId: 'pedido-123',
      fechaCreacion: '2026-10-28T12:00:00',
      items: [],
      total: 0,
      estado: 'ENTREGADO',
      direccionEnvio: null,
      metodoPago: null,
    }]);
    fixture.detectChanges();

    const detailsLink = fixture.nativeElement.querySelector('.order-detail-link') as HTMLAnchorElement;
    expect(detailsLink.textContent).toContain('Ver detalles');
    expect(detailsLink.getAttribute('href')).toBe('/client/orders/pedido-123');
    expect(fixture.nativeElement.querySelector('.reorder-button')).toBeNull();
    expect(fixture.nativeElement.textContent).not.toContain('Comprar de nuevo');
  });

  it('should only show delivered orders in the Pedidos tab', () => {
    const makeOrder = (pedidoId: string, estado: 'ENTREGADO' | 'ENVIADO' | 'PREPARANDO') => ({
      pedidoId,
      fechaCreacion: '2026-10-28T12:00:00',
      items: [],
      total: 0,
      estado,
      direccionEnvio: null,
      metodoPago: null,
    });
    component.orders.set([
      makeOrder('pedido-entregado', 'ENTREGADO'),
      makeOrder('pedido-enviado', 'ENVIADO'),
      makeOrder('pedido-preparando', 'PREPARANDO'),
    ]);

    expect(component.filteredOrders().map((order) => order.pedidoId)).toEqual(['pedido-entregado']);
  });
});
