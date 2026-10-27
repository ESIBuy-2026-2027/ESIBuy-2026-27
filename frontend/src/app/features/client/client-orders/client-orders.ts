import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { CartService } from '../../../core/services/cart.service';
import { CustomerOrder, PedidoService } from '../../../core/services/pedido.service';

@Component({
  selector: 'app-client-orders',
  standalone: true,
  imports: [FormsModule, RouterLink],
  templateUrl: './client-orders.html',
  styleUrl: './client-orders.css',
})
export class ClientOrders implements OnInit {
  readonly cartService = inject(CartService);
  private readonly pedidoService = inject(PedidoService);
  readonly sidebarCollapsed = signal(false);
  readonly orders = signal<CustomerOrder[]>([]);
  readonly searchQuery = signal('');
  readonly activeTab = signal<'orders' | 'pending'>('orders');
  readonly period = signal<'all' | '3months' | '6months' | 'year'>('all');
  readonly loading = signal(true);
  readonly error = signal<string | null>(null);
  readonly filteredOrders = computed(() => {
    const query = this.searchQuery().trim().toLocaleLowerCase();
    const cutoff = this.getPeriodCutoff(this.period());

    return this.orders().filter((order) => {
      const matchesTab = this.activeTab() === 'orders'
        || order.estado === 'PENDIENTE'
        || order.estado === 'PREPARANDO';
      const matchesSearch = !query
        || order.pedidoId.toLocaleLowerCase().includes(query)
        || order.items.some((item) => item.nombre.toLocaleLowerCase().includes(query));
      const matchesPeriod = !cutoff || new Date(order.fechaCreacion) >= cutoff;
      return matchesTab && matchesSearch && matchesPeriod;
    });
  });
  private readonly currencyFormatter = new Intl.NumberFormat('es-ES', {
    style: 'currency',
    currency: 'EUR',
  });

  ngOnInit(): void {
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
    return new Intl.DateTimeFormat('es-ES', { dateStyle: 'medium', timeStyle: 'short' }).format(new Date(date));
  }

  toggleSidebar(): void {
    this.sidebarCollapsed.update((collapsed) => !collapsed);
  }

  selectTab(tab: 'orders' | 'pending'): void {
    this.activeTab.set(tab);
  }

  searchOrders(): void {
    this.searchQuery.update((query) => query.trim());
  }

  private getPeriodCutoff(period: 'all' | '3months' | '6months' | 'year'): Date | null {
    if (period === 'all') {
      return null;
    }

    const cutoff = new Date();
    const months = period === 'year' ? 12 : period === '6months' ? 6 : 3;
    cutoff.setMonth(cutoff.getMonth() - months);
    return cutoff;
  }
}
