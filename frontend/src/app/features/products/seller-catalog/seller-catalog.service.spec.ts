import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { SellerCatalogService } from './seller-catalog.service';

describe('SellerCatalogService', () => {
  let service: SellerCatalogService;
  let httpTesting: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(SellerCatalogService);
    httpTesting = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpTesting.verify());

  it('should request the authenticated seller catalog summary', () => {
    const summary = {
      totalProductos: 12,
      productosSinStock: 2,
      productosStockBajo: 3,
      umbralStockBajo: 5,
    };

    service.getSummary().subscribe((result) => expect(result).toEqual(summary));

    const request = httpTesting.expectOne('/api/products/resumen');
    expect(request.request.method).toBe('GET');
    request.flush(summary);
  });
});
