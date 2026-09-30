import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ProductsService } from './products.service';

describe('ProductsService', () => {
  let service: ProductsService;
  let httpTesting: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(ProductsService);
    httpTesting = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpTesting.verify());

  it('should load available categories from the backend', () => {
    const categories = [{ id: 'category-1', name: 'Electrónica' }];

    service.getCategories().subscribe((result) => expect(result).toEqual(categories));

    const request = httpTesting.expectOne('/api/categorias');
    expect(request.request.method).toBe('GET');
    request.flush(categories);
  });

  it('should submit product data to the product creation endpoint', () => {
    const product = {
      name: 'Auriculares',
      categoryIds: ['category-1'],
      price: 19.99,
      available: true,
    };

    service.createProduct(product).subscribe((result) => expect(result.id).toBe('product-1'));

    const request = httpTesting.expectOne('/api/products');
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual(product);
    request.flush({ id: 'product-1' });
  });
});
