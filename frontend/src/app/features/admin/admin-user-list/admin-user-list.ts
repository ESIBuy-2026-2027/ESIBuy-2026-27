import { ChangeDetectorRef, Component, ElementRef, HostListener, OnInit, ViewChild, computed, signal } from '@angular/core';
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
  mensajeExito = signal('');
  usuarioPendiente = signal<UserAdmin | null>(null);
  pasoConfirmacion = signal<1 | 2>(1);
  eliminando = signal(false);
  errorEliminacion = signal('');
  actualizandoEstado = signal<string | null>(null);

  filtroRol: string = '';
  filtroEstado: string = '';
  busquedaNombre = signal('');
  usuariosFiltrados = computed(() => {
    const query = this.busquedaNombre().trim().toLocaleLowerCase();
    if (!query) {
      return this.usuarios();
    }

    return this.usuarios().filter((usuario) =>
      `${usuario.firstName} ${usuario.lastName}`.toLocaleLowerCase().includes(query));
  });
  private lastFocusedElement: HTMLElement | null = null;

  constructor(
    private readonly authService: AuthService,
    private readonly cdr: ChangeDetectorRef
  ) { }

  ngOnInit(): void {
    const stateMessage = history.state?.successMessage;
    if (stateMessage && typeof stateMessage === 'string') {
      this.mensajeExito.set(stateMessage);
    }
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

  @ViewChild('deleteDialog')
  set deleteDialog(element: ElementRef<HTMLElement> | undefined) {
    element?.nativeElement.focus();
  }

  puedeEliminar(usuario: UserAdmin): boolean {
    return this.authService.currentUser()?.id !== usuario.id;
  }

  puedeCambiarEstado(usuario: UserAdmin): boolean {
    return this.authService.currentUser()?.id !== usuario.id;
  }

  cambiarEstadoUsuario(usuario: UserAdmin, accion: 'activar' | 'desactivar' | 'bloquear' | 'desbloquear'): void {
    if (!this.puedeCambiarEstado(usuario) || this.actualizandoEstado()) {
      return;
    }

    this.actualizandoEstado.set(usuario.id);
    this.error.set('');
    this.mensajeExito.set('');

    let peticion;

    switch (accion) {
      case 'activar':
        peticion = this.authService.activarUsuarioAdmin(usuario.id);
        break;

      case 'desactivar':
        peticion = this.authService.desactivarUsuarioAdmin(usuario.id);
        break;

      case 'bloquear':
        peticion = this.authService.bloquearUsuarioAdmin(usuario.id);
        break;

      case 'desbloquear':
        peticion = this.authService.desbloquearUsuarioAdmin(usuario.id);
        break;
    }

    peticion.subscribe({
      next: () => {
        this.usuarios.update((usuarios) =>
          usuarios.map((actual) => {
            if (actual.id !== usuario.id) {
              return actual;
            }

            switch (accion) {
              case 'activar':
                return { ...actual, active: true, blocked: false };

              case 'desactivar':
                return { ...actual, active: false, blocked: false };

              case 'bloquear':
                return { ...actual, active: false, blocked: true };

              case 'desbloquear':
                return { ...actual, active: true, blocked: false };
            }
          }),
        );

        const mensajes = {
          activar: `La cuenta de ${usuario.email} se ha activado correctamente.`,
          desactivar: `La cuenta de ${usuario.email} se ha desactivado correctamente.`,
          bloquear: `La cuenta de ${usuario.email} se ha bloqueado correctamente.`,
          desbloquear: `La cuenta de ${usuario.email} se ha desbloqueado correctamente.`,
        };

        this.mensajeExito.set(mensajes[accion]);
        this.actualizandoEstado.set(null);
      },

      error: () => {
        this.error.set('No se pudo cambiar el estado del usuario. Inténtalo de nuevo.');
        this.actualizandoEstado.set(null);
      },
    });
  }

  getEstadoUsuario(usuario: UserAdmin): 'activo' | 'inactivo' | 'bloqueado' {
    if (usuario.blocked) {
      return 'bloqueado';
    }
    return usuario.active ? 'activo' : 'inactivo';
  }

  getTextoEstado(usuario: UserAdmin): string {
    switch (this.getEstadoUsuario(usuario)) {
      case 'activo':
        return 'Activo';
      case 'inactivo':
        return 'Inactivo';
      case 'bloqueado':
        return 'Bloqueado';
    }
  }

  getClaseEstado(usuario: UserAdmin): string {
    switch (this.getEstadoUsuario(usuario)) {
      case 'activo':
        return 'active';
      case 'inactivo':
        return 'inactive';
      case 'bloqueado':
        return 'blocked';
    }
  }

  abrirConfirmacion(usuario: UserAdmin): void {
    if (!this.puedeEliminar(usuario)) {
      return;
    }
    this.lastFocusedElement = document.activeElement instanceof HTMLElement ? document.activeElement : null;
    this.usuarioPendiente.set(usuario);
    this.pasoConfirmacion.set(1);
    this.errorEliminacion.set('');
    this.mensajeExito.set('');
  }

  confirmarPrimerPaso(): void {
    this.pasoConfirmacion.set(2);
  }

  cancelarConfirmacion(): void {
    if (!this.eliminando()) {
      this.usuarioPendiente.set(null);
      this.pasoConfirmacion.set(1);
      this.errorEliminacion.set('');
      const focusTarget = this.lastFocusedElement;
      this.lastFocusedElement = null;
      queueMicrotask(() => {
        if (focusTarget?.isConnected) {
          focusTarget.focus();
        }
      });
    }
  }

  eliminarUsuario(): void {
    const usuario = this.usuarioPendiente();
    if (!usuario || !this.puedeEliminar(usuario) || this.eliminando()) {
      return;
    }

    this.eliminando.set(true);
    this.errorEliminacion.set('');
    this.authService.eliminarUsuarioAdmin(usuario.id).subscribe({
      next: () => {
        this.usuarios.update((usuarios) => usuarios.filter((actual) => actual.id !== usuario.id));
        this.mensajeExito.set(`La cuenta de ${usuario.email} se eliminó correctamente.`);
        this.usuarioPendiente.set(null);
        this.eliminando.set(false);
      },
      error: (err: unknown) => {
        this.errorEliminacion.set(this.mensajeErrorEliminacion(err));
        this.eliminando.set(false);
      }
    });
  }

  @HostListener('document:keydown', ['$event'])
  gestionarTecladoDialogo(event: KeyboardEvent): void {
    if (!this.usuarioPendiente()) {
      return;
    }
    if (event.key === 'Escape') {
      event.preventDefault();
      this.cancelarConfirmacion();
      return;
    }
    if (event.key !== 'Tab') {
      return;
    }

    const dialog = document.querySelector<HTMLElement>('.delete-dialog');
    const focusable = dialog?.querySelectorAll<HTMLElement>('button:not(:disabled)');
    if (!focusable?.length) {
      event.preventDefault();
      dialog?.focus();
      return;
    }
    const first = focusable[0];
    const last = focusable[focusable.length - 1];
    if (event.shiftKey && (document.activeElement === first || document.activeElement === dialog)) {
      event.preventDefault();
      last.focus();
    } else if (!event.shiftKey && (document.activeElement === last || document.activeElement === dialog)) {
      event.preventDefault();
      first.focus();
    }
  }

  private mensajeErrorEliminacion(error: unknown): string {
    if (error instanceof HttpErrorResponse) {
      switch (error.status) {
        case 400: return 'La solicitud no es válida. No puedes eliminar tu propia cuenta.';
        case 401: return 'Tu sesión ha caducado. Inicia sesión de nuevo.';
        case 403: return 'No tienes permisos para eliminar usuarios.';
        case 404: return 'El usuario ya no existe. Actualiza el listado.';
        case 409: return 'No se puede eliminar este vendedor mientras tenga productos asociados.';
        default: break;
      }
    }
    return 'No se pudo eliminar el usuario. Inténtalo de nuevo.';
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