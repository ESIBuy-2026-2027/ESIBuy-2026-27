import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

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

export interface ApiResponse<T> {
  success: boolean;
  message: string;
  data: T;
  correlationId: string | null;
}

@Injectable({
  providedIn: 'root'
})

export class AuthService {
  private readonly apiUrl = '/api/users';

  constructor(private readonly http: HttpClient) {}

  registerClient(data: any): Observable<any> {
    return this.http.post(`${this.apiUrl}/register/client`, data);
  }

  registerSeller(data: any): Observable<any> {
    return this.http.post(`${this.apiUrl}/register/seller`, data);
  }

  createAdmin(data: any): Observable<any> {
    return this.http.post(`${this.apiUrl}/admin`, data);
  }

  login(data: any): Observable<any> {
    return this.http.post(`${this.apiUrl}/login`, data);
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

  obtenerUsuarioAdmin(id: string): Observable<ApiResponse<UserResponse>> {
    return this.http.get<ApiResponse<UserResponse>>(`/api/v1/admin/users/${encodeURIComponent(id)}`);
  }

  actualizarUsuarioAdmin(id: string, data: UpdateUserRequest): Observable<ApiResponse<UserResponse>> {
    return this.http.put<ApiResponse<UserResponse>>(`/api/v1/admin/users/${encodeURIComponent(id)}`, data);
  }
}

