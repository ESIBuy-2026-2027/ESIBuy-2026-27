import { Component, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { CartService } from '../cart/cart.service';

@Component({
  selector: 'app-client-orders',
  standalone: true,
  imports: [RouterLink],
  templateUrl: './client-orders.html',
  styleUrl: './client-orders.css',
})
export class ClientOrders {
  readonly cartService = inject(CartService);
  readonly sidebarCollapsed = signal(false);

  toggleSidebar(): void {
    this.sidebarCollapsed.update((collapsed) => !collapsed);
  }
}

