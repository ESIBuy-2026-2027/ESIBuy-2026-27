import { ChangeDetectorRef, Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms'; // <-- IMPORTANTE: Añadido para los filtros
import { AuthService, UserAdmin } from '../../../core/services/auth.service';

@Component({
  selector: 'app-admin-user-list',
  standalone: true,
  imports: [CommonModule, RouterLink, FormsModule], // <-- Añadido aquí
  templateUrl: './admin-user-list.html',
  styleUrls: ['./admin-user-list.css']
})
export class AdminUserList implements OnInit {
  usuarios = signal<UserAdmin[]>([]);
  cargando = signal(true);
  error = signal('');

  filtroRol: string = '';
  filtroEstado: string = '';

  constructor(
    private readonly authService: AuthService,
    private readonly cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.cargarUsuarios();
  }

  cargarUsuarios(): void {
    this.cargando.set(true);
    this.error.set('');
    this.cdr.markForCheck();

    this.authService.obtenerTodosLosUsuarios(this.filtroRol, this.filtroEstado).subscribe({
      next: (data) => {
        this.usuarios.set(data);
        this.cargando.set(false);
      },
      error: (err: unknown) => {
        if (err instanceof HttpErrorResponse && err.status === 403) {
          this.error.set('No tienes permisos para consultar los usuarios.');
        } else if (err instanceof HttpErrorResponse && err.status === 401) {
          this.error.set('Tu sesión no es válida. Inicia sesión de nuevo.');
        } else {
          this.error.set('No se pudo cargar la lista de usuarios. Inténtalo de nuevo.');
        }
        this.cargando.set(false);
      }
    });
  }

  aplicarFiltros(): void {
    this.cargarUsuarios();
  }

  getAvatarUrl(profileImage: string | null | undefined): string {
    if (!profileImage) {
      return '/assets/images/avatars/av_all.jpeg';
    }

    if (/^https?:\/\//i.test(profileImage)) {
      return profileImage;
    }

    const imageName = profileImage
      .replace(/^\/?assets\/images\/avatars\//, '')
      .replace(/^\/+/, '');

    return `/assets/images/avatars/${imageName}`;
  }

  getRoleBadgeClass(role: string): string {
    switch (role) {
      case 'ADMIN': return 'badge-admin';
      case 'SELLER': return 'badge-seller';
      case 'CLIENT': return 'badge-client';
      default: return 'badge-default';
    }
  }
}