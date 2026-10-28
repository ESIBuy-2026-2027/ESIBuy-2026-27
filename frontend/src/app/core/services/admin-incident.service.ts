import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

export type IncidentStatus = 'PENDIENTE' | 'RESUELTA';

export interface Incident {
  id: string;
  subject: string;
  category: string;
  reference?: string | null;
  description: string;
  status: IncidentStatus;
  createdBy: string;
  createdAt: string;
  resolvedAt?: string | null;
}

export interface CreateIncidentPayload {
  subject: string;
  category: string;
  reference?: string;
  description: string;
}

@Injectable({ providedIn: 'root' })
export class AdminIncidentService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = '/api/admin/incidents';

  create(payload: CreateIncidentPayload): Observable<Incident> {
    return this.http.post<Incident>(this.apiUrl, payload, { withCredentials: true });
  }

  getAll(): Observable<Incident[]> {
    return this.http.get<Incident[]>(this.apiUrl, { withCredentials: true });
  }

  resolve(id: string): Observable<Incident> {
    return this.http.patch<Incident>(
      `${this.apiUrl}/${encodeURIComponent(id)}/resolve`,
      {},
      { withCredentials: true }
    );
  }
}

