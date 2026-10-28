import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { ProductsService, ProductItem, Category } from '../../../core/services/products.service';
import { WishlistService } from '../../../core/services/wishlist.service';
import { ToastService } from '../../../core/services/toast.service';
import { AuthService } from '../../../core/services/auth.service';
import { CartService } from '../../../core/services/cart.service';
import { ADMIN_READONLY_ERROR_MESSAGE } from '../../../core/services/admin-product.service';

const LOW_STOCK_THRESHOLD = 5;

@Component({
  selector: 'app-product-detail',
  standalone: true,
  imports: [FormsModule],
  templateUrl: './product-detail.html',
  styleUrl: './product-detail.css',
})
export class ProductDetail implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly productsService = inject(ProductsService);
  readonly wishlistService = inject(WishlistService);
  readonly toastService = inject(ToastService);
  readonly authService = inject(AuthService);
  readonly cartService = inject(CartService);

  readonly quantity = signal(1);

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
  readonly isPremiumUser = computed(() => this.authService.currentUser()?.customerType === 'PREMIUM');

  readonly cartItemCount = computed(() => {
    const p = this.product();
    if (!p) return 0;
    const cartData = typeof this.cartService?.cart === 'function' ? this.cartService.cart() : null;
    return cartData?.items?.find((item) => item.productoId === p.id)?.cantidad ?? 0;
  });

  readonly availableStock = computed(() => {
    const p = this.product();
    if (!p) return 0;
    const inCart = this.cartItemCount();
    return Math.max(0, p.stock - inCart);
  });



  getDiscountedPrice(p: ProductItem): number {
    const discount = Math.min(100, Math.max(0, p.discount ?? 0));
    return this.roundPrice(p.price * (1 - discount / 100));
  }

  getPremiumPrice(p: ProductItem): number {
    const standardDiscount = Math.max(0, p.discount ?? 0);
    const premiumDiscount = Math.max(0, p.premiumDiscount ?? 0);
    const totalDiscount = Math.min(100, standardDiscount + premiumDiscount);

    return this.roundPrice(p.price * (1 - totalDiscount / 100));
  }

  getCurrentPrice(p: ProductItem): number {
    return this.isPremiumUser()
      ? this.getPremiumPrice(p)
      : this.getDiscountedPrice(p);
  }


  hasCurrentDiscount(p: ProductItem): boolean {
    return this.isPremiumUser()
      ? this.hasStandardDiscount(p) || this.hasPremiumDiscount(p)
      : this.hasStandardDiscount(p);
  }


  getPremiumSavings(p: ProductItem): number {
    return this.roundPrice(
      this.getDiscountedPrice(p) - this.getPremiumPrice(p)
    );
  }

  getTotalDiscount(p: ProductItem): number {
    return Math.min(
      100,
      Math.max(0, p.discount ?? 0) + Math.max(0, p.premiumDiscount ?? 0)
    );
  }

  private roundPrice(price: number): number {
    return Math.round((price + Number.EPSILON) * 100) / 100;
  }

  hasStandardDiscount(p: ProductItem): boolean {
    return (p.discount ?? 0) > 0;
  }

  hasPremiumDiscount(p: ProductItem): boolean {
    return (p.premiumDiscount ?? 0) > 0;
  }


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
      error: () => { },
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

    if (user.role === 'ADMIN') {
      this.toastService.show(ADMIN_READONLY_ERROR_MESSAGE, 'error');
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
    const role = this.authService.currentUser()?.role;

    if (role === 'SELLER') {
      void this.router.navigate(['/seller/catalog']);
      return;
    }

    if (role === 'CLIENT' || role === 'ADMIN') {
      void this.router.navigate(['/client/catalog']);
      return;
    }

    if (window.history.length <= 1) {
      void this.router.navigate(['/']);
      return;
    }

    window.history.back();
  }

  categoryName(id: string): string {
    return this.categories().find((c) => c.id === id)?.name ?? 'Categoría';
  }

  formatPrice(price: number): string {
    return this.currencyFormatter.format(price);
  }

  isLowStock(stock: number): boolean {
    return stock > 0 && stock <= LOW_STOCK_THRESHOLD;
  }

  decreaseQuantity(): void {
    this.quantity.update((q) => Math.max(1, q - 1));
  }

  increaseQuantity(): void {
    const max = this.availableStock();
    this.quantity.update((q) => Math.min(max, q + 1));
  }

  setQuantity(val: number | null): void {
    const max = this.availableStock();
    if (val === null || !Number.isFinite(val)) {
      return;
    }
    const normalized = Math.max(1, Math.min(Math.trunc(val), max));
    this.quantity.set(normalized);
  }

  onQuantityInput(event: Event): void {
    const input = event.target as HTMLInputElement;
    const max = this.availableStock();
    if (max <= 0) {
      input.value = '0';
      this.quantity.set(0);
      return;
    }
    let val = parseInt(input.value, 10);
    if (!Number.isNaN(val)) {
      if (val > max) {
        input.value = String(max);
        val = max;
      } else if (val < 1) {
        input.value = '1';
        val = 1;
      }
      this.quantity.set(val);
    }
  }

  onQuantityBlur(event: Event): void {
    const input = event.target as HTMLInputElement;
    const max = this.availableStock();
    let val = parseInt(input.value, 10);
    if (Number.isNaN(val) || val < 1) {
      val = max > 0 ? 1 : 0;
    } else if (val > max) {
      val = max;
    }
    input.value = String(val);
    this.quantity.set(val);
  }

  onBuy(): void {
    this.addToCart();
  }

  addToCart(): void {
    const p = this.product();
    if (!p) return;

    const user = this.authService.currentUser();
    if (!user) {
      this.toastService.show('Debes iniciar sesión con tu cuenta de cliente para realizar compras.', 'info');
      void this.router.navigate(['/login']);
      return;
    }

    if (user.role === 'ADMIN') {
      this.toastService.show(ADMIN_READONLY_ERROR_MESSAGE, 'error');
      return;
    }

    if (user.role !== 'CLIENT') {
      this.toastService.show('Solo los clientes pueden realizar compras.', 'info');
      return;
    }

    const avail = this.availableStock();
    if (avail <= 0) {
      this.toastService.show('No quedan existencias disponibles de este producto.', 'error');
      return;
    }
    const qty = Math.min(this.quantity(), avail);
    this.cartService.addItem(p.id, qty).subscribe({
      next: () => {
        this.toastService.show('Producto añadido al carrito', 'success');
        this.quantity.set(1);
      },
      error: () => {
        this.toastService.show('Error al añadir producto al carrito', 'error');
      },
    });
  }

  readonly imageError = signal(false);

  onImageError(): void {
    this.imageError.set(true);
  }
}
