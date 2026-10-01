import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
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

  obtenerTodosLosUsuarios(rol?: string, estado?: string): Observable<UserAdmin[]> {
    let params = new HttpParams();

    if (rol) {
      params = params.set('rol', rol);
    }
    if (estado) {
      params = params.set('estado', estado);
    }

    return this.http.get<UserAdmin[]>('/api/admin/usuarios', { params });
  }
}
