import { isPlatformBrowser } from '@angular/common';
import { Component, computed, inject, PLATFORM_ID, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink, RouterLinkActive } from '@angular/router';

type OrderStatus = 'Pendiente' | 'En preparación' | 'Enviado' | 'Entregado' | 'Cancelado';

interface SellerOrderItem {
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



const MOCK_ORDERS: SellerOrder[] = [
  {
    id: 'PED-1048',
    date: '15 oct 2026',
    dateValue: '2026-10-15',
    customer: 'Lucía Gómez',
    products: 'Auriculares · 1 ud.',
    total: 25,
    status: 'Pendiente',
    items: [{
      name: 'Auriculares inalámbricos',
      quantity: 1,
      price: 25,
      imageUrl: 'https://images.unsplash.com/photo-1505740420928-5e560c06d30e?auto=format&fit=crop&w=160&q=80',
    }],
    shippingAddress: 'Calle Toledo, 24, 1º B, 13001 Ciudad Real',
    customerEmail: 'lucia.gomez@example.com',
  },
  {
    id: 'PED-1047',
    date: '14 oct 2026',
    dateValue: '2026-10-14',
    customer: 'Marcos Ruiz',
    products: 'Sudadera · 2 uds.',
    total: 60,
    status: 'En preparación',
    items: [{
      name: 'Sudadera básica',
      quantity: 2,
      price: 30,
      imageUrl: 'https://images.unsplash.com/photo-1551488831-00ddcb6c6bd3?auto=format&fit=crop&w=160&q=80',
    }],
    shippingAddress: 'Avenida de Europa, 15, 2º A, 13005 Ciudad Real',
    customerEmail: 'marcos.ruiz@example.com',
  },
  {
    id: 'PED-1046',
    date: '13 oct 2026',
    dateValue: '2026-10-13',
    customer: 'Elena Martín',
    products: 'Casita de juguete · 1 ud.',
    total: 60,
    status: 'Enviado',
    items: [{
      name: 'Casita de juguete',
      quantity: 1,
      price: 60,
      imageUrl: 'https://images.unsplash.com/photo-1594787318286-3d835c1d207f?auto=format&fit=crop&w=160&q=80',
    }],
    shippingAddress: 'Calle Alarcos, 8, bajo, 13001 Ciudad Real',
    customerEmail: 'elena.martin@example.com',
  },
  {
    id: 'PED-1045',
    date: '11 oct 2026',
    dateValue: '2026-10-11',
    customer: 'Pablo Sánchez',
    products: 'Batería Yamaha · 1 ud.',
    total: 300,
    status: 'Entregado',
    items: [{
      name: 'Batería Yamaha',
      quantity: 1,
      price: 300,
      imageUrl: 'https://images.unsplash.com/photo-1519892300165-cb5542fb47c7?auto=format&fit=crop&w=160&q=80',
    }],
    shippingAddress: 'Calle Ciruela, 31, 3º C, 13001 Ciudad Real',
    customerEmail: 'pablo.sanchez@example.com',
  },
  {
    id: 'PED-1044',
    date: '10 oct 2026',
    dateValue: '2026-10-10',
    customer: 'Sara López',
    products: 'Sofá · 1 ud.',
    total: 250,
    status: 'Cancelado',
    items: [{
      name: 'Sofá de tres plazas',
      quantity: 1,
      price: 250,
      imageUrl: 'https://images.unsplash.com/photo-1555041469-a586c61ea9bc?auto=format&fit=crop&w=160&q=80',
    }],
    shippingAddress: 'Calle Mata, 6, 1º, 13004 Ciudad Real',
    customerEmail: 'sara.lopez@example.com',
  },
];

@Component({
  selector: 'app-seller-orders',
  standalone: true,
  imports: [FormsModule, RouterLink, RouterLinkActive],
  templateUrl: './seller-orders.html',
  styleUrl: './seller-orders.css',
})
export class SellerOrders {
  private readonly platformId = inject(PLATFORM_ID);
  readonly orders = signal(MOCK_ORDERS);
  readonly orderSearch = signal('');
  readonly selectedStatus = signal<'Todos' | OrderStatus>('Todos');
  readonly dateFrom = signal('');
  readonly dateTo = signal('');
  readonly filtersOpen = signal(false);
  readonly selectedOrder = signal<SellerOrder | null>(null);
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

  toggleFilters(): void {
    this.filtersOpen.update((isOpen) => !isOpen);
  }

  clearFilters(): void {
    this.selectedStatus.set('Todos');
    this.dateFrom.set('');
    this.dateTo.set('');
  }

  selectStatus(status: OrderStatus): void {
    this.selectedStatus.set(this.selectedStatus() === status ? 'Todos' : status);
  }

  openOrderDetails(order: SellerOrder): void {
    this.selectedOrder.set(order);
  }

  closeOrderDetails(): void {
    this.selectedOrder.set(null);
  }

  updateOrderStatus(status: OrderStatus): void {
    const order = this.selectedOrder();
    if (!order) {
      return;
    }

    const updatedOrder = { ...order, status };
    this.orders.update((orders) => orders.map((currentOrder) =>
      currentOrder.id === order.id ? updatedOrder : currentOrder));
    this.selectedOrder.set(updatedOrder);
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
