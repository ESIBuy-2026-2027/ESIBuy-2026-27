import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { Observable, of, throwError } from 'rxjs';
import { ProductDetails, ProductsService } from '../../../core/services/products.service';
import { SellerCatalogService, SellerProduct } from '../../../core/services/seller-catalog.service';
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
  let updateProduct: ReturnType<typeof vi.fn<
    (id: string, request: Parameters<ProductsService['updateProduct']>[1]) => Observable<ProductDetails>
  >>;

  beforeEach(async () => {
    productsResponse = of(products);
    categoriesResponse = of([{ id: 'category-1', name: 'Audio' }]);
    catalogService = {
      getProducts: () => productsResponse,
    };
    updateProduct = vi.fn((id: string, request: Parameters<ProductsService['updateProduct']>[1]) =>
      of({
        ...products[0],
        id,
        name: request.name,
        categoryIds: request.categoryIds,
        stock: request.stock ?? products[0].stock,
        description: request.description ?? null,
        price: request.price,
        discount: request.discount ?? null,
        premiumDiscount: request.premiumDiscount ?? null,
        imageUrl: request.imageUrl ?? null,
        available: request.available,
        sellerId: products[0].sellerId,
      }));
    await TestBed.configureTestingModule({
      imports: [SellerCatalog],
      providers: [
        provideRouter([]),
        { provide: SellerCatalogService, useValue: catalogService },
        { provide: ProductsService, useValue: { getCategories: () => categoriesResponse, updateProduct } },
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
    expect(
      Array.from(
        element.querySelectorAll('.seller-navigation .navigation-link'),
        (link) => link.querySelector('span:last-child')?.textContent?.trim(),
      ),
    ).toEqual([
      'Catálogo',
      'Pedidos',
      'Estadísticas',
      'Valoraciones',
    ]);
    expect(element.textContent).toContain('Auriculares');
    expect(element.querySelector('.stock-badge')?.textContent?.replace(/\s+/g, ' ').trim()).toBe('Stock bajo · 3');
    expect(element.textContent).toContain('Publicado');
    expect(element.querySelector('thead')?.textContent).toContain('Categoría');
    expect(element.textContent).toContain('Audio');
    expect(element.textContent).not.toContain('INVENTARIO');
    expect(element.querySelector('.product-table caption')?.textContent).toContain('Productos de mi catálogo');
    expect(element.querySelector('.filter-bar .search-input-wrapper')).not.toBeNull();
    expect(element.querySelector('.filter-bar #seller-filter-category')).not.toBeNull();
    expect(element.querySelector('.filter-bar #seller-filter-visibility')).not.toBeNull();
    expect(element.querySelector('.filter-bar #seller-filter-sort')).not.toBeNull();
    expect(element.querySelector('.filter-bar .price-inputs')).not.toBeNull();
    expect(element.querySelector('.filter-product-count')?.textContent).toContain('1 producto');
    expect(element.querySelector('.reset-filter-btn')?.textContent).toContain('Limpiar filtros');
    expect(element.querySelector<HTMLImageElement>('.product-image')?.alt).toBe('');
    expect(element.querySelector('input[type="search"]')?.hasAttribute('aria-label')).toBe(true);
    expect(element.querySelectorAll('.navigation-link[aria-disabled="true"]')).toHaveLength(2);
    expect(element.querySelectorAll('.navigation-link[title="Próximamente"]')).toHaveLength(2);

    expect(element.querySelector<HTMLAnchorElement>('.edit-button')?.getAttribute('href'))
      .toBe('/products/product-1/edit');
    expect(element.querySelector('.delete-button')).toBeNull();
  });


  it('filters seller products by visibility and sorts by stock', () => {
    const component = fixture.componentInstance;
    const hiddenProduct: SellerProduct = {
      ...products[0],
      id: 'product-2',
      name: 'Teclado',
      stock: 0,
      available: false,
      price: 29.99,
    };
    component.products.set([...products, hiddenProduct]);

    component.selectedVisibility.set('hidden');
    expect(component.visibleProducts().map((product) => product.id)).toEqual(['product-2']);

    component.selectedVisibility.set('all');
    component.sortOrder.set('stock-asc');
    expect(component.visibleProducts().map((product) => product.id)).toEqual(['product-2', 'product-1']);

    component.clearFilters();
    expect(component.selectedVisibility()).toBe('all');
    expect(component.sortOrder()).toBe('recommended');
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

  it('should update price inline and replace the product after saving', async () => {
    const component = fixture.componentInstance;
    const element = fixture.nativeElement as HTMLElement;
    const priceButton = element.querySelector<HTMLButtonElement>('.price-value');

    priceButton?.click();
    fixture.detectChanges();
    const input = element.querySelector<HTMLInputElement>('.price-input');
    expect(input).not.toBeNull();
    input!.value = '24.5';
    input!.dispatchEvent(new Event('input'));
    fixture.detectChanges();
    element.querySelector<HTMLButtonElement>('.inline-save')?.click();
    await fixture.whenStable();
    fixture.detectChanges();

    expect(updateProduct).toHaveBeenCalledWith('product-1', expect.objectContaining({
      price: 24.5,
      stock: 3,
      categoryIds: ['category-1'],
    }));
    expect(component.products()[0].price).toBe(24.5);
    expect(element.querySelector('.price-value')?.textContent).toContain('24,50');
  });

  it('should reject negative inline stock without calling the API', () => {
    const component = fixture.componentInstance;
    component.startInlineEdit(products[0], 'stock');
    component.inlineValue.set(-1);
    component.saveInlineEdit(products[0]);

    expect(updateProduct).not.toHaveBeenCalled();
    expect(component.inlineError()).toBe('El stock debe ser un número entero no negativo.');
  });

  it('should toggle product visibility without removing it from the catalog', async () => {
    const component = fixture.componentInstance;
    const element = fixture.nativeElement as HTMLElement;

    element.querySelector<HTMLButtonElement>('.editable-availability')?.click();
    await fixture.whenStable();
    fixture.detectChanges();

    expect(updateProduct).toHaveBeenCalledWith('product-1', expect.objectContaining({
      available: false,
      price: 19.99,
      stock: 3,
    }));
    expect(component.products()[0].available).toBe(false);
    expect(element.querySelector('.editable-availability')?.textContent).toContain('Oculto');
    expect(element.textContent).toContain('Auriculares');
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