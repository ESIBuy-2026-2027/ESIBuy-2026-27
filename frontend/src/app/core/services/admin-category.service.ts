import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { Category } from './products.service';

@Injectable({ providedIn: 'root' })
export class AdminCategoryService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = '/api/admin/categorias';

  getAll(): Observable<Category[]> {
    return this.http.get<Category[]>(this.apiUrl, { withCredentials: true });
  }

  create(name: string): Observable<Category> {
    return this.http.post<Category>(this.apiUrl, { name }, { withCredentials: true });
  }

  update(id: string, name: string): Observable<Category> {
    return this.http.put<Category>(`${this.apiUrl}/${encodeURIComponent(id)}`, { name }, {
      withCredentials: true,
    });
  }

  delete(id: string): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${encodeURIComponent(id)}`, {
      withCredentials: true,
    });
  }
}
