import { isPlatformBrowser } from '@angular/common';
import { Component, computed, inject, PLATFORM_ID, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink, RouterLinkActive } from '@angular/router';

type OrderStatus = 'Pendiente' | 'En preparación' | 'Enviado' | 'Entregado';

interface SellerOrder {
  id: string;
  date: string;
  customer: string;
  products: string;
  total: number;
  status: OrderStatus;
}



const MOCK_ORDERS: SellerOrder[] = [
  {
    id: 'PED-1048',
    date: '15 oct 2026',
    customer: 'Lucía Gómez',
    products: 'Auriculares · 1 ud.',
    total: 25,
    status: 'Pendiente',
  },
  {
    id: 'PED-1047',
    date: '14 oct 2026',
    customer: 'Marcos Ruiz',
    products: 'Sudadera · 2 uds.',
    total: 60,
    status: 'En preparación',
  },
  {
    id: 'PED-1046',
    date: '13 oct 2026',
    customer: 'Elena Martín',
    products: 'Casita de juguete · 1 ud.',
    total: 60,
    status: 'Enviado',
  },
  {
    id: 'PED-1045',
    date: '11 oct 2026',
    customer: 'Pablo Sánchez',
    products: 'Batería Yamaha · 1 ud.',
    total: 300,
    status: 'Entregado',
  },
  {
    id: 'PED-1044',
    date: '10 oct 2026',
    customer: 'Sara López',
    products: 'Sofá · 1 ud.',
    total: 250,
    status: 'Entregado',
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
  readonly sidebarCollapsed = signal(this.readSidebarPreference());

  readonly visibleOrders = computed(() => {
    const query = this.orderSearch().trim().toLocaleLowerCase();
    const status = this.selectedStatus();

    return this.orders().filter((order) => {
      const matchesSearch = !query
        || order.id.toLocaleLowerCase().includes(query)
        || order.customer.toLocaleLowerCase().includes(query)
        || order.products.toLocaleLowerCase().includes(query);
      const matchesStatus = status === 'Todos' || order.status === status;
      return matchesSearch && matchesStatus;
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

  toggleSidebar(): void {
    const collapsed = !this.sidebarCollapsed();
    this.sidebarCollapsed.set(collapsed);

    if (isPlatformBrowser(this.platformId)) {
      localStorage.setItem('seller-orders-sidebar-collapsed', String(collapsed));
    }
  }

  private readSidebarPreference(): boolean {
    if (!isPlatformBrowser(this.platformId)) {
      return false;
    }

    return localStorage.getItem('seller-orders-sidebar-collapsed') === 'true';
  }
}
