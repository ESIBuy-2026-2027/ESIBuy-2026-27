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
    imageUrl: 'https://example.com/headphones.jpg',
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

    expect(element.querySelector('h1')?.textContent?.trim()).toBe('Mi catálogo');
    expect(element.querySelector('[aria-label="Productos del catálogo"]')).not.toBeNull();
    expect(Array.from(element.querySelectorAll('.seller-navigation .navigation-link'), (link) =>
      link.querySelector('span:last-child')?.textContent?.trim()))
      .toEqual(['Mi catálogo', 'Mis pedidos', 'Estadísticas', 'Valoraciones']);
    expect(element.textContent).toContain('Auriculares');
    expect(element.querySelector('.stock-badge')?.textContent?.replace(/\s+/g, ' ').trim()).toBe('Stock bajo · 3');
    expect(element.textContent).toContain('Publicado');
    expect(element.querySelector('thead')?.textContent).toContain('Categoría');
    expect(element.textContent).toContain('Audio');
    expect(element.textContent).not.toContain('INVENTARIO');
    expect(element.querySelector('.product-table caption')?.textContent).toContain('Productos de mi catálogo');
    expect(element.querySelector('.catalog-controls .catalog-search')).not.toBeNull();
    expect(element.querySelector('.catalog-controls .product-count')?.textContent).toContain('1 productos');
    const filterButton = element.querySelector<HTMLButtonElement>('.catalog-filter-button');
    expect(filterButton?.textContent).toContain('Filtrar');
    expect(filterButton?.disabled).toBe(true);
    expect(filterButton?.getAttribute('aria-disabled')).toBe('true');
    expect(filterButton?.title).toBe('Próximamente');
    expect(element.querySelector<HTMLImageElement>('.product-image')?.alt).toBe('');
    expect(element.querySelector('input[type="search"]')?.hasAttribute('aria-label')).toBe(false);
    expect(element.querySelectorAll('.navigation-link[aria-disabled="true"]')).toHaveLength(3);
    expect(element.querySelectorAll('.navigation-link[title="Próximamente"]')).toHaveLength(3);

    const actionButtons = Array.from(element.querySelectorAll<HTMLButtonElement>('.icon-button'));
    expect(actionButtons).toHaveLength(2);
    expect(actionButtons.every((button) => button.disabled && button.getAttribute('aria-disabled') === 'true'))
      .toBe(true);
    expect(actionButtons.every((button) => button.title === 'Próximamente')).toBe(true);
  });

  it('should show the quantity alone for normal stock and keep labels for exceptional stock', () => {
    const component = fixture.componentInstance;
    const normalStockProduct = { ...products[0], stock: 100 };
    component.products.set([normalStockProduct]);
    fixture.detectChanges();

    const element = fixture.nativeElement as HTMLElement;
    expect(element.querySelector('.stock-badge')?.textContent?.trim()).toBe('100');
    expect(element.textContent).not.toContain('En stock');

    component.products.set([{ ...products[0], stock: 0 }]);
    fixture.detectChanges();
    expect(element.querySelector('.stock-badge')?.textContent?.replace(/\s+/g, ' ').trim()).toBe('Sin stock · 0');
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
