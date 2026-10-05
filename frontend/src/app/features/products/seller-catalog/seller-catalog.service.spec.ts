import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { SellerCatalogService, SellerProduct } from './seller-catalog.service';

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
      totalProducts: 12,
      outOfStockProducts: 2,
      lowStockProducts: 3,
      lowStockThreshold: 5,
    };

    service.getSummary().subscribe((result) => expect(result).toEqual(summary));

    const request = httpTesting.expectOne('/api/products/resumen');
    expect(request.request.method).toBe('GET');
    request.flush(summary);
  });

  it('should request only the authenticated seller products', () => {
    const products: SellerProduct[] = [{
      id: 'product-1',
      name: 'Auriculares',
      categoryIds: ['category-1'],
      stock: 3,
      description: 'Auriculares inalámbricos',
      price: 19.99,
      discount: null,
      premiumDiscount: null,
      imageUrl: null,
      available: true,
      sellerId: 'seller-1',
    }];

    service.getProducts().subscribe((result) => expect(result).toEqual(products));

    const request = httpTesting.expectOne('/api/products/mine');
    expect(request.request.method).toBe('GET');
    request.flush(products);
  });
});
