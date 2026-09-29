import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private apiUrl = '/api/users';

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
}
