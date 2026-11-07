import { HttpClient, HttpParams } from '@angular/common/http';
import { computed, Injectable, signal } from '@angular/core';
import { Observable, catchError, of, tap, timeout } from 'rxjs';

export type UserRole = 'CLIENT' | 'SELLER' | 'ADMIN';
export type CustomerType = 'STANDARD' | 'PREMIUM';

export interface AuthenticatedUser {
  id: string;
  email: string;
  role: UserRole;
  active: boolean;
  totpEnabled?: boolean;
  customerType?: CustomerType;
  profileImage?: string | null;
}

export interface LoginResponse {
  requires2fa?: boolean;
  requires3fa?: boolean;
  expiresInSeconds?: number;
  email?: string;
  id?: string;
  role?: UserRole;
  active?: boolean;
  totpEnabled?: boolean;
  customerType?: CustomerType;
}

export interface ClientProfile {
  birthDate?: string;
  customerType?: CustomerType;
}

export interface SellerProfile {
  businessName?: string;
  defaultCategory?: string;
  headquarters?: string;
}

export interface AdminProfile {
  headquarters?: string;
  hireDate?: string;
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
  clientProfile?: ClientProfile | null;
  sellerProfile?: SellerProfile | null;
  adminProfile?: AdminProfile | null;
}

export interface UpdateUserRequest {
  firstName: string;
  lastName: string;
  nationalId?: string;
  phone?: string;
  profileImage?: string;
  headquarters?: string;
  businessName?: string;
  birthDate?: string;
  defaultCategory?: string;
}

export interface UserResponse {
  id: string;
  email: string;
  firstName: string;
  lastName: string;
  nationalId?: string | null;
  phone: string | null;
  role: string;
  profileImage?: string | null;
  headquarters?: string | null;
  businessName?: string | null;
  birthDate?: string | null;
  defaultCategory?: string | null;
}

export interface CreateAdminRequest {
  nationalId: string;
  firstName: string;
  lastName: string;
  email: string;
  password: string;
  confirmPassword: string;
  headquarters?: string | null;
  profileImage?: string | null;
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
  private readonly adminApiUrl = '/api/admin/users';
  private readonly currentUserSignal = signal<AuthenticatedUser | null>(null);

  readonly currentUser = this.currentUserSignal.asReadonly();
  readonly isAuthenticated = computed(() => this.currentUserSignal() !== null);

  constructor(private readonly http: HttpClient) { }

  registerClient(data: unknown): Observable<unknown> {
    return this.http.post(
      `${this.apiUrl}/register/client`,
      data,
      { withCredentials: true },
    );
  }

  registerSeller(data: unknown): Observable<unknown> {
    return this.http.post(
      `${this.apiUrl}/register/seller`,
      data,
      { withCredentials: true },
    );
  }


  createAdmin(data: CreateAdminRequest): Observable<unknown> {
    return this.http.post(
      '/api/admin/create-admin',
      data,
      { withCredentials: true },
    );
  }


  login(data: {
    email: string;
    password: string;
    totpCode?: string;
  }): Observable<LoginResponse> {
    return this.http
      .post<LoginResponse>(
        `${this.apiUrl}/login`,
        data,
        { withCredentials: true },
      )
      .pipe(
        tap((response) => {
          // No autenticar hasta completar los factores requeridos.
          if (
            !response.requires2fa &&
            !response.requires3fa &&
            response.id
          ) {
            this.currentUserSignal.set(response as AuthenticatedUser);
            this.refreshProfileImage();
          }
        }),
      );
  }

  requestPasswordReset(email: string) {
    return this.http.post<{ message: string }>(
      '/api/auth/password-reset/request',
      { email },
      { withCredentials: true }
    );
  }


  validatePasswordResetToken(token: string) {
    return this.http.post<{ message: string }>(
      '/api/auth/password-reset/validate',
      { token },
      { withCredentials: true }
    );
  }

  confirmPasswordReset(token: string, newPassword: string) {
    return this.http.post<{ message: string }>(
      '/api/auth/password-reset/confirm',
      { token, newPassword },
      { withCredentials: true }
    );
  }

  verifyLoginOtp(code: string): Observable<LoginResponse> {
    return this.http
      .post<LoginResponse>(
        `${this.apiUrl}/login/verify-otp`,
        { code },
        { withCredentials: true },
      )
      .pipe(
        tap((response) => {
          if (response.id && response.role) {
            this.currentUserSignal.set(response as AuthenticatedUser);
            this.refreshProfileImage();
          }
        }),
      );
  }

  setup2FA(): Observable<{ secret: string; qrUri: string }> {
    return this.http.post<{ secret: string; qrUri: string }>(
      `${this.apiUrl}/2fa/setup`,
      {},
      { withCredentials: true },
    );
  }

  enable2FA(
    code: string,
  ): Observable<{ message: string; backupCodes: string[] }> {
    return this.http.post<{ message: string; backupCodes: string[] }>(
      `${this.apiUrl}/2fa/enable`,
      { code },
      { withCredentials: true },
    );
  }

  regenerateBackupCodes(
    code?: string,
  ): Observable<{ message: string; backupCodes: string[] }> {
    return this.http.post<{ message: string; backupCodes: string[] }>(
      `${this.apiUrl}/2fa/regenerate-backup-codes`,
      { code },
      { withCredentials: true },
    );
  }

  disable2FA(code: string): Observable<{ message: string }> {
    return this.http.post<{ message: string }>(
      `${this.apiUrl}/2fa/disable`,
      { code },
      { withCredentials: true },
    );
  }

  me(): Observable<AuthenticatedUser> {
    return this.http
      .get<AuthenticatedUser>(
        `${this.apiUrl}/me`,
        { withCredentials: true },
      )
      .pipe(
        tap((user) => this.currentUserSignal.set(user)),
      );
  }

  loadCurrentUser(): Observable<AuthenticatedUser | null> {
    return this.me().pipe(
      tap(() => this.refreshProfileImage()),
      timeout(2000),
      catchError(() => {
        this.clearCurrentUser();
        return of(null);
      }),
    );
  }

  logout(): Observable<void> {
    return this.http
      .post<void>(
        `${this.apiUrl}/logout`,
        {},
        { withCredentials: true },
      )
      .pipe(
        tap(() => this.clearCurrentUser()),
      );
  }

  clearCurrentUser(): void {
    this.currentUserSignal.set(null);
  }

  getAllUsers(role?: string, status?: string): Observable<UserAdmin[]> {
    let params = new HttpParams();

    if (role) {
      params = params.set('role', role);
    }

    if (status) {
      params = params.set('status', status);
    }

    return this.http.get<UserAdmin[]>(this.adminApiUrl, {
      params,
      withCredentials: true,
    });
  }

  getAdminUser(id: string): Observable<ApiResponse<UserResponse>> {
    return this.http.get<ApiResponse<UserResponse>>(
      `${this.adminApiUrl}/${encodeURIComponent(id)}`,
      { withCredentials: true },
    );
  }

  updateAdminUser(
    id: string,
    data: UpdateUserRequest,
  ): Observable<ApiResponse<UserResponse>> {
    return this.http.put<ApiResponse<UserResponse>>(
      `${this.adminApiUrl}/${encodeURIComponent(id)}`,
      data,
      { withCredentials: true },
    );
  }

  deleteAdminUser(id: string): Observable<ApiResponse<void>> {
    return this.http.delete<ApiResponse<void>>(
      `${this.adminApiUrl}/${encodeURIComponent(id)}`,
      { withCredentials: true },
    );
  }

  toggleAdminUserStatus(
    id: string,
  ): Observable<ApiResponse<UserResponse>> {
    return this.http.put<ApiResponse<UserResponse>>(
      `${this.adminApiUrl}/${encodeURIComponent(id)}/status`,
      {},
      { withCredentials: true },
    );
  }

  blockAdminUser(id: string): Observable<ApiResponse<void>> {
    return this.http.post<ApiResponse<void>>(
      `${this.adminApiUrl}/${encodeURIComponent(id)}/block`,
      {},
      { withCredentials: true },
    );
  }

  unblockAdminUser(id: string): Observable<ApiResponse<void>> {
    return this.http.post<ApiResponse<void>>(
      `${this.adminApiUrl}/${encodeURIComponent(id)}/unblock`,
      {},
      { withCredentials: true },
    );
  }

  activateAdminUser(id: string): Observable<ApiResponse<void>> {
    return this.http.post<ApiResponse<void>>(
      `${this.adminApiUrl}/${encodeURIComponent(id)}/activate`,
      {},
      { withCredentials: true },
    );
  }

  deactivateAdminUser(id: string): Observable<ApiResponse<void>> {
    return this.http.post<ApiResponse<void>>(
      `${this.adminApiUrl}/${encodeURIComponent(id)}/deactivate`,
      {},
      { withCredentials: true },
    );
  }

  updateProfileImage(profileImage: string | null | undefined): void {
    const currentUser = this.currentUserSignal();

    if (!currentUser) {
      return;
    }

    this.currentUserSignal.set({
      ...currentUser,
      profileImage: profileImage?.trim() || null,
    });
  }

  private refreshProfileImage(): void {
    const userId = this.currentUserSignal()?.id;

    if (!userId) {
      return;
    }

    this.http.get<{ profileImage?: string | null }>(
      `${this.apiUrl}/me/profile`,
      { withCredentials: true },
    ).pipe(
      catchError(() => of(null)),
    ).subscribe((profile) => {
      const currentUser = this.currentUserSignal();

      if (profile && currentUser?.id === userId) {
        this.currentUserSignal.set({
          ...currentUser,
          profileImage: profile.profileImage ?? null,
        });
      }
    });
  }

}