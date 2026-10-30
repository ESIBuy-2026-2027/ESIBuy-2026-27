import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import {
  CustomerOrder,
  CustomerOrderFulfillmentStatus,
  CustomerOrderPaymentStatus,
  getCustomerOrderFulfillmentStatus,
  getCustomerOrderPaymentStatus,
  PedidoService,
} from '../../../core/services/pedido.service';

type PeriodFilter = 'all' | '3months' | '6months' | 'year';
type SortOrder = 'date-desc' | 'date-asc' | 'amount-desc' | 'amount-asc';
type OrderStatusFilter = CustomerOrderFulfillmentStatus | 'TODOS';
type PaymentStatusFilter = CustomerOrderPaymentStatus | 'TODOS';

@Component({
  selector: 'app-client-orders',
  standalone: true,
  imports: [FormsModule, RouterLink],
  templateUrl: './client-orders.html',
  styleUrl: './client-orders.css',
})
export class ClientOrders implements OnInit {
  private readonly pedidoService = inject(PedidoService);
  readonly sidebarCollapsed = signal(false);
  readonly orders = signal<CustomerOrder[]>([]);
  readonly searchQuery = signal('');
  readonly filtersOpen = signal(false);
  readonly period = signal<PeriodFilter>('all');
  readonly statusFilter = signal<OrderStatusFilter>('TODOS');
  readonly paymentFilter = signal<PaymentStatusFilter>('TODOS');
  readonly sortOrder = signal<SortOrder>('date-desc');
  readonly pageIndex = signal(0);
  readonly pageSize = 10;
  readonly loading = signal(true);
  readonly error = signal<string | null>(null);
  readonly orderCounts = computed(() => {
    const orders = this.orders();
    return {
      total: orders.length,
      pending: orders.filter((order) => getCustomerOrderFulfillmentStatus(order) === 'PENDIENTE').length,
      preparing: orders.filter((order) => getCustomerOrderFulfillmentStatus(order) === 'PREPARANDO').length,
      shipped: orders.filter((order) => getCustomerOrderFulfillmentStatus(order) === 'ENVIADO').length,
      delivered: orders.filter((order) => getCustomerOrderFulfillmentStatus(order) === 'ENTREGADO').length,
      cancelled: orders.filter((order) => getCustomerOrderFulfillmentStatus(order) === 'CANCELADO').length,
    };
  });
  readonly statusSummaries = computed(() => {
    const counts = this.orderCounts();
    return [
      { label: 'Pendientes', status: 'PENDIENTE' as const, count: counts.pending, className: 'summary-pending' },
      { label: 'En preparación', status: 'PREPARANDO' as const, count: counts.preparing, className: 'summary-preparing' },
      { label: 'Enviados', status: 'ENVIADO' as const, count: counts.shipped, className: 'summary-shipped' },
      { label: 'Entregados', status: 'ENTREGADO' as const, count: counts.delivered, className: 'summary-delivered' },
    ];
  });
  readonly hasActiveFilters = computed(() =>
    this.statusFilter() !== 'TODOS'
    || this.paymentFilter() !== 'TODOS'
    || this.period() !== 'all'
    || this.sortOrder() !== 'date-desc'
  );
  readonly filteredOrders = computed(() => {
    const query = this.searchQuery().trim().toLocaleLowerCase();
    const cutoff = this.getPeriodCutoff(this.period());

    const filtered = this.orders().filter((order) => {
      const matchesSearch = !query
        || order.pedidoId.toLocaleLowerCase().includes(query)
        || order.items.some((item) => item.nombre.toLocaleLowerCase().includes(query));
      const matchesPeriod = !cutoff || new Date(order.fechaCreacion) >= cutoff;
      const fulfillmentStatus = getCustomerOrderFulfillmentStatus(order);
      const matchesStatus = this.statusFilter() === 'TODOS'
        || fulfillmentStatus === this.statusFilter();
      const matchesPayment = this.paymentFilter() === 'TODOS'
        || getCustomerOrderPaymentStatus(order) === this.paymentFilter();
      return matchesSearch && matchesPeriod && matchesStatus && matchesPayment;
    });
    const sort = this.sortOrder();
    return filtered.sort((first, second) => {
      if (sort === 'date-desc' || sort === 'date-asc') {
        const difference = Date.parse(first.fechaCreacion) - Date.parse(second.fechaCreacion);
        return sort === 'date-desc' ? -difference : difference;
      }
      const difference = first.total - second.total;
      return sort === 'amount-desc' ? -difference : difference;
    });
  });
  readonly pageCount = computed(() => Math.max(1, Math.ceil(this.filteredOrders().length / this.pageSize)));
  readonly visibleOrders = computed(() => {
    const start = this.pageIndex() * this.pageSize;
    return this.filteredOrders().slice(start, start + this.pageSize);
  });
  private readonly currencyFormatter = new Intl.NumberFormat('es-ES', {
    style: 'currency',
    currency: 'EUR',
  });

  ngOnInit(): void {
    this.loadOrders();
  }

  loadOrders(): void {
    this.loading.set(true);
    this.error.set(null);
    this.pageIndex.set(0);
    this.pedidoService.obtenerPedidosDelCliente().subscribe({
      next: (orders) => {
        this.orders.set(orders);
        this.loading.set(false);
      },
      error: () => {
        this.error.set('No se pudo cargar tu historial de compras. Inténtalo de nuevo.');
        this.loading.set(false);
      },
    });
  }

  formatPrice(price: number): string {
    return this.currencyFormatter.format(price);
  }

  formatDate(date: string): string {
    const parsedDate = new Date(date);
    return Number.isNaN(parsedDate.getTime())
      ? 'No disponible'
      : new Intl.DateTimeFormat('es-ES', { dateStyle: 'medium', timeStyle: 'short' }).format(parsedDate);
  }

  getStatusLabel(order: CustomerOrder): string {
    const status = getCustomerOrderFulfillmentStatus(order);
    const labels: Record<CustomerOrderFulfillmentStatus, string> = {
      PENDIENTE: 'Pendiente',
      PREPARANDO: 'En preparación',
      ENVIADO: 'Enviado',
      ENTREGADO: 'Entregado',
      CANCELADO: 'Cancelado',
    };
    return labels[status];
  }

  getStatusClass(order: CustomerOrder): string {
    const status = getCustomerOrderFulfillmentStatus(order);
    const classes: Record<CustomerOrderFulfillmentStatus, string> = {
      PENDIENTE: 'status-pending',
      PREPARANDO: 'status-preparing',
      ENVIADO: 'status-shipped',
      ENTREGADO: 'status-delivered',
      CANCELADO: 'status-cancelled',
    };
    return classes[status];
  }

  getPaymentLabel(order: CustomerOrder): string {
    return getCustomerOrderPaymentStatus(order) === 'PAGADO' ? 'Pagado' : 'Pendiente';
  }

  getPaymentClass(order: CustomerOrder): string {
    return getCustomerOrderPaymentStatus(order) === 'PAGADO' ? 'payment-paid' : 'payment-pending';
  }

  getItemCount(order: CustomerOrder): number {
    return order.items.reduce((count, item) => count + item.cantidad, 0);
  }

  toggleSidebar(): void {
    this.sidebarCollapsed.update((collapsed) => !collapsed);
  }

  searchOrders(): void {
    this.searchQuery.update((query) => query.trim());
    this.pageIndex.set(0);
  }

  setSearchQuery(query: string): void {
    this.searchQuery.set(query);
    this.pageIndex.set(0);
  }

  setStatusFilter(status: OrderStatusFilter): void {
    this.statusFilter.set(status);
    this.pageIndex.set(0);
  }

  setPaymentFilter(status: PaymentStatusFilter): void {
    this.paymentFilter.set(status);
    this.pageIndex.set(0);
  }

  setPeriod(period: PeriodFilter): void {
    this.period.set(period);
    this.pageIndex.set(0);
  }

  setSortOrder(sort: SortOrder): void {
    this.sortOrder.set(sort);
    this.pageIndex.set(0);
  }

  toggleFilters(): void {
    this.filtersOpen.update((open) => !open);
  }

  closeFilters(): void {
    this.filtersOpen.set(false);
  }

  selectStatus(status: CustomerOrderFulfillmentStatus): void {
    this.setStatusFilter(this.statusFilter() === status ? 'TODOS' : status);
  }

  clearFilters(): void {
    this.searchQuery.set('');
    this.statusFilter.set('TODOS');
    this.paymentFilter.set('TODOS');
    this.period.set('all');
    this.sortOrder.set('date-desc');
    this.pageIndex.set(0);
  }

  previousPage(): void {
    this.pageIndex.update((page) => Math.max(0, page - 1));
  }

  nextPage(): void {
    this.pageIndex.update((page) => Math.min(this.pageCount() - 1, page + 1));
  }

  pageLabel(): string {
    const orderCount = this.filteredOrders().length;
    if (orderCount === 0) {
      return '0 pedidos';
    }
    const firstItem = this.pageIndex() * this.pageSize + 1;
    const lastItem = Math.min(firstItem + this.pageSize - 1, orderCount);
    return `${firstItem}–${lastItem} de ${orderCount}`;
  }

  private getPeriodCutoff(period: PeriodFilter): Date | null {
    if (period === 'all') {
      return null;
    }

    const cutoff = new Date();
    const months = period === 'year' ? 12 : period === '6months' ? 6 : 3;
    cutoff.setMonth(cutoff.getMonth() - months);
    return cutoff;
  }
}
