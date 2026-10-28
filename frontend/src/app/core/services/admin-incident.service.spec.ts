import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { afterEach, beforeEach, describe, expect, it } from 'vitest';
import { AdminIncidentService, CreateIncidentPayload, Incident } from './admin-incident.service';

describe('AdminIncidentService', () => {
  let service: AdminIncidentService;
  let httpTesting: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(AdminIncidentService);
    httpTesting = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpTesting.verify());

  it('creates an incident using the admin endpoint and credentials', () => {
    const payload: CreateIncidentPayload = {
      subject: 'Fallo al procesar pedido',
      category: 'PEDIDO',
      reference: 'PED-1234',
      description: 'El cliente no recibe confirmación',
    };

    const mockResponse: Incident = {
      id: 'inc-1',
      ...payload,
      status: 'PENDIENTE',
      createdBy: 'admin@esibuy.com',
      createdAt: '2026-10-28T10:00:00Z',
    };

    service.create(payload).subscribe((result) => {
      expect(result).toEqual(mockResponse);
    });

    const request = httpTesting.expectOne('/api/admin/incidents');
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual(payload);
    expect(request.request.withCredentials).toBe(true);
    request.flush(mockResponse);
  });

  it('lists incidents ordered from the admin endpoint with credentials', () => {
    service.getAll().subscribe((list) => {
      expect(list).toHaveLength(2);
      expect(list[0].id).toBe('inc-1');
    });

    const request = httpTesting.expectOne('/api/admin/incidents');
    expect(request.request.method).toBe('GET');
    expect(request.request.withCredentials).toBe(true);
    request.flush([
      { id: 'inc-1', subject: 'Incidencia 1', category: 'PEDIDO', description: 'Desc 1', status: 'PENDIENTE', createdBy: 'admin', createdAt: '2026-10-28T12:00:00Z' },
      { id: 'inc-2', subject: 'Incidencia 2', category: 'PAGO', description: 'Desc 2', status: 'RESUELTA', createdBy: 'admin', createdAt: '2026-10-28T10:00:00Z' },
    ]);
  });
});

