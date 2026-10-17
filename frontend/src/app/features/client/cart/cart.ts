import { Component, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { CartService } from './cart.service';

@Component({
  selector: 'app-client-cart',
  standalone: true,
  imports: [RouterLink],
  templateUrl: './cart.html',
  styleUrl: './cart.css',
})
export class ClientCart {
  readonly cartService = inject(CartService);
  private readonly currencyFormatter = new Intl.NumberFormat('es-ES', {
    style: 'currency',
    currency: 'EUR',
  });

  formatPrice(price: number): string {
    return this.currencyFormatter.format(price);
  }
}
