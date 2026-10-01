import { HttpClient, HttpErrorResponse } from '@angular/common/http';
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
  nationalId: string;
  phone: string;
  profileImage: string;
  role: string;
  active: boolean;
  createdAt: string;
  clientProfile?: any;
  sellerProfile?: any;
  adminProfile?: any;
}
@Injectable({
  providedIn: 'root'
})

export class AuthService {
  private apiUrl = '/api/users';
  private readonly currentUserSignal = signal<AuthenticatedUser | null>(null);
  readonly currentUser = this.currentUserSignal.asReadonly();
  readonly isAuthenticated = computed(() => this.currentUserSignal() !== null);

  constructor(private http: HttpClient) {}

  registerClient(data: any): Observable<any> {
    return this.http.post(`${this.apiUrl}/register/client`, data);
  }

  registerSeller(data: any): Observable<any> {
    return this.http.post(`${this.apiUrl}/register/seller`, data);
  }

  createAdmin(data: any): Observable<any> {
    return this.http.post(`${this.apiUrl}/admin`, data);
  }

  login(data: { email: string; password: string }): Observable<AuthenticatedUser> {
    return this.http.post<AuthenticatedUser>(`${this.apiUrl}/login`, data, { withCredentials: true }).pipe(
      tap((user) => this.currentUserSignal.set(user))
    );
  }

  me(): Observable<AuthenticatedUser> {
    return this.http.get<AuthenticatedUser>(`${this.apiUrl}/me`, { withCredentials: true }).pipe(
      tap((user) => this.currentUserSignal.set(user))
    );
  }

  loadCurrentUser(): Observable<AuthenticatedUser | null> {
    return this.me().pipe(
      catchError((error: unknown) => {
        if (error instanceof HttpErrorResponse && error.status === 401) {
          this.clearCurrentUser();
          return of(null);
        }
        return throwError(() => error);
      })
    );
  }

  logout(): Observable<void> {
    return this.http.post<void>(`${this.apiUrl}/logout`, {}, { withCredentials: true }).pipe(
      tap(() => this.clearCurrentUser())
    );
  }

  clearCurrentUser(): void {
    this.currentUserSignal.set(null);
  }

  getCandidatos(): Observable<any[]> {
    return this.http.get<any[]>('/api/admin/candidatos');
  }

  promoverAAdmin(idUsuario: string, data: any): Observable<any> {
    return this.http.put<any>(`/api/admin/promover/${idUsuario}`, data);
  }
  obtenerTodosLosUsuarios(): Observable<UserAdmin[]> {
    return this.http.get<UserAdmin[]>('/api/admin/usuarios');
  }
}