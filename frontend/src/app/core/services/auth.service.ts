import { HttpClient, HttpErrorResponse, HttpParams } from '@angular/common/http';
import { computed, Injectable, signal } from '@angular/core';
import { Observable, catchError, of, tap, throwError } from 'rxjs';


export type UserRole = 'CLIENT' | 'SELLER' | 'ADMIN';

export interface AuthenticatedUser {
  id: string;
  email: string;
  role: UserRole;
  active: boolean;
}

export interface UserAdmin {
  id: string;
  email: string;
  firstName: string;
  lastName: string;
  nationalId: string | null;
  phone: string | null;
  profileImage: string | null;
  role: UserRole;
  active: boolean;
  blocked: boolean;
  createdAt: string | null;
  clientProfile?: { birthDate?: string; customerType?: string } | null;
  sellerProfile?: { businessName?: string; defaultCategory?: string; sede?: string } | null;
  adminProfile?: { headquarters?: string; hireDate?: string } | null;
}

export interface UpdateUserRequest {
  firstName: string;
  lastName: string;
  phone: string;
}

export interface UserResponse {
  id: string;
  email: string;
  firstName: string;
  lastName: string;
  phone: string | null;
  role: string;
}


export interface AltaAdminRequest {
  dni: string;
  nombre: string;
  apellidos: string;
  email: string;
  contrasena: string;
  repetirContrasena: string;
  sede?: string | null;
  imagenPerfil?: string | null;
}

export interface ApiResponse<T> {
  success: boolean;
  message: string;
  data: T;
  correlationId: string | null;
}


@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly apiUrl = '/api/users';
  private readonly currentUserSignal = signal<AuthenticatedUser | null>(null);
  readonly currentUser = this.currentUserSignal.asReadonly();
  readonly isAuthenticated = computed(() => this.currentUserSignal() !== null);

  constructor(private readonly http: HttpClient) { }

  registerClient(data: unknown): Observable<unknown> {
    return this.http.post(`${this.apiUrl}/register/client`, data, { withCredentials: true });
  }

  registerSeller(data: unknown): Observable<unknown> {
    return this.http.post(`${this.apiUrl}/register/seller`, data, { withCredentials: true });
  }

  adminRegisterClient(data: unknown): Observable<unknown> {
    return this.http.post('/api/v1/admin/users/register/client', data, { withCredentials: true });
  }

  adminRegisterSeller(data: unknown): Observable<unknown> {
    return this.http.post('/api/v1/admin/users/register/seller', data, { withCredentials: true });
  }

  createAdmin(data: unknown): Observable<unknown> {
    return this.http.post(`${this.apiUrl}/admin`, data, { withCredentials: true });
  }

  login(data: { email: string; password: string }): Observable<AuthenticatedUser> {
    return this.http.post<AuthenticatedUser>(`${this.apiUrl}/login`, data, { withCredentials: true })
      .pipe(tap((user) => this.currentUserSignal.set(user)));
  }

  me(): Observable<AuthenticatedUser> {
    return this.http.get<AuthenticatedUser>(`${this.apiUrl}/me`, { withCredentials: true })
      .pipe(tap((user) => this.currentUserSignal.set(user)));
  }

  loadCurrentUser(): Observable<AuthenticatedUser | null> {
    return this.me().pipe(
      catchError((error: unknown) => {
        if (error instanceof HttpErrorResponse && error.status === 401) {
          this.clearCurrentUser();
          return of(null);
        }
        return throwError(() => error);
      }),
    );
  }

  logout(): Observable<void> {
    return this.http.post<void>(`${this.apiUrl}/logout`, {}, { withCredentials: true })
      .pipe(tap(() => this.clearCurrentUser()));
  }

  clearCurrentUser(): void {
    this.currentUserSignal.set(null);
  }




  obtenerTodosLosUsuarios(rol?: string, estado?: string): Observable<UserAdmin[]> {
    let params = new HttpParams();

    if (rol) {
      params = params.set('rol', rol);
    }
    if (estado) {
      params = params.set('estado', estado);
    }

    return this.http.get<UserAdmin[]>('/api/admin/usuarios', {
      params,
      withCredentials: true,
    });
  }

  obtenerUsuarioAdmin(id: string): Observable<ApiResponse<UserResponse>> {
    return this.http.get<ApiResponse<UserResponse>>(
      `/api/v1/admin/users/${encodeURIComponent(id)}`,
      { withCredentials: true },
    );
  }

  actualizarUsuarioAdmin(id: string, data: UpdateUserRequest): Observable<ApiResponse<UserResponse>> {
    return this.http.put<ApiResponse<UserResponse>>(
      `/api/v1/admin/users/${encodeURIComponent(id)}`,
      data,
      { withCredentials: true },
    );
  }

  eliminarUsuarioAdmin(id: string): Observable<ApiResponse<void>> {
    return this.http.delete<ApiResponse<void>>(
      `/api/admin/usuarios/${encodeURIComponent(id)}`,
      { withCredentials: true },
    );
  }

  alternarEstadoUsuarioAdmin(id: string): Observable<ApiResponse<UserResponse>> {
    return this.http.put<ApiResponse<UserResponse>>(
      `/api/admin/usuarios/${encodeURIComponent(id)}/estado`,
      {},
      { withCredentials: true },
    );
  }

  bloquearUsuarioAdmin(id: string): Observable<ApiResponse<void>> {
    return this.http.post<ApiResponse<void>>(
      `/api/admin/usuarios/${encodeURIComponent(id)}/bloquear`,
      {},
      { withCredentials: true },
    );
  }

  desbloquearUsuarioAdmin(id: string): Observable<ApiResponse<void>> {
    return this.http.post<ApiResponse<void>>(
      `/api/admin/usuarios/${encodeURIComponent(id)}/desbloquear`,
      {},
      { withCredentials: true },
    );
  }

  crearAdministrador(data: AltaAdminRequest): Observable<any> {
    return this.http.post<any>('/api/admin/alta', data, { withCredentials: true });
  }

  activarUsuarioAdmin(id: string): Observable<ApiResponse<void>> {
    return this.http.post<ApiResponse<void>>(
      `/api/admin/usuarios/${encodeURIComponent(id)}/activar`,
      {},
      { withCredentials: true },
    );
  }

  desactivarUsuarioAdmin(id: string): Observable<ApiResponse<void>> {
    return this.http.post<ApiResponse<void>>(
      `/api/admin/usuarios/${encodeURIComponent(id)}/desactivar`,
      {},
      { withCredentials: true },
    );
  }

}
