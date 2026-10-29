import { Component, OnInit, inject, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { CartService } from '../../../core/services/cart.service';
import { CustomerOrder, PedidoService } from '../../../core/services/pedido.service';

@Component({
  selector: 'app-client-order-detail',
  standalone: true,
  imports: [RouterLink],
  templateUrl: './client-order-detail.html',
  styleUrl: './client-order-detail.css',
})
export class ClientOrderDetail implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly pedidoService = inject(PedidoService);
  private readonly cartService = inject(CartService);
  readonly order = signal<CustomerOrder | null>(null);
  readonly loading = signal(true);
  readonly error = signal<string | null>(null);
  readonly actionError = signal<string | null>(null);
  readonly addingProductId = signal<string | null>(null);
  private readonly currencyFormatter = new Intl.NumberFormat('es-ES', {
    style: 'currency',
    currency: 'EUR',
  });

  ngOnInit(): void {
    const pedidoId = this.route.snapshot.paramMap.get('pedidoId');
    if (!pedidoId) {
      this.error.set('No se ha indicado un pedido.');
      this.loading.set(false);
      return;
    }
    this.pedidoService.obtenerPedidoDelCliente(pedidoId).subscribe({
      next: (order) => {
        this.order.set(order);
        this.loading.set(false);
      },
      error: () => {
        this.error.set('No se pudo cargar el detalle del pedido.');
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

  buyAgain(productId: string, quantity: number): void {
    if (this.addingProductId()) {
      return;
    }

    this.actionError.set(null);
    this.addingProductId.set(productId);
    this.cartService.addItem(productId, quantity).subscribe({
      next: () => this.addingProductId.set(null),
      error: () => {
        this.actionError.set('No se pudo añadir el producto al carrito. Puede que ya no esté disponible.');
        this.addingProductId.set(null);
      },
    });
  }

  viewProduct(productId: string): void {
    void this.router.navigate(['/products', productId]);
  }
}
