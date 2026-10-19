import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { ProductsService, ProductItem, Category } from '../products.service';
import { WishlistService } from '../../../core/services/wishlist.service';
import { ToastService } from '../../../core/services/toast.service';
import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-product-detail',
  standalone: true,
  imports: [],
  templateUrl: './product-detail.html',
  styleUrl: './product-detail.css',
})
export class ProductDetail implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly productsService = inject(ProductsService);
  readonly wishlistService = inject(WishlistService);
  private readonly toastService = inject(ToastService);
  readonly authService = inject(AuthService);

  private readonly currencyFormatter = new Intl.NumberFormat('es-ES', {
    style: 'currency',
    currency: 'EUR',
  });

  product = signal<ProductItem | null>(null);
  categories = signal<Category[]>([]);
  loading = signal(true);
  error = signal(false);
  errorMessage = signal<string | null>(null);

  isClient = computed(() => this.authService.currentUser()?.role === 'CLIENT');
  isAdmin = computed(() => this.authService.currentUser()?.role === 'ADMIN');
  isSeller = computed(() => this.authService.currentUser()?.role === 'SELLER');

  isFavorite = computed(() => {
    const p = this.product();
    return p ? this.wishlistService.isFavorite(p.id) : false;
  });

  hasOffers = computed(() => {
    const p = this.product();
    if (!p) return false;
    return (p.discount != null && p.discount > 0) || (p.premiumDiscount != null && p.premiumDiscount > 0);
  });

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (!id) {
      this.error.set(true);
      this.errorMessage.set('Identificador de producto no válido');
      this.loading.set(false);
      return;
    }

    this.loadCategories();
    this.loadProduct(id);
    this.wishlistService.refreshIfClient();
  }

  loadCategories(): void {
    this.productsService.getCategories().subscribe({
      next: (categories) => this.categories.set(categories),
      error: () => {},
    });
  }

  loadProduct(id: string): void {
    this.loading.set(true);
    this.error.set(false);

    this.productsService.getProductById(id).subscribe({
      next: (data) => {
        this.product.set(data);
        this.loading.set(false);
      },
      error: (err) => {
        this.loading.set(false);
        this.error.set(true);
        if (err.status === 404) {
          this.errorMessage.set('El producto no existe.');
        } else {
          this.errorMessage.set('No se pudo cargar la información del producto.');
        }
      },
    });
  }

  toggleFavorite(): void {
    const p = this.product();
    if (!p) return;

    const user = this.authService.currentUser();
    if (!user) {
      this.toastService.show('Debes iniciar sesión con tu cuenta de cliente para guardar favoritos.', 'info');
      void this.router.navigate(['/login']);
      return;
    }

    if (user.role !== 'CLIENT') {
      this.toastService.show('Solo los clientes pueden guardar productos en su lista de deseos.', 'info');
      return;
    }

    this.wishlistService.toggleWishlist(p.id).subscribe({
      next: (res) => {
        this.toastService.show(res.message, 'success');
      },
      error: () => {
        this.toastService.show('Error al actualizar la lista de deseos', 'error');
      },
    });
  }

  goBack(): void {
    if (window.history.length > 1) {
      window.history.back();
    } else {
      void this.router.navigate(['/']);
    }
  }

  categoryName(id: string): string {
    return this.categories().find((c) => c.id === id)?.name ?? 'Categoría';
  }

  formatPrice(price: number): string {
    return this.currencyFormatter.format(price);
  }

  onBuy(): void {
    const user = this.authService.currentUser();
    if (!user) {
      this.toastService.show('Debes iniciar sesión con tu cuenta de cliente para realizar compras.', 'info');
      void this.router.navigate(['/login']);
      return;
    }

    if (user.role !== 'CLIENT') {
      this.toastService.show('Solo los clientes pueden realizar compras.', 'info');
      return;
    }

    this.toastService.show('Función de compra en desarrollo.', 'info');
  }
}

