import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { WishlistInfo, WishlistService } from '../../../core/services/wishlist.service';
import { ToastService } from '../../../core/services/toast.service';
import { CartService } from '../../../core/services/cart.service';
import { AuthService } from '../../../core/services/auth.service';
import { ProductItem } from '../../../core/services/products.service';

@Component({
  selector: 'app-client-wishlist',
  standalone: true,
  imports: [FormsModule, RouterLink],
  templateUrl: './client-wishlist.html',
  styleUrl: './client-wishlist.css',
})
export class ClientWishlist implements OnInit {
  readonly wishlistService = inject(WishlistService);
  readonly toastService = inject(ToastService);
  readonly cartService = inject(CartService);
  readonly authService = inject(AuthService);

  readonly isPremiumUser = computed(
    () => this.authService.currentUser()?.customerType === 'PREMIUM'
  );

  private readonly currencyFormatter = new Intl.NumberFormat('es-ES', {
    style: 'currency',
    currency: 'EUR',
  });

  items = signal<ProductItem[]>([]);
  loading = signal(true);
  error = signal(false);
  selectedWishlist = signal<WishlistInfo | null>(null);

  // Dialogs state
  createDialogOpen = signal(false);
  newListName = signal('');
  createError = signal('');
  creatingList = signal(false);

  editDialogOpen = signal(false);
  editListName = signal('');
  editError = signal('');
  editingList = signal(false);

  deleteDialogOpen = signal(false);
  deletingList = signal(false);

  readonly sidebarCollapsed = signal(
    typeof localStorage !== 'undefined' &&
      localStorage.getItem('client-sidebar-collapsed') === 'true',
  );
  animateTransition = false;
  readonly quantities = signal<Record<string, number>>({});
  readonly addingProducts = signal<Record<string, boolean>>({});
  readonly removingProducts = signal<Record<string, boolean>>({});

  toggleSidebar(): void {
    this.animateTransition = true;
    const collapsed = !this.sidebarCollapsed();
    this.sidebarCollapsed.set(collapsed);
    localStorage.setItem('client-sidebar-collapsed', String(collapsed));
  }

  getQuantity(productId: string): number {
    return this.quantities()[productId] ?? 1;
  }

  setQuantity(productId: string, qty: number, maxStock: number): void {
    const val = Math.max(1, Math.min(qty || 1, maxStock));
    this.quantities.update((map) => ({ ...map, [productId]: val }));
  }

  addToCart(product: ProductItem, event?: Event): void {
    if (event) {
      event.stopPropagation();
      event.preventDefault();
    }

    if (!product.available || product.stock === 0) {
      this.toastService.show('El producto no está disponible para compra.', 'error');
      return;
    }

    const qty = this.getQuantity(product.id);
    if (this.addingProducts()[product.id]) return;
    this.addingProducts.update((states) => ({ ...states, [product.id]: true }));
    this.cartService
      .addItem(product.id, qty)
      .subscribe({
        next: () => {
          this.toastService.show('Producto añadido al carrito', 'success');
        },
        error: () => {
          this.toastService.show('Error al añadir producto al carrito', 'error');
        },
      })
      .add(() => this.addingProducts.update((states) => ({ ...states, [product.id]: false })));
  }

  ngOnInit(): void {
    this.loadWishlistsAndProducts();
  }

  loadWishlistsAndProducts(preferredListId?: string): void {
    this.loading.set(true);
    this.error.set(false);

    this.wishlistService.getWishlists().subscribe({
      next: (lists) => {
        if (lists.length === 0) {
          this.selectedWishlist.set(null);
          this.items.set([]);
          this.loading.set(false);
          return;
        }

        let target = lists[0];
        if (preferredListId) {
          const match = lists.find((l) => l.id === preferredListId);
          if (match) {
            target = match;
          }
        } else if (this.selectedWishlist()) {
          const match = lists.find((l) => l.id === this.selectedWishlist()?.id);
          if (match) {
            target = match;
          }
        }

        this.selectWishlist(target);
      },
      error: () => {
        this.loading.set(false);
        this.error.set(true);
      },
    });
  }

  selectWishlist(wishlist: WishlistInfo): void {
    this.selectedWishlist.set(wishlist);
    this.loading.set(true);
    this.error.set(false);

    this.wishlistService.getWishlist(wishlist.id).subscribe({
      next: (products) => {
        this.items.set(products);
        this.loading.set(false);
      },
      error: () => {
        this.loading.set(false);
        this.error.set(true);
      },
    });
  }

  removeFromWishlist(productId: string, event: Event): void {
    event.stopPropagation();
    event.preventDefault();

    if (this.removingProducts()[productId]) return;
    this.removingProducts.update((states) => ({ ...states, [productId]: true }));

    const listId = this.selectedWishlist()?.id;
    this.wishlistService
      .removeFromWishlist(productId, listId)
      .subscribe({
        next: (res) => {
          this.items.update((current) => current.filter((p) => p.id !== productId));
          this.toastService.show(res.message, 'success');
        },
        error: () => {
          this.toastService.show('Error al eliminar producto de la lista de deseos', 'error');
        },
      })
      .add(() => this.removingProducts.update((states) => ({ ...states, [productId]: false })));
  }

  // --- Crear nueva lista de deseos ---
  openCreateDialog(): void {
    this.newListName.set('');
    this.createError.set('');
    this.createDialogOpen.set(true);
  }

  closeCreateDialog(): void {
    this.createDialogOpen.set(false);
    this.newListName.set('');
    this.createError.set('');
  }

  saveNewList(): void {
    const name = this.newListName().trim();
    if (!name) {
      this.createError.set('El nombre de la lista de deseos es obligatorio.');
      return;
    }
    if (name.length < 2 || name.length > 50) {
      this.createError.set('El nombre debe tener entre 2 y 50 caracteres.');
      return;
    }
    if (this.wishlistService.wishlists().some((l) => l.name.toLowerCase() === name.toLowerCase())) {
      this.createError.set('Ya tienes una lista de deseos con ese nombre.');
      return;
    }

    this.creatingList.set(true);
    this.wishlistService.createWishlist(name).subscribe({
      next: (created) => {
        this.creatingList.set(false);
        this.closeCreateDialog();
        this.toastService.show(`Lista "${created.name}" creada con éxito`, 'success');
        this.selectWishlist(created);
      },
      error: (err) => {
        this.creatingList.set(false);
        this.createError.set(err?.error?.message || 'Error al crear la lista de deseos');
      },
    });
  }

  // --- Editar nombre de lista ---
  openEditDialog(): void {
    const current = this.selectedWishlist();
    if (!current) return;
    this.editListName.set(current.name);
    this.editError.set('');
    this.editDialogOpen.set(true);
  }

  closeEditDialog(): void {
    this.editDialogOpen.set(false);
    this.editListName.set('');
    this.editError.set('');
  }

  saveEditedName(): void {
    const current = this.selectedWishlist();
    if (!current) return;

    const name = this.editListName().trim();
    if (!name) {
      this.editError.set('El nombre de la lista de deseos es obligatorio.');
      return;
    }
    if (name.length < 2 || name.length > 50) {
      this.editError.set('El nombre debe tener entre 2 y 50 caracteres.');
      return;
    }
    if (
      this.wishlistService
        .wishlists()
        .some((l) => l.id !== current.id && l.name.toLowerCase() === name.toLowerCase())
    ) {
      this.editError.set('Ya tienes otra lista de deseos con ese nombre.');
      return;
    }

    this.editingList.set(true);
    this.wishlistService.updateWishlistName(current.id, name).subscribe({
      next: (updated) => {
        this.editingList.set(false);
        this.selectedWishlist.update((w) => (w ? { ...w, name: updated.name } : null));
        this.closeEditDialog();
        this.toastService.show('Nombre de la lista actualizado', 'success');
      },
      error: (err) => {
        this.editingList.set(false);
        this.editError.set(err?.error?.message || 'Error al actualizar el nombre');
      },
    });
  }

  // --- Borrar lista de deseos ---
  openDeleteDialog(): void {
    if (!this.selectedWishlist()) return;
    this.deleteDialogOpen.set(true);
  }

  closeDeleteDialog(): void {
    this.deleteDialogOpen.set(false);
  }

  confirmDeleteList(): void {
    const current = this.selectedWishlist();
    if (!current) return;

    this.deletingList.set(true);
    this.wishlistService.deleteWishlist(current.id).subscribe({
      next: () => {
        this.deletingList.set(false);
        this.closeDeleteDialog();
        this.toastService.show(`Lista "${current.name}" eliminada`, 'success');

        const remaining = this.wishlistService.wishlists();
        if (remaining.length > 0) {
          this.selectWishlist(remaining[0]);
        } else {
          this.selectedWishlist.set(null);
          this.items.set([]);
        }
      },
      error: () => {
        this.deletingList.set(false);
        this.toastService.show('Error al eliminar la lista de deseos', 'error');
      },
    });
  }

  formatPrice(price: number): string {
    return this.currencyFormatter.format(price);
  }

  getTotalDiscount(p: ProductItem): number {
    const standardDiscount = Math.max(0, p.discount ?? 0);
    const premiumDiscount = Math.max(0, p.premiumDiscount ?? 0);
    return Math.min(100, standardDiscount + premiumDiscount);
  }

  getDiscountedPrice(p: ProductItem): number {
    const discount = Math.max(0, p.discount ?? 0);
    return Math.round(p.price * (1 - discount / 100) * 100) / 100;
  }

  getPremiumPrice(p: ProductItem): number {
    const totalDiscount = this.getTotalDiscount(p);
    return Math.round(p.price * (1 - totalDiscount / 100) * 100) / 100;
  }

  hasStandardDiscount(p: ProductItem): boolean {
    return (p.discount ?? 0) > 0;
  }

  hasPremiumDiscount(p: ProductItem): boolean {
    return (p.premiumDiscount ?? 0) > 0;
  }

  hasAnyDiscount(p: ProductItem): boolean {
    return this.hasStandardDiscount(p) || this.hasPremiumDiscount(p);
  }

  getDisplayPrice(p: ProductItem): number {
    return this.isPremiumUser()
      ? this.getPremiumPrice(p)
      : this.getDiscountedPrice(p);
  }
}
