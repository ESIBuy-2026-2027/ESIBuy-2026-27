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
            obtenerHistorialEstadosEnvio: () =>
              of({ historialEstados: [], estimatedDeliveryDate: null }),
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
    expect(el.querySelectorAll('.orders-tab')).toHaveLength(6);
    expect(el.querySelector('.orders-tab.is-selected')?.textContent).toContain('Todos');
    expect(el.querySelector('.filter-button')?.textContent).toContain('Filtrar');
    expect(el.querySelector('#orders-period')).toBeTruthy();
    expect(el.querySelector('#orders-payment')).toBeNull();

    (el.querySelector('.filter-button') as HTMLButtonElement).click();
    fixture.detectChanges();
    expect(el.querySelector('#orders-payment')).toBeTruthy();
    expect(el.querySelector('#orders-sort')).toBeTruthy();

    const navLinks = Array.from(el.querySelectorAll('.navigation-link'));
    expect(navLinks.length).toBe(4);
    expect(navLinks[0].textContent).toContain('Catálogo');
    expect(navLinks[1].textContent).toContain('Lista de deseos');
    expect(navLinks[2].textContent).toContain('Mis pedidos');
    expect(navLinks[2].classList.contains('is-active')).toBe(true);
    expect(navLinks[2].querySelector('svg path')?.getAttribute('d')).toContain('M21 8');
    expect(navLinks[3].textContent).toContain('Carrito');
  });

  it('should filter by status tabs and show advanced filters only when requested', async () => {
    component.orders.set([
      {
        pedidoId: 'pending',
        fechaCreacion: '2026-10-28T12:00:00',
        items: [],
        total: 0,
        estado: 'PENDIENTE',
        estadoPago: 'PAGADO',
        direccionEnvio: null,
        metodoPago: null,
      },
      {
        pedidoId: 'delivered',
        fechaCreacion: '2026-10-28T12:00:00',
        items: [],
        total: 0,
        estado: 'ENTREGADO',
        estadoPago: 'PAGADO',
        direccionEnvio: null,
        metodoPago: null,
      },
    ]);
    fixture.detectChanges();

    const element: HTMLElement = fixture.nativeElement;
    expect(element.querySelector('#orders-payment')).toBeNull();
    const pendingTab = Array.from(element.querySelectorAll('.orders-tab')).find((tab) =>
      tab.textContent?.includes('Pendientes'),
    ) as HTMLButtonElement;
    pendingTab.click();
    fixture.detectChanges();

    expect(component.statusFilter()).toBe('PENDIENTE');
    expect(element.querySelectorAll('.order-row')).toHaveLength(1);
    expect(element.textContent).toContain('#pending');
    expect(element.textContent).not.toContain('#delivered');

    (element.querySelector('.filter-button') as HTMLButtonElement).click();
    fixture.detectChanges();
    expect(element.querySelector('#orders-payment')).toBeTruthy();

    (element.querySelector('.close-orders-filter') as HTMLButtonElement).click();
    fixture.detectChanges();
    expect(element.querySelector('#orders-payment')).toBeNull();
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
      {
        pedidoId: 'delivered',
        fechaCreacion: '2026-10-28T12:00:00',
        items: [],
        total: 0,
        estado: 'ENTREGADO',
        estadoPago: 'PAGADO',
        direccionEnvio: null,
        metodoPago: null,
      },
      {
        pedidoId: 'pending',
        fechaCreacion: '2026-10-28T12:00:00',
        items: [],
        total: 0,
        estado: 'PENDIENTE',
        estadoPago: 'PAGADO',
        direccionEnvio: null,
        metodoPago: null,
      },
      {
        pedidoId: 'preparing',
        fechaCreacion: '2026-10-28T12:00:00',
        items: [],
        total: 0,
        estado: 'PREPARANDO',
        estadoPago: 'PAGADO',
        direccionEnvio: null,
        metodoPago: null,
      },
      {
        pedidoId: 'shipped',
        fechaCreacion: '2026-10-28T12:00:00',
        items: [],
        total: 0,
        estado: 'ENVIADO',
        estadoPago: 'PAGADO',
        direccionEnvio: null,
        metodoPago: null,
      },
      {
        pedidoId: 'cancelled',
        fechaCreacion: '2026-10-28T12:00:00',
        items: [],
        total: 0,
        estado: 'CANCELADO',
        estadoPago: 'PAGADO',
        direccionEnvio: null,
        metodoPago: null,
      },
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

  it('should show the filtered count beside the visible period selector', () => {
    component.orders.set([
      {
        pedidoId: 'order-1',
        fechaCreacion: '2026-10-28T12:00:00',
        items: [],
        total: 0,
        estado: 'PENDIENTE',
        estadoPago: 'PAGADO',
        direccionEnvio: null,
        metodoPago: null,
      },
      {
        pedidoId: 'order-2',
        fechaCreacion: '2026-10-27T12:00:00',
        items: [],
        total: 0,
        estado: 'ENTREGADO',
        estadoPago: 'PAGADO',
        direccionEnvio: null,
        metodoPago: null,
      },
    ]);
    fixture.detectChanges();

    const element: HTMLElement = fixture.nativeElement;
    const resultsMeta = element.querySelector('.orders-results-meta') as HTMLElement;

    expect(resultsMeta.textContent).toContain('1–2 de 2');
    expect(element.querySelector('.orders-period-row')?.textContent).toContain(
      '2 pedidos encontrados',
    );
    expect(element.querySelector('#orders-period')).toBeTruthy();
  });

  it('should provide a clear details link without a reorder button on the order list', () => {
    component.orders.set([
      {
        pedidoId: 'pedido-123',
        fechaCreacion: '2026-10-28T12:00:00',
        items: [],
        total: 0,
        estado: 'ENTREGADO',
        estadoPago: 'PAGADO' as const,
        direccionEnvio: null,
        metodoPago: null,
      },
    ]);
    fixture.detectChanges();

    const detailsLink = fixture.nativeElement.querySelector(
      '.order-detail-link',
    ) as HTMLAnchorElement;
    expect(detailsLink.textContent).toContain('Ver los detalles del pedido');
    expect(detailsLink.getAttribute('href')).toBe('/client/orders/pedido-123');
    expect(fixture.nativeElement.querySelector('.reorder-button')).toBeNull();
    expect(fixture.nativeElement.textContent).not.toContain('Comprar de nuevo');
  });

  it('should show order metadata and emphasize fulfillment status above the item rows', () => {
    component.orders.set([
      {
        pedidoId: 'pedido-amazon',
        fechaCreacion: '2026-10-28T12:00:00',
        items: [
          {
            productoId: 'producto-1',
            nombre: 'Auriculares inalámbricos',
            cantidad: 2,
            precioUnitario: 19.99,
            subtotal: 39.98,
            imageUrl: null,
          },
        ],
        total: 45,
        estado: 'ENVIADO',
        estadoPago: 'PENDIENTE',
        direccionEnvio: {
          recipientName: 'Tomás García',
          street: 'Calle Mayor 1',
          additionalInfo: null,
          postalCode: '28001',
          city: 'Madrid',
          province: 'Madrid',
          phone: null,
        },
        metodoPago: null,
      },
    ]);
    fixture.detectChanges();

    const element: HTMLElement = fixture.nativeElement;
    const orderCard = element.querySelector('.order-card') as HTMLElement;

    expect(orderCard.querySelector('.order-card-header')?.textContent).toContain(
      'Pedido realizado',
    );
    expect(orderCard.querySelector('.order-card-header')?.textContent).toContain('Tomás García');
    expect(orderCard.querySelector('.order-card-header')?.textContent).toContain('#pedido-amazon');
    expect(orderCard.querySelector('.order-status')?.textContent).toContain('Enviado');
    expect(orderCard.querySelector('.payment-badge')?.textContent).toContain('Pendiente');
    expect(orderCard.querySelector('.order-item-info h3')?.textContent).toContain(
      'Auriculares inalámbricos',
    );
    expect(orderCard.querySelector('.order-item-info p')?.textContent).toContain('2 ×');
  });

  it('should show every order item with its image or an accessible placeholder', () => {
    component.orders.set([
      {
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
      },
    ]);
    fixture.detectChanges();

    const element: HTMLElement = fixture.nativeElement;
    const itemImages = element.querySelectorAll('img.order-item-image');
    expect(itemImages).toHaveLength(1);
    expect(itemImages[0].getAttribute('src')).toBe('https://example.test/auriculares.jpg');
    expect(itemImages[0].getAttribute('alt')).toBe('Auriculares');
    expect(element.querySelector('.order-item-placeholder')?.textContent?.trim()).toBe('F');
    expect(element.querySelectorAll('.order-item')).toHaveLength(2);
    expect(element.textContent).toContain('Auriculares');
    expect(element.textContent).toContain('Funda');
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

  it('should show independent order and payment statuses', () => {
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
        items: [
          {
            productoId: 'item-1',
            nombre: 'Lámpara',
            cantidad: 1,
            precioUnitario: 10,
            subtotal: 10,
            imageUrl: null,
          },
        ],
        total: 10,
        estado: 'ENTREGADO',
        estadoPago: 'PAGADO',
        direccionEnvio: null,
        metodoPago: null,
      },
      {
        pedidoId: 'new-order',
        fechaCreacion: '2026-02-01T10:00:00',
        items: [
          {
            productoId: 'item-2',
            nombre: 'Mesa',
            cantidad: 2,
            precioUnitario: 20,
            subtotal: 40,
            imageUrl: null,
          },
        ],
        total: 40,
        estado: 'ENTREGADO',
        estadoPago: 'PAGADO',
        direccionEnvio: null,
        metodoPago: null,
      },
    ]);

    expect(component.filteredOrders().map((order) => order.pedidoId)).toEqual([
      'new-order',
      'old-order',
    ]);
    component.setSortOrder('amount-asc');
    expect(component.filteredOrders().map((order) => order.pedidoId)).toEqual([
      'old-order',
      'new-order',
    ]);
    component.setSearchQuery('mesa');
    expect(component.filteredOrders().map((order) => order.pedidoId)).toEqual(['new-order']);
    component.setStatusFilter('ENVIADO');
    expect(component.filteredOrders()).toEqual([]);
    component.clearFilters();
    expect(component.searchQuery()).toBe('');
    expect(component.statusFilter()).toBe('TODOS');
    expect(component.sortOrder()).toBe('date-desc');
  });

  it('should paginate filtered orders by ten', () => {
    component.orders.set(
      Array.from({ length: 11 }, (_, index) => ({
        pedidoId: `pedido-${index}`,
        fechaCreacion: `2026-10-${String(index + 1).padStart(2, '0')}T12:00:00`,
        items: [],
        total: index,
        estado: 'ENTREGADO' as const,
        estadoPago: 'PAGADO' as const,
        direccionEnvio: null,
        metodoPago: null,
      })),
    );

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
