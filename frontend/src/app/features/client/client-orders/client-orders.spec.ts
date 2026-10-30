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

  it('should create and render navigation order in sidebar with Mis pedidos active', () => {
    expect(component).toBeTruthy();
    const el: HTMLElement = fixture.nativeElement;

    expect(el.querySelector('#orders-title')?.textContent).toContain('Mis pedidos');
    expect(el.querySelector('#orders-search-input')).toBeTruthy();
    expect(el.querySelector('.orders-tabs')).toBeNull();
    expect(el.querySelector('.filter-button')?.textContent).toContain('Filtrar');
    expect(el.querySelector('#orders-status')).toBeNull();

    (el.querySelector('.filter-button') as HTMLButtonElement).click();
    fixture.detectChanges();
    expect(el.querySelector('#orders-status')).toBeTruthy();

    const navLinks = Array.from(el.querySelectorAll('.navigation-link'));
    expect(navLinks.length).toBe(5);
    expect(navLinks[0].textContent).toContain('Catálogo');
    expect(navLinks[1].textContent).toContain('Lista de deseos');
    expect(navLinks[2].textContent).toContain('Mis pedidos');
    expect(navLinks[2].classList.contains('is-active')).toBe(true);
    expect(navLinks[2].querySelector('svg path')?.getAttribute('d')).toContain('M21 8');
    expect(navLinks[3].textContent).toContain('Carrito');
    expect(navLinks[4].textContent).toContain('Perfil');
  });

  it('should show filters only when requested and apply the selected order status', async () => {
    component.orders.set([
      { pedidoId: 'pending', fechaCreacion: '2026-10-28T12:00:00', items: [], total: 0, estado: 'PENDIENTE', estadoPago: 'PAGADO', direccionEnvio: null, metodoPago: null },
      { pedidoId: 'delivered', fechaCreacion: '2026-10-28T12:00:00', items: [], total: 0, estado: 'ENTREGADO', estadoPago: 'PAGADO', direccionEnvio: null, metodoPago: null },
    ]);
    fixture.detectChanges();

    const element: HTMLElement = fixture.nativeElement;
    expect(element.querySelector('#orders-status')).toBeNull();
    (element.querySelector('.filter-button') as HTMLButtonElement).click();
    fixture.detectChanges();

    const statusSelect = element.querySelector('#orders-status') as HTMLSelectElement;
    statusSelect.value = 'PENDIENTE';
    statusSelect.dispatchEvent(new Event('change'));
    fixture.detectChanges();
    await fixture.whenStable();

    expect(component.statusFilter()).toBe('PENDIENTE');
    expect(element.querySelectorAll('.order-row')).toHaveLength(1);
    expect(element.textContent).toContain('#pending');
    expect(element.textContent).not.toContain('#delivered');

    (element.querySelector('.close-orders-filter') as HTMLButtonElement).click();
    fixture.detectChanges();
    expect(element.querySelector('#orders-status')).toBeNull();
  });

  it('should toggle sidebar collapsed state', () => {
    expect(component.sidebarCollapsed()).toBe(false);
    component.toggleSidebar();
    expect(component.sidebarCollapsed()).toBe(true);
    component.toggleSidebar();
    expect(component.sidebarCollapsed()).toBe(false);
  });

  it('should show all orders together with only the current status in the list', () => {
    component.orders.set([
      { pedidoId: 'delivered', fechaCreacion: '2026-10-28T12:00:00', items: [], total: 0, estado: 'ENTREGADO', estadoPago: 'PAGADO', direccionEnvio: null, metodoPago: null },
      { pedidoId: 'pending', fechaCreacion: '2026-10-28T12:00:00', items: [], total: 0, estado: 'PENDIENTE', estadoPago: 'PAGADO', direccionEnvio: null, metodoPago: null },
      { pedidoId: 'preparing', fechaCreacion: '2026-10-28T12:00:00', items: [], total: 0, estado: 'PREPARANDO', estadoPago: 'PAGADO', direccionEnvio: null, metodoPago: null },
      { pedidoId: 'shipped', fechaCreacion: '2026-10-28T12:00:00', items: [], total: 0, estado: 'ENVIADO', estadoPago: 'PAGADO', direccionEnvio: null, metodoPago: null },
      { pedidoId: 'cancelled', fechaCreacion: '2026-10-28T12:00:00', items: [], total: 0, estado: 'CANCELADO', estadoPago: 'PAGADO', direccionEnvio: null, metodoPago: null },
    ]);
    fixture.detectChanges();

    const element: HTMLElement = fixture.nativeElement;
    expect(element.querySelectorAll('.order-row')).toHaveLength(5);
    expect(element.querySelector('app-client-shipment-tracking')).toBeNull();
    expect(element.textContent).toContain('#delivered');
    expect(element.textContent).toContain('#pending');
    expect(element.textContent).toContain('#cancelled');
    expect(element.querySelector('.status-pending')?.textContent).toContain('Pendiente');

    component.setStatusFilter('ENTREGADO');
    fixture.detectChanges();
    expect(element.querySelectorAll('.order-row')).toHaveLength(1);
    expect(element.textContent).toContain('#delivered');
    expect(element.textContent).not.toContain('#pending');
  });

  it('should place the total count beside the visible range below the search and filter toolbar', () => {
    component.orders.set([
      { pedidoId: 'order-1', fechaCreacion: '2026-10-28T12:00:00', items: [], total: 0, estado: 'PENDIENTE', estadoPago: 'PAGADO', direccionEnvio: null, metodoPago: null },
      { pedidoId: 'order-2', fechaCreacion: '2026-10-27T12:00:00', items: [], total: 0, estado: 'ENTREGADO', estadoPago: 'PAGADO', direccionEnvio: null, metodoPago: null },
    ]);
    fixture.detectChanges();

    const element: HTMLElement = fixture.nativeElement;
    const resultsMeta = element.querySelector('.orders-results-meta') as HTMLElement;

    expect(resultsMeta.textContent).toContain('1–2 de 2');
    expect(resultsMeta.querySelector('.order-count')?.textContent).toContain('2 pedidos');
    expect(element.querySelector('.orders-toolbar .order-count')).toBeNull();
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

  it('should preview order product images in the main list', () => {
    component.orders.set([{
      pedidoId: 'pedido-images',
      fechaCreacion: '2026-10-28T12:00:00',
      items: [
        {
          productoId: 'producto-1',
          nombre: 'Auriculares',
          cantidad: 1,
          precioUnitario: 19.99,
          subtotal: 19.99,
          imageUrl: 'https://example.test/auriculares.jpg',
        },
        {
          productoId: 'producto-2',
          nombre: 'Funda',
          cantidad: 1,
          precioUnitario: 9.99,
          subtotal: 9.99,
          imageUrl: null,
        },
      ],
      total: 29.98,
      estado: 'ENTREGADO',
      estadoPago: 'PAGADO',
      direccionEnvio: null,
      metodoPago: null,
    }]);
    fixture.detectChanges();

    const element: HTMLElement = fixture.nativeElement;
    const previewImages = element.querySelectorAll('.order-item-thumbnail img, img.order-item-thumbnail');
    expect(previewImages).toHaveLength(1);
    expect(previewImages[0].getAttribute('src')).toBe('https://example.test/auriculares.jpg');
    expect(previewImages[0].getAttribute('alt')).toBe('Auriculares');
    expect(element.querySelector('.order-item-placeholder')?.textContent?.trim()).toBe('F');
    expect(element.querySelector('.order-item-count')?.textContent).toContain('2 artículos');
  });

  it('should include every fulfillment state in the unified order list', () => {
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

    expect(component.filteredOrders().map((order) => order.pedidoId)).toEqual([
      'pedido-entregado',
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
      cancelled: 0,
    });
    expect(component.getPaymentLabel(component.orders()[0])).toBe('Pagado');
    expect(component.getStatusLabel(component.orders()[0])).toBe('Pendiente');

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
