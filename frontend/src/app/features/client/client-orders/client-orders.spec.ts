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
      estadoPago: 'PAGADO' as const,
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
      estadoPago: 'PAGADO' as const,
      direccionEnvio: null,
      metodoPago: null,
    }]);
    fixture.detectChanges();

    const detailsLink = fixture.nativeElement.querySelector('.order-detail-link') as HTMLAnchorElement;
    expect(detailsLink.textContent).toContain('Ver detalle');
    expect(detailsLink.getAttribute('href')).toBe('/client/orders/pedido-123');
    expect(fixture.nativeElement.querySelector('.reorder-button')).toBeNull();
    expect(fixture.nativeElement.textContent).not.toContain('Comprar de nuevo');
  });

  it('should only show delivered orders in the Pedidos tab', () => {
    const makeOrder = (
      pedidoId: string,
      estado: 'PAGADO' | 'PENDIENTE' | 'PREPARANDO' | 'ENVIADO' | 'ENTREGADO' | 'CANCELADO',
    ) => ({
      pedidoId,
      fechaCreacion: '2026-10-28T12:00:00',
      items: [],
      total: 0,
      estado,
      estadoPago: 'PAGADO' as const,
      direccionEnvio: null,
      metodoPago: null,
    });
    component.orders.set([
      makeOrder('pedido-entregado', 'ENTREGADO'),
      makeOrder('pedido-pagado', 'PAGADO'),
      makeOrder('pedido-pendiente', 'PENDIENTE'),
      makeOrder('pedido-enviado', 'ENVIADO'),
      makeOrder('pedido-preparando', 'PREPARANDO'),
      makeOrder('pedido-cancelado', 'CANCELADO'),
    ]);

    expect(component.filteredOrders().map((order) => order.pedidoId)).toEqual(['pedido-entregado']);

    component.selectTab('pending');

    expect(component.filteredOrders().map((order) => order.pedidoId)).toEqual([
      'pedido-pagado',
      'pedido-pendiente',
      'pedido-enviado',
      'pedido-preparando',
      'pedido-cancelado',
    ]);
  });

  it('should show independent order and payment statuses and real summary counts', () => {
    component.orders.set([
      {
        pedidoId: 'pending',
        fechaCreacion: '2026-10-28T12:00:00',
        items: [],
        total: 12,
        estado: 'PENDIENTE',
        estadoPago: 'PAGADO',
        direccionEnvio: null,
        metodoPago: null,
      },
      {
        pedidoId: 'delivered',
        fechaCreacion: '2026-10-28T12:00:00',
        items: [],
        total: 24,
        estado: 'ENTREGADO',
        estadoPago: 'PAGADO',
        direccionEnvio: null,
        metodoPago: null,
      },
      {
        pedidoId: 'unpaid',
        fechaCreacion: '2026-10-28T12:00:00',
        items: [],
        total: 6,
        estado: 'PENDIENTE',
        estadoPago: 'PENDIENTE',
        direccionEnvio: null,
        metodoPago: null,
      },
    ]);

    expect(component.orderCounts()).toEqual({
      total: 3,
      pending: 2,
      preparing: 0,
      shipped: 0,
      delivered: 1,
    });
    expect(component.getPaymentLabel(component.orders()[0])).toBe('Pagado');
    expect(component.getStatusLabel(component.orders()[0])).toBe('Pendiente');

    component.selectTab('pending');
    component.setPaymentFilter('PENDIENTE');
    expect(component.filteredOrders().map((order) => order.pedidoId)).toEqual(['unpaid']);
  });

  it('should search, filter and sort orders and reset pagination when filters change', () => {
    component.orders.set([
      {
        pedidoId: 'old-order',
        fechaCreacion: '2026-01-01T10:00:00',
        items: [{ productoId: 'item-1', nombre: 'Lámpara', cantidad: 1, precioUnitario: 10, subtotal: 10, imageUrl: null }],
        total: 10,
        estado: 'ENTREGADO',
        estadoPago: 'PAGADO',
        direccionEnvio: null,
        metodoPago: null,
      },
      {
        pedidoId: 'new-order',
        fechaCreacion: '2026-02-01T10:00:00',
        items: [{ productoId: 'item-2', nombre: 'Mesa', cantidad: 2, precioUnitario: 20, subtotal: 40, imageUrl: null }],
        total: 40,
        estado: 'ENTREGADO',
        estadoPago: 'PAGADO',
        direccionEnvio: null,
        metodoPago: null,
      },
    ]);

    expect(component.filteredOrders().map((order) => order.pedidoId)).toEqual(['new-order', 'old-order']);
    component.setSortOrder('amount-asc');
    expect(component.filteredOrders().map((order) => order.pedidoId)).toEqual(['old-order', 'new-order']);
    component.setSearchQuery('mesa');
    expect(component.filteredOrders().map((order) => order.pedidoId)).toEqual(['new-order']);
    expect(component.getItemCount(component.filteredOrders()[0])).toBe(2);
    component.setStatusFilter('ENVIADO');
    expect(component.filteredOrders()).toEqual([]);
    component.clearFilters();
    expect(component.searchQuery()).toBe('');
    expect(component.statusFilter()).toBe('TODOS');
    expect(component.sortOrder()).toBe('date-desc');
  });

  it('should paginate filtered orders by ten', () => {
    component.orders.set(Array.from({ length: 11 }, (_, index) => ({
      pedidoId: `pedido-${index}`,
      fechaCreacion: `2026-10-${String(index + 1).padStart(2, '0')}T12:00:00`,
      items: [],
      total: index,
      estado: 'ENTREGADO' as const,
      estadoPago: 'PAGADO' as const,
      direccionEnvio: null,
      metodoPago: null,
    })));

    expect(component.visibleOrders()).toHaveLength(10);
    expect(component.pageCount()).toBe(2);
    component.nextPage();
    expect(component.pageIndex()).toBe(1);
    expect(component.visibleOrders()).toHaveLength(1);
    expect(component.pageLabel()).toBe('11–11 de 11');
    component.previousPage();
    expect(component.pageIndex()).toBe(0);
  });
});
