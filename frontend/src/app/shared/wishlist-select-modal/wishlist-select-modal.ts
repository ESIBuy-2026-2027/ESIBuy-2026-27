import { Component, effect, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { WishlistService } from '../../core/services/wishlist.service';
import { ToastService } from '../../core/services/toast.service';

@Component({
  selector: 'app-wishlist-select-modal',
  standalone: true,
  imports: [FormsModule],
  templateUrl: './wishlist-select-modal.html',
  styleUrl: './wishlist-select-modal.css',
})
export class WishlistSelectModalComponent {
  readonly wishlistService = inject(WishlistService);
  private readonly toastService = inject(ToastService);

  readonly selectedWishlistId = signal<string>('');
  readonly creatingNew = signal(false);
  readonly newListName = signal('');
  readonly saving = signal(false);
  readonly errorMessage = signal('');

  constructor() {
    effect(() => {
      const open = this.wishlistService.selectModalOpen();
      const lists = this.wishlistService.wishlists();
      if (open && lists.length > 0) {
        // Pre-select the first list if none selected
        if (!this.selectedWishlistId() || !lists.some((l) => l.id === this.selectedWishlistId())) {
          this.selectedWishlistId.set(lists[0].id);
        }
        this.creatingNew.set(false);
        this.newListName.set('');
        this.errorMessage.set('');
      }
    });
  }

  selectWishlist(id: string): void {
    this.selectedWishlistId.set(id);
    this.creatingNew.set(false);
    this.errorMessage.set('');
  }

  toggleCreatingNew(): void {
    this.creatingNew.update((val) => !val);
    this.errorMessage.set('');
    if (this.creatingNew()) {
      this.newListName.set('');
    }
  }

  close(): void {
    this.wishlistService.closeWishlistSelector();
  }

  save(): void {
    const productId = this.wishlistService.targetProductId();
    if (!productId) {
      this.close();
      return;
    }

    if (this.creatingNew()) {
      const name = this.newListName().trim();
      if (!name) {
        this.errorMessage.set('El nombre de la lista es obligatorio.');
        return;
      }
      if (name.length < 2 || name.length > 50) {
        this.errorMessage.set('El nombre debe tener entre 2 y 50 caracteres.');
        return;
      }
      if (
        this.wishlistService.wishlists().some((l) => l.name.toLowerCase() === name.toLowerCase())
      ) {
        this.errorMessage.set('Ya tienes una lista de deseos con ese nombre.');
        return;
      }

      this.saving.set(true);
      this.wishlistService.createWishlist(name).subscribe({
        next: (created) => {
          this.wishlistService.addToWishlist(productId, created.id).subscribe({
            next: () => {
              this.saving.set(false);
              this.toastService.show(`Producto añadido a "${created.name}"`, 'success');
              this.close();
            },
            error: () => {
              this.saving.set(false);
              this.toastService.show('Error al añadir el producto a la lista', 'error');
            },
          });
        },
        error: () => {
          this.saving.set(false);
          this.errorMessage.set('Error al crear la nueva lista.');
        },
      });
      return;
    }

    const listId = this.selectedWishlistId();
    if (!listId) {
      this.errorMessage.set('Selecciona una lista de deseos.');
      return;
    }

    const targetList = this.wishlistService.wishlists().find((l) => l.id === listId);
    const listName = targetList ? targetList.name : 'lista de deseos';

    this.saving.set(true);
    this.wishlistService.addToWishlist(productId, listId).subscribe({
      next: () => {
        this.saving.set(false);
        this.toastService.show(`Producto añadido a "${listName}"`, 'success');
        this.close();
      },
      error: () => {
        this.saving.set(false);
        this.toastService.show('Error al guardar el producto en la lista', 'error');
      },
    });
  }
}
