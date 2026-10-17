import { HttpErrorResponse } from '@angular/common/http';
import { Component, effect, HostListener, inject, signal } from '@angular/core';
import { Router, RouterLink, RouterOutlet } from '@angular/router';
import { AuthService } from './core/services/auth.service';
import { CartService } from './features/client/cart/cart.service';

@Component({
  imports: [RouterLink, RouterOutlet],
  standalone: true,
  selector: 'app-root',
  styleUrl: './app.css',
  templateUrl: './app.html',
})
export class App {
  readonly authService = inject(AuthService);
  readonly cartService = inject(CartService);
  private readonly router = inject(Router);
  readonly logoutError = signal<string | null>(null);
  readonly profileMenuOpen = signal(false);

  constructor() {
    effect(() => {
      if (this.authService.currentUser()?.role === 'CLIENT') {
        this.cartService.load();
      } else {
        this.cartService.clear();
      }
    });
  }

  toggleProfileMenu(): void {
    this.profileMenuOpen.update((isOpen) => !isOpen);
  }

  closeProfileMenu(): void {
    this.profileMenuOpen.set(false);
  }

  @HostListener('document:click', ['$event'])
  onDocumentClick(event: MouseEvent): void {
    const target = event.target;
    if (target instanceof Node && (event.composedPath().some((node) => (
      node instanceof HTMLElement
      && (node.id === 'profile-menu' || node.classList.contains('profile-button'))
    )))) {
      return;
    }
    this.closeProfileMenu();
  }

  @HostListener('document:keydown.escape')
  onEscape(): void {
    this.closeProfileMenu();
  }

  goCatalog(): void {
    this.closeProfileMenu();
    void this.router.navigate(['/seller/catalog']);
  }

  goUsersManagement(): void {
    this.closeProfileMenu();
    void this.router.navigate(['/admin/usuarios']);
  }

  goToProfile(): void {
    this.closeProfileMenu();

    const role = this.authService.currentUser()?.role;

    if (role === 'SELLER') {
      void this.router.navigate(['/seller/profile']);
    } else if (role === 'CLIENT') {
      void this.router.navigate(['/client/profile']);
    } else if (role === 'ADMIN') {
      void this.router.navigate(['/admin/profile']);
    }
  }

  logout(): void {
    this.closeProfileMenu();
    this.logoutError.set(null);
    this.authService.logout().subscribe({
      next: () => {
        void this.router.navigate(['/']);
      },
      error: (error: unknown) => {
        if (error instanceof HttpErrorResponse && error.status === 401) {
          this.authService.clearCurrentUser();
          void this.router.navigate(['/'], { queryParams: { expired: 'true' } });
          return;
        }
        this.logoutError.set('No se pudo cerrar la sesión. Inténtalo de nuevo.');
      },
    });
  }
}