import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, signal } from '@angular/core';
import { Router, RouterLink, RouterOutlet } from '@angular/router';
import { AuthService } from './core/services/auth.service';

@Component({
  imports: [RouterLink, RouterOutlet],
  standalone: true,
  selector: 'app-root',
  styleUrl: './app.css',
  templateUrl: './app.html',
})
export class App {
  readonly authService = inject(AuthService);
  private readonly router = inject(Router);
  readonly logoutError = signal<string | null>(null);

  logout(): void {
    this.logoutError.set(null);
    this.authService.logout().subscribe({
      next: () => this.router.navigate(['/']),
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
  
  goProfile(): void {
    //si es un vendedor que lleve al catalogo, si es un cliente a su perfil(no implementado) y si es administrador que lleve al administrador de usuarios
    if (this.authService.currentUser()?.role === 'ADMIN') {
      this.router.navigate(['/admin/usuarios'])
    } else if (this.authService.currentUser()?.role === 'CLIENT') {
      this.router.navigate(['/'])
    } else if (this.authService.currentUser()?.role === 'SELLER') {
    this.router.navigate(['/seller/catalog'])
    }
  }
}