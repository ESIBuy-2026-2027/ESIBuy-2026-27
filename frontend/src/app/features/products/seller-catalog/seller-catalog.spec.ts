import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { Observable, of, throwError } from 'rxjs';
import { SellerCatalogService, SellerProduct } from './seller-catalog.service';
import { SellerCatalog } from './seller-catalog';

interface CatalogSummary {
  totalProducts: number;
  outOfStockProducts: number;
  lowStockProducts: number;
  lowStockThreshold: number;
}

describe('SellerCatalog', () => {
  let fixture: ComponentFixture<SellerCatalog>;
  const summary: CatalogSummary = {
    totalProducts: 12,
    outOfStockProducts: 2,
    lowStockProducts: 3,
    lowStockThreshold: 5,
  };
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
  let summaryResponse: Observable<CatalogSummary>;
  let productsResponse: Observable<SellerProduct[]>;
  let catalogService: {
    getSummary: () => Observable<CatalogSummary>;
    getProducts: () => Observable<SellerProduct[]>;
  };

  beforeEach(async () => {
    summaryResponse = of(summary);
    productsResponse = of(products);
    catalogService = {
      getSummary: () => summaryResponse,
      getProducts: () => productsResponse,
    };
    await TestBed.configureTestingModule({
      imports: [SellerCatalog],
      providers: [
        provideRouter([]),
        { provide: SellerCatalogService, useValue: catalogService },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(SellerCatalog);
    await fixture.whenStable();
  });

  it('should create the seller catalog page', () => {
    expect(fixture.componentInstance).toBeTruthy();
  });

  it('should render the catalog layout and all summary metrics', () => {
    const element = fixture.nativeElement as HTMLElement;

    expect(element.querySelector('h1')?.textContent).toContain('Catálogo');
    expect(element.querySelector('[aria-label="Resumen del catálogo"]')).not.toBeNull();
    expect(element.querySelector('[aria-label="Productos del catálogo"]')).not.toBeNull();
    expect(element.textContent).toContain('12');
    expect(element.textContent).toContain('5 avisos');
    expect(element.textContent).toContain('Catálogo activo');
    expect(element.textContent).toContain('Auriculares');
    expect(element.textContent).toContain('Stock bajo · 3');
    expect(element.textContent).toContain('Publicado');
  });

  it('should link to the product creation route', () => {
    const element = fixture.nativeElement as HTMLElement;
    const createLinks = element.querySelectorAll('a[href="/products/new"]');

    expect(createLinks.length).toBeGreaterThan(0);
  });

  it('should show an error and offer a retry when metrics cannot be loaded', async () => {
    fixture.destroy();
    summaryResponse = throwError(() => new Error('API unavailable'));

    fixture = TestBed.createComponent(SellerCatalog);
    fixture.detectChanges();
    await fixture.whenStable();

    const element = fixture.nativeElement as HTMLElement;
    expect(element.textContent).toContain('No se pudieron cargar las métricas');
    expect(element.querySelector('button[aria-label="Reintentar carga de métricas"]')).not.toBeNull();
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
});
