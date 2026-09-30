import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AuthService, UserAdmin } from '../../../core/services/auth.service';

@Component({
  selector: 'app-admin-user-list',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './admin-user-list.html',
  styleUrls: ['./admin-user-list.css']
})
export class AdminUserList implements OnInit {
  usuarios = signal<UserAdmin[]>([]);
  cargando = signal(true);
  error = signal('');

  constructor(private readonly authService: AuthService) {}

  ngOnInit(): void {
    this.cargarUsuarios();
  }

  cargarUsuarios(): void {
    this.authService.obtenerTodosLosUsuarios().subscribe({
      next: (data) => {
        this.usuarios.set(data);
        this.cargando.set(false);
      },
      error: (err) => {
        this.error.set('Error al cargar los usuarios. Verifica tus permisos.');
        this.cargando.set(false);
        console.error(err);
      }
    });
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