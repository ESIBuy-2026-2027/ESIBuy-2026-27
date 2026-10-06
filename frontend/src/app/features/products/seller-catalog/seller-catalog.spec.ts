import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { Observable, of, throwError } from 'rxjs';
import { ProductsService } from '../products.service';
import { SellerCatalogService, SellerProduct } from './seller-catalog.service';
import { SellerCatalog } from './seller-catalog';

describe('SellerCatalog', () => {
  let fixture: ComponentFixture<SellerCatalog>;
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
  let productsResponse: Observable<SellerProduct[]>;
  let categoriesResponse: Observable<{ id: string; name: string }[]>;
  let catalogService: {
    getProducts: () => Observable<SellerProduct[]>;
  };

  beforeEach(async () => {
    productsResponse = of(products);
    categoriesResponse = of([{ id: 'category-1', name: 'Audio' }]);
    catalogService = {
      getProducts: () => productsResponse,
    };
    await TestBed.configureTestingModule({
      imports: [SellerCatalog],
      providers: [
        provideRouter([]),
        { provide: SellerCatalogService, useValue: catalogService },
        { provide: ProductsService, useValue: { getCategories: () => categoriesResponse } },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(SellerCatalog);
    await fixture.whenStable();
  });

  it('should create the seller catalog page', () => {
    expect(fixture.componentInstance).toBeTruthy();
  });

  it('should render the seller product catalog and category names', () => {
    const element = fixture.nativeElement as HTMLElement;

    expect(element.querySelector('h1')?.textContent).toContain('Catálogo');
    expect(element.querySelector('[aria-label="Productos del catálogo"]')).not.toBeNull();
    expect(element.textContent).toContain('Auriculares');
    expect(element.textContent).toContain('Stock bajo · 3');
    expect(element.textContent).toContain('Publicado');
    expect(element.querySelector('thead')?.textContent).toContain('Categoría');
    expect(element.textContent).toContain('Audio');
  });

  it('should link to the product creation route', () => {
    const element = fixture.nativeElement as HTMLElement;
    const createLinks = element.querySelectorAll('a[href="/products/new"]');

    expect(createLinks.length).toBeGreaterThan(0);
  });

  it('should show an empty state when the seller has no products', async () => {
    fixture.destroy();
    productsResponse = of([]);

    fixture = TestBed.createComponent(SellerCatalog);
    fixture.detectChanges();
    await fixture.whenStable();

    expect((fixture.nativeElement as HTMLElement).textContent).toContain('Aún no tienes productos');
  });

  it('should show a retry option when the product list cannot be loaded', async () => {
    fixture.destroy();
    productsResponse = throwError(() => new Error('API unavailable'));

    fixture = TestBed.createComponent(SellerCatalog);
    fixture.detectChanges();
    await fixture.whenStable();

    const element = fixture.nativeElement as HTMLElement;
    expect(element.textContent).toContain('No se pudieron cargar tus productos');
    expect(element.querySelector('.catalog-error button')).not.toBeNull();
  });

  it('should show a retry option when category names cannot be loaded', async () => {
    fixture.destroy();
    categoriesResponse = throwError(() => new Error('API unavailable'));

    fixture = TestBed.createComponent(SellerCatalog);
    fixture.detectChanges();
    await fixture.whenStable();

    const element = fixture.nativeElement as HTMLElement;
    expect(element.textContent).toContain('No se pudieron cargar los nombres de las categorías');
    expect(element.textContent).toContain('No disponible');
  });
});
