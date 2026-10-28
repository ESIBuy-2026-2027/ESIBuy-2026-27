import { isPlatformBrowser } from '@angular/common';
import { Component, computed, inject, OnInit, PLATFORM_ID, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink, RouterLinkActive } from '@angular/router';
import {
  PedidoService,
  SellerOrder as ApiSellerOrder,
  SellerOrderStatus,
} from '../../../core/services/pedido.service';

type OrderStatus = 'Pendiente' | 'En preparación' | 'Enviado' | 'Entregado' | 'Cancelado';

interface SellerOrderItem {
  productId: string;
  name: string;
  quantity: number;
  price: number;
  imageUrl: string;
}

interface SellerOrder {
  id: string;
  date: string;
  dateValue: string;
  customer: string;
  products: string;
  total: number;
  status: OrderStatus;
  items: SellerOrderItem[];
  shippingAddress: string;
  customerEmail: string;
}



@Component({
  selector: 'app-seller-orders',
  standalone: true,
  imports: [FormsModule, RouterLink, RouterLinkActive],
  templateUrl: './seller-orders.html',
  styleUrl: './seller-orders.css',
})
export class SellerOrders implements OnInit {
  private readonly platformId = inject(PLATFORM_ID);
  private readonly pedidoService = inject(PedidoService);
  readonly orders = signal<SellerOrder[]>([]);
  readonly ordersLoading = signal(true);
  readonly ordersError = signal<string | null>(null);
  readonly orderSearch = signal('');
  readonly selectedStatus = signal<'Todos' | OrderStatus>('Todos');
  readonly dateFrom = signal('');
  readonly dateTo = signal('');
  readonly hasActiveFilters = computed(() =>
    this.selectedStatus() !== 'Todos'
    || !!this.dateFrom()
    || !!this.dateTo()
    || !!this.orderSearch().trim()
  );
  readonly expandedOrderId = signal<string | null>(null);
  readonly statusSavingOrderId = signal<string | null>(null);
  readonly statusUpdateError = signal<string | null>(null);
  readonly statusUpdateSuccess = signal<string | null>(null);
  readonly pendingStatusChange = signal<{ order: SellerOrder; status: OrderStatus } | null>(null);
  readonly selectedRollbackReason = signal('');
  readonly rollbackReasons = [
    'Corrección de información del pedido',
    'Incidencia durante la preparación',
    'Solicitud del comprador',
    'Otro motivo operativo',
  ];
  readonly sidebarCollapsed = signal(this.readSidebarPreference());
  readonly statusSummary = computed(() => ([
    { label: 'Pendientes', status: 'Pendiente' as const },
    { label: 'En preparación', status: 'En preparación' as const },
    { label: 'Enviados', status: 'Enviado' as const },
    { label: 'Entregados', status: 'Entregado' as const },
    { label: 'Cancelados', status: 'Cancelado' as const },
  ].map((summary) => ({
    ...summary,
    count: this.orders().filter((order) => order.status === summary.status).length,
  }))));

  readonly visibleOrders = computed(() => {
    const query = this.orderSearch().trim().toLocaleLowerCase();
    const status = this.selectedStatus();
    const from = this.dateFrom();
    const to = this.dateTo();

    return this.orders().filter((order) => {
      const matchesSearch = !query
        || order.id.toLocaleLowerCase().includes(query)
        || order.customer.toLocaleLowerCase().includes(query)
        || order.products.toLocaleLowerCase().includes(query);
      const matchesStatus = status === 'Todos' || order.status === status;
      const matchesFromDate = !from || order.dateValue >= from;
      const matchesToDate = !to || order.dateValue <= to;
      return matchesSearch && matchesStatus && matchesFromDate && matchesToDate;
    });
  });

  ngOnInit(): void {
    this.loadOrders();
  }

  loadOrders(): void {
    this.ordersLoading.set(true);
    this.ordersError.set(null);
    this.pedidoService.obtenerPedidosDelVendedor().subscribe({
      next: (orders) => {
        this.orders.set(orders.map((order) => this.toViewOrder(order)));
        this.ordersLoading.set(false);
      },
      error: () => {
        this.ordersLoading.set(false);
        this.ordersError.set('No se han podido cargar los pedidos. Inténtalo de nuevo.');
      },
    });
  }

  private toViewOrder(order: ApiSellerOrder): SellerOrder {
    const date = new Date(order.fechaCreacion);
    return {
        id: order.pedidoId,
        date: new Intl.DateTimeFormat('es-ES', { dateStyle: 'medium' }).format(date),
        dateValue: order.fechaCreacion.slice(0, 10),
        customer: order.clienteNombre,
        products: order.items.map((item) => `${item.nombre} · ${item.cantidad} ud.`).join(', '),
        total: order.total,
        status: this.toViewStatus(order.estado),
        items: order.items.map((item) => ({
          productId: item.productoId,
          name: item.nombre,
          quantity: item.cantidad,
          price: item.precioUnitario,
          imageUrl: item.imageUrl ?? '',
        })),
        shippingAddress: order.direccionEnvio,
        customerEmail: order.clienteEmail ?? '',
    };
  }

  private toViewStatus(status: SellerOrderStatus): OrderStatus {
    const statuses: Record<SellerOrderStatus, OrderStatus> = {
      PAGADO: 'Pendiente',
      PENDIENTE: 'Pendiente',
      PREPARANDO: 'En preparación',
      ENVIADO: 'Enviado',
      ENTREGADO: 'Entregado',
      CANCELADO: 'Cancelado',
    };
    return statuses[status];
  }

  formatPrice(total: number): string {
    return new Intl.NumberFormat('es-ES', {
      style: 'currency',
      currency: 'EUR',
    }).format(total);
  }

  statusClass(status: OrderStatus): string {
    return status
      .toLocaleLowerCase()
      .normalize('NFD')
      .replace(/[\u0300-\u036f]/g, '')
      .replaceAll(' ', '-');
  }

  orderActionLabel(status: OrderStatus): string {
    if (status === 'Pendiente') {
      return 'Preparar';
    }

    if (status === 'En preparación') {
      return 'Marcar enviado';
    }

    return 'Ver detalle';
  }

  clearFilters(): void {
    this.orderSearch.set('');
    this.selectedStatus.set('Todos');
    this.dateFrom.set('');
    this.dateTo.set('');
  }

  selectStatus(status: OrderStatus): void {
    this.selectedStatus.set(this.selectedStatus() === status ? 'Todos' : status);
  }

  toggleOrderDetails(orderId: string): void {
    this.statusUpdateError.set(null);
    this.statusUpdateSuccess.set(null);
    this.pendingStatusChange.set(null);
    this.selectedRollbackReason.set('');
    this.expandedOrderId.set(this.expandedOrderId() === orderId ? null : orderId);
  }

  updateOrderStatus(order: SellerOrder, status: OrderStatus): void {
    if (order.status === status || this.statusSavingOrderId()) {
      return;
    }

    if (this.isRollback(order.status, status)) {
      this.pendingStatusChange.set({ order, status });
      this.selectedRollbackReason.set('');
      this.statusUpdateError.set(null);
      this.statusUpdateSuccess.set(null);
      return;
    }

    this.saveOrderStatus(order, status);
  }

  confirmRollback(): void {
    const pending = this.pendingStatusChange();
    const reason = this.selectedRollbackReason();
    if (!pending || !reason) {
      return;
    }
    this.pendingStatusChange.set(null);
    this.saveOrderStatus(pending.order, pending.status, reason);
  }

  cancelRollback(): void {
    this.pendingStatusChange.set(null);
    this.selectedRollbackReason.set('');
  }

  private saveOrderStatus(order: SellerOrder, status: OrderStatus, justificacion?: string): void {
    this.statusSavingOrderId.set(order.id);
    this.statusUpdateError.set(null);
    this.statusUpdateSuccess.set(null);
    this.pedidoService.actualizarEstadoPedido(order.id, this.toApiStatus(status), justificacion).subscribe({
      next: () => {
        this.orders.update((orders) => orders.map((currentOrder) =>
          currentOrder.id === order.id ? { ...currentOrder, status } : currentOrder));
        this.statusSavingOrderId.set(null);
        this.statusUpdateSuccess.set('Estado actualizado correctamente.');
      },
      error: () => {
        this.statusSavingOrderId.set(null);
        this.statusUpdateSuccess.set(null);
        this.statusUpdateError.set('No se ha podido actualizar el estado del pedido. Inténtalo de nuevo.');
      },
    });
  }

  private isRollback(current: OrderStatus, next: OrderStatus): boolean {
    const order: OrderStatus[] = ['Pendiente', 'En preparación', 'Enviado', 'Entregado'];
    const currentIndex = order.indexOf(current);
    const nextIndex = order.indexOf(next);
    return currentIndex >= 0 && nextIndex >= 0 && nextIndex < currentIndex;
  }

  private toApiStatus(status: OrderStatus): SellerOrderStatus {
    const statuses: Record<OrderStatus, SellerOrderStatus> = {
      Pendiente: 'PENDIENTE',
      'En preparación': 'PREPARANDO',
      Enviado: 'ENVIADO',
      Entregado: 'ENTREGADO',
      Cancelado: 'CANCELADO',
    };
    return statuses[status];
  }

  toggleSidebar(): void {
    const collapsed = !this.sidebarCollapsed();
    this.sidebarCollapsed.set(collapsed);

    if (isPlatformBrowser(this.platformId)) {
      localStorage.setItem('seller-sidebar-collapsed', String(collapsed));
    }
  }

  private readSidebarPreference(): boolean {
    if (!isPlatformBrowser(this.platformId)) {
      return false;
    }

    return localStorage.getItem('seller-sidebar-collapsed') === 'true'
      || localStorage.getItem('seller-orders-sidebar-collapsed') === 'true';
  }

}
