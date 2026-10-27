import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { afterEach, beforeEach, describe, expect, it } from 'vitest';
import {
  AdminProductService,
  AdminProductSummary,
  AdminProductDetail,
  ADMIN_READONLY_ERROR_MESSAGE,
} from './admin-product.service';

describe('AdminProductService', () => {
  let service: AdminProductService;
  let httpTesting: HttpTestingController;

  const mockProductList: AdminProductSummary[] = [
    {
      id: 'prod-1',
      name: 'Portátil Gaming',
      description: 'Potente portátil para jugar',
      price: 1299.99,
      stock: 5,
      category: 'Informática',
      sellerId: 'seller-1',
      sellerName: 'TechStore',
      active: true,
    },
    {
      id: 'prod-2',
      name: 'Ratón Inalámbrico',
      description: 'Ratón ergonómico',
      price: 29.99,
      stock: 0,
      category: 'Periféricos',
      sellerId: 'seller-2',
      sellerName: 'Periféricos SL',
      active: false,
    },
  ];

  const mockProductDetail: AdminProductDetail = {
    id: 'prod-1',
    name: 'Portátil Gaming',
    description: 'Potente portátil para jugar',
    price: 1299.99,
    stock: 5,
    category: 'Informática',
    sellerId: 'seller-1',
    sellerName: 'TechStore',
    active: true,
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(AdminProductService);
    httpTesting = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpTesting.verify());

  it('lists all products for supervision without filters', () => {
    let result: AdminProductSummary[] | undefined;
    service.getProducts().subscribe((data) => (result = data));

    const req = httpTesting.expectOne('/api/admin/products');
    expect(req.request.method).toBe('GET');
    expect(req.request.withCredentials).toBe(true);
    expect(req.request.params.keys().length).toBe(0);

    req.flush(mockProductList);
    expect(result).toEqual(mockProductList);
  });

  it('lists products applying seller, category and stock filters', () => {
    service
      .getProducts({
        sellerId: 'seller-1',
        categoryId: 'cat-1',
        inStock: true,
      })
      .subscribe();

    const req = httpTesting.expectOne(
      (r) =>
        r.url === '/api/admin/products' &&
        r.params.get('sellerId') === 'seller-1' &&
        r.params.get('categoryId') === 'cat-1' &&
        r.params.get('inStock') === 'true'
    );
    expect(req.request.method).toBe('GET');
    expect(req.request.withCredentials).toBe(true);
    req.flush(mockProductList);
  });

  it('retrieves detailed product info for admin supervision', () => {
    let result: AdminProductDetail | undefined;
    service.getProductById('prod-1').subscribe((data) => (result = data));

    const req = httpTesting.expectOne('/api/admin/products/prod-1');
    expect(req.request.method).toBe('GET');
    expect(req.request.withCredentials).toBe(true);

    req.flush(mockProductDetail);
    expect(result).toEqual(mockProductDetail);
  });

  it('issues delete request to verify write rejection from backend', () => {
    service.deleteProductAsAdmin('prod-1').subscribe();

    const req = httpTesting.expectOne('/api/products/prod-1');
    expect(req.request.method).toBe('DELETE');
    expect(req.request.withCredentials).toBe(true);
    req.flush(null);
  });

  it('exports user-facing error message constant for unauthorized admin writes', () => {
    expect(ADMIN_READONLY_ERROR_MESSAGE).toBe(
      'Operación no permitida: el perfil de administrador solo tiene permisos de lectura sobre productos.'
    );
  });
});

