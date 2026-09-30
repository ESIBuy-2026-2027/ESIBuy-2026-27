import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { Observable, of, throwError } from 'rxjs';
import { SellerCatalogService } from './seller-catalog.service';
import { SellerCatalog } from './seller-catalog';

interface CatalogSummary {
  totalProductos: number;
  productosSinStock: number;
  productosStockBajo: number;
  umbralStockBajo: number;
}

describe('SellerCatalog', () => {
  let fixture: ComponentFixture<SellerCatalog>;
  const summary: CatalogSummary = {
    totalProductos: 12,
    productosSinStock: 2,
    productosStockBajo: 3,
    umbralStockBajo: 5,
  };
  let summaryResponse: Observable<CatalogSummary>;
  let catalogService: { getSummary: () => Observable<CatalogSummary> };

  beforeEach(async () => {
    summaryResponse = of(summary);
    catalogService = { getSummary: () => summaryResponse };
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
});
