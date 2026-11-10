import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { afterEach, beforeEach, describe, expect, it } from 'vitest';
import { AdminCategoryService } from './admin-category.service';

describe('AdminCategoryService', () => {
  let service: AdminCategoryService;
  let httpTesting: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(AdminCategoryService);
    httpTesting = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpTesting.verify());

  it('lists categories using the admin endpoint and credentials', () => {
    service.getAll().subscribe();

    const request = httpTesting.expectOne('/api/admin/categorias');
    expect(request.request.method).toBe('GET');
    expect(request.request.withCredentials).toBe(true);
    request.flush([]);
  });

  it('creates and updates categories with credentials', () => {
    service.create('Papelería').subscribe();
    const createRequest = httpTesting.expectOne('/api/admin/categorias');
    expect(createRequest.request.method).toBe('POST');
    expect(createRequest.request.body).toEqual({ name: 'Papelería' });
    expect(createRequest.request.withCredentials).toBe(true);
    createRequest.flush({ id: 'c-1', name: 'Papelería' });

    service.update('c-1', 'Oficina').subscribe();
    const updateRequest = httpTesting.expectOne('/api/admin/categorias/c-1');
    expect(updateRequest.request.method).toBe('PUT');
    expect(updateRequest.request.body).toEqual({ name: 'Oficina' });
    expect(updateRequest.request.withCredentials).toBe(true);
    updateRequest.flush({ id: 'c-1', name: 'Oficina' });
  });

  it('deletes categories with credentials', () => {
    service.delete('c-1').subscribe();

    const request = httpTesting.expectOne('/api/admin/categorias/c-1');
    expect(request.request.method).toBe('DELETE');
    expect(request.request.withCredentials).toBe(true);
    request.flush(null);
  });
});
