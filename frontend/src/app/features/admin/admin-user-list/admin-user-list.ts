import { ChangeDetectorRef, Component, ElementRef, HostListener, OnInit, ViewChild, computed, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { FormsModule } from '@angular/forms'; // <-- IMPORTANTE: Añadido para los filtros
import { AuthService, UserAdmin } from '../../../core/services/auth.service';

@Component({
  selector: 'app-admin-user-list',
  standalone: true,
  imports: [CommonModule, RouterLink, RouterLinkActive, FormsModule],
  templateUrl: './admin-user-list.html',
  styleUrls: ['./admin-user-list.css']
})
export class AdminUserList implements OnInit {
  sidebarCollapsed = signal(false);
  users = signal<UserAdmin[]>([]);
  loading = signal(true);
  error = signal('');
  successMessage = signal('');
  pendingUser = signal<UserAdmin | null>(null);
  confirmationStep = signal<1 | 2>(1);
  deleting = signal(false);
  deletionError = signal('');
  updatingStatus = signal<string | null>(null);

  roleFilter: string = '';
  statusFilter: string = '';
  nameSearch = signal('');
  filteredUsers = computed(() => {
    const query = this.nameSearch().trim().toLocaleLowerCase();
    if (!query) {
      return this.users();
    }

    return this.users().filter((user) =>
      `${user.firstName} ${user.lastName}`.toLocaleLowerCase().includes(query));
  });
  private lastFocusedElement: HTMLElement | null = null;

  constructor(
    private readonly authService: AuthService,
    private readonly cdr: ChangeDetectorRef
  ) { }

  ngOnInit(): void {
    const stateMessage = history.state?.successMessage;
    if (stateMessage && typeof stateMessage === 'string') {
      this.successMessage.set(stateMessage);
    }
    this.loadUsers();
  }

  loadUsers(): void {
    this.loading.set(true);
    this.error.set('');
    this.cdr.markForCheck();

    this.authService.getAllUsers(this.roleFilter, this.statusFilter).subscribe({
      next: (data) => {
        this.users.set(data);
        this.loading.set(false);
      },
      error: (err: unknown) => {
        if (err instanceof HttpErrorResponse && err.status === 403) {
          this.error.set('No tienes permisos para consultar los usuarios.');
        } else if (err instanceof HttpErrorResponse && err.status === 401) {
          this.error.set('Tu sesión no es válida. Inicia sesión de nuevo.');
        } else {
          this.error.set('No se pudo cargar la lista de usuarios. Inténtalo de nuevo.');
        }
        this.loading.set(false);
      }
    });
  }

  applyFilters(): void {
    this.loadUsers();
  }

  @ViewChild('deleteDialog')
  set deleteDialog(element: ElementRef<HTMLElement> | undefined) {
    element?.nativeElement.focus();
  }

  canDelete(user: UserAdmin): boolean {
    return user.role === 'ADMIN' && this.authService.currentUser()?.id !== user.id;
  }

  canChangeStatus(user: UserAdmin): boolean {
    return this.authService.currentUser()?.id !== user.id;
  }

  changeUserStatus(user: UserAdmin, action: 'activar' | 'desactivar' | 'bloquear' | 'desbloquear'): void {
    if (!this.canChangeStatus(user) || this.updatingStatus()) {
      return;
    }

    this.updatingStatus.set(user.id);
    this.error.set('');
    this.successMessage.set('');

    let request;

    switch (action) {
      case 'activar':
        request = this.authService.activateAdminUser(user.id);
        break;

      case 'desactivar':
        request = this.authService.deactivateAdminUser(user.id);
        break;

      case 'bloquear':
        request = this.authService.blockAdminUser(user.id);
        break;

      case 'desbloquear':
        request = this.authService.unblockAdminUser(user.id);
        break;
    }

    request.subscribe({
      next: () => {
        this.users.update((users) =>
          users.map((actual) => {
            if (actual.id !== user.id) {
              return actual;
            }

            switch (action) {
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

        const messages = {
          activar: `La cuenta de ${user.email} se ha activado correctamente.`,
          desactivar: `La cuenta de ${user.email} se ha desactivado correctamente.`,
          bloquear: `La cuenta de ${user.email} se ha bloqueado correctamente.`,
          desbloquear: `La cuenta de ${user.email} se ha desbloqueado correctamente.`,
        };

        this.successMessage.set(messages[action]);
        this.updatingStatus.set(null);
      },

      error: () => {
        this.error.set('No se pudo cambiar el estado del usuario. Inténtalo de nuevo.');
        this.updatingStatus.set(null);
      },
    });
  }

  getUserStatus(user: UserAdmin): 'activo' | 'inactivo' | 'bloqueado' {
    if (user.blocked) {
      return 'bloqueado';
    }
    return user.active ? 'activo' : 'inactivo';
  }

  getStatusText(user: UserAdmin): string {
    switch (this.getUserStatus(user)) {
      case 'activo':
        return 'Activo';
      case 'inactivo':
        return 'Inactivo';
      case 'bloqueado':
        return 'Bloqueado';
    }
  }

  getStatusClass(user: UserAdmin): string {
    switch (this.getUserStatus(user)) {
      case 'activo':
        return 'active';
      case 'inactivo':
        return 'inactive';
      case 'bloqueado':
        return 'blocked';
    }
  }

  openConfirmation(user: UserAdmin): void {
    if (!this.canDelete(user)) {
      return;
    }
    this.lastFocusedElement = document.activeElement instanceof HTMLElement ? document.activeElement : null;
    this.pendingUser.set(user);
    this.confirmationStep.set(1);
    this.deletionError.set('');
    this.successMessage.set('');
  }

  confirmFirstStep(): void {
    this.confirmationStep.set(2);
  }

  cancelConfirmation(): void {
    if (!this.deleting()) {
      this.pendingUser.set(null);
      this.confirmationStep.set(1);
      this.deletionError.set('');
      const focusTarget = this.lastFocusedElement;
      this.lastFocusedElement = null;
      queueMicrotask(() => {
        if (focusTarget?.isConnected) {
          focusTarget.focus();
        }
      });
    }
  }

  deleteUser(): void {
    const user = this.pendingUser();
    if (!user || !this.canDelete(user) || this.deleting()) {
      return;
    }

    this.deleting.set(true);
    this.deletionError.set('');
    this.authService.deleteAdminUser(user.id).subscribe({
      next: () => {
        this.users.update((users) => users.filter((currentUser) => currentUser.id !== user.id));
        this.successMessage.set(`La cuenta de ${user.email} se eliminó correctamente.`);
        this.pendingUser.set(null);
        this.deleting.set(false);
      },
      error: (err: unknown) => {
        this.deletionError.set(this.getDeletionErrorMessage(err));
        this.deleting.set(false);
      }
    });
  }

  @HostListener('document:keydown', ['$event'])
  handleDialogKeyboard(event: KeyboardEvent): void {
    if (!this.pendingUser()) {
      return;
    }
    if (event.key === 'Escape') {
      event.preventDefault();
      this.cancelConfirmation();
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

  private getDeletionErrorMessage(error: unknown): string {
    if (error instanceof HttpErrorResponse) {
      switch (error.status) {
        case 400: return 'La solicitud no es válida. No puedes eliminar tu propia cuenta.';
        case 401: return 'Tu sesión ha caducado. Inicia sesión de nuevo.';
        case 403: return 'No tienes permisos para eliminar usuarios.';
        case 404: return 'El usuario ya no existe. Actualiza el listado.';
        case 409: return 'Solo se pueden eliminar cuentas de otros administradores.';
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

  toggleSidebar(): void {
    this.sidebarCollapsed.update((collapsed) => !collapsed);
  }

}
