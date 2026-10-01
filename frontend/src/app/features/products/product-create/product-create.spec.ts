import { HttpErrorResponse } from '@angular/common/http';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { Observable, of, throwError } from 'rxjs';
import { Category, CreatedProduct, ProductCreateRequest, ProductsService } from '../products.service';
import { ProductCreate as ProductCreateComponent } from './product-create';

describe('ProductCreate', () => {
  const categories: Category[] = [
    { id: 'category-tech', name: 'Electrónica' },
    { id: 'category-home', name: 'Hogar' },
  ];
  let fixture: ComponentFixture<ProductCreateComponent>;
  let productService: Pick<ProductsService, 'getCategories' | 'createProduct'>;
  let createProduct: ReturnType<typeof vi.fn<(request: ProductCreateRequest) => Observable<CreatedProduct>>>;
  let categoriesResponse: Observable<Category[]>;

  beforeEach(async () => {
    categoriesResponse = of(categories);
    createProduct = vi.fn<(request: ProductCreateRequest) => Observable<CreatedProduct>>(
      () => of({ id: 'product-1' }),
    );
    productService = {
      getCategories: () => categoriesResponse,
      createProduct,
    };

    await TestBed.configureTestingModule({
      imports: [ProductCreateComponent],
      providers: [
        provideRouter([{ path: 'seller/catalog', children: [] }]),
        { provide: ProductsService, useValue: productService },
      ],
    }).compileComponents();
    fixture = TestBed.createComponent(ProductCreateComponent);
    fixture = TestBed.createComponent(ProductCreateComponent);
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();
  });

  it('should load existing categories and render every product field', () => {
    const element = fixture.nativeElement as HTMLElement;

    expect(element.querySelector('h1')?.textContent).toContain('Añadir producto');
    const options = element.querySelectorAll<HTMLOptionElement>('select[formControlName="categoriaIds"] option');
    expect(options.length).toBe(2);
    expect(options[0].textContent).toBe('Electrónica');
    fixture.componentInstance.form.controls.categoriaIds.setValue(['category-tech']);
    fixture.detectChanges();
    expect(options[0].selected).toBe(true);
    for (const field of [
      'nombre',
      'categoriaIds',
      'stock',
      'descripcion',
      'precio',
      'descuento',
      'descuentoPremium',
      'imagen',
      'disponible',
    ]) {
      expect(element.querySelector(`[formControlName="${field}"]`)).not.toBeNull();
    }
  });

  it('should show validation errors for required and invalid product fields', () => {
    fixture.componentInstance.form.setValue({
      nombre: '   ',
      categoriaIds: [],
      stock: -1,
      descripcion: 'x'.repeat(2001),
      precio: 0,
      descuento: 101,
      descuentoPremium: -1,
      imagen: 'not-a-url',
      disponible: null,
    });

    fixture.componentInstance.onSubmit();
    fixture.detectChanges();

    expect(fixture.componentInstance.form.invalid).toBe(true);
    expect(fixture.componentInstance.form.controls.nombre.touched).toBe(true);
    expect(fixture.componentInstance.form.controls.categoriaIds.touched).toBe(true);
    expect(fixture.componentInstance.form.controls.stock.hasError('min')).toBe(true);
    expect(fixture.componentInstance.form.controls.descripcion.hasError('maxlength')).toBe(true);
    expect(fixture.componentInstance.form.controls.precio.hasError('min')).toBe(true);
    expect(fixture.componentInstance.form.controls.descuento.hasError('max')).toBe(true);
    expect(fixture.componentInstance.form.controls.descuentoPremium.hasError('min')).toBe(true);
    expect(fixture.componentInstance.form.controls.imagen.hasError('pattern')).toBe(true);
    expect(fixture.componentInstance.form.controls.disponible.hasError('required')).toBe(true);
    expect(createProduct).not.toHaveBeenCalled();
  });

  it('should confirm product creation on the same page', async () => {
    fixture.componentInstance.form.setValue({
      nombre: '  Auriculares  ',
      categoriaIds: ['category-tech', 'category-home'],
      stock: 7,
      descripcion: 'Auriculares inalámbricos',
      precio: 19.99,
      descuento: null,
      descuentoPremium: 15,
      imagen: 'https://example.com/auriculares.png',
      disponible: true,
    });

    fixture.componentInstance.onSubmit();
    await fixture.whenStable();

    expect(createProduct).toHaveBeenCalledWith({
      name: 'Auriculares',
      categoryIds: ['category-tech', 'category-home'],
      stock: 7,
      description: 'Auriculares inalámbricos',
      price: 19.99,
      premiumDiscount: 15,
      imageUrl: 'https://example.com/auriculares.png',
      available: true,
    } satisfies ProductCreateRequest);
    expect(fixture.componentInstance.submitting()).toBe(false);
    expect(fixture.componentInstance.submitSuccess()).toBe(true);
    expect((fixture.nativeElement as HTMLElement).textContent).toContain('Producto registrado correctamente.');
  });

  it('should show all backend validation errors under their fields', () => {
    createProduct.mockReturnValue(throwError(() => new HttpErrorResponse({
      status: 400,
      error: {
        message: 'La solicitud contiene errores',
        errors: {
          nombre: 'El nombre no puede estar vacío.',
          precio: 'El precio debe ser mayor que 0.',
        },
      },
    })));
    fixture.componentInstance.form.setValue({
      nombre: 'Producto',
      categoriaIds: ['category-tech'],
      stock: null,
      descripcion: '',
      precio: 5,
      descuento: null,
      descuentoPremium: null,
      imagen: '',
      disponible: true,
    });

    fixture.componentInstance.onSubmit();
    fixture.detectChanges();

    const element = fixture.nativeElement as HTMLElement;
    expect(element.textContent).toContain('El nombre no puede estar vacío.');
    expect(element.textContent).toContain('El precio debe ser mayor que 0.');
    expect(fixture.componentInstance.serverFieldErrors).toEqual({
      nombre: 'El nombre no puede estar vacío.',
      precio: 'El precio debe ser mayor que 0.',
    });
  });

  it('should show the backend message when a selected category does not exist', () => {
    const message = 'No existen las categorías indicadas: category-unknown';
    createProduct.mockReturnValue(throwError(() => new HttpErrorResponse({
      status: 400,
      error: { message },
    })));
    fixture.componentInstance.form.setValue({
      nombre: 'Producto',
      categoriaIds: ['category-unknown'],
      stock: null,
      descripcion: '',
      precio: 5,
      descuento: null,
      descuentoPremium: null,
      imagen: '',
      disponible: true,
    });

    fixture.componentInstance.onSubmit();
    fixture.detectChanges();

    expect(fixture.componentInstance.submitError()).toBe(message);
    expect(fixture.componentInstance.serverFieldErrors.categoriaIds).toBe(message);
  });

  it('should omit blank optional fields when submitting', () => {
    fixture.componentInstance.form.setValue({
      nombre: 'Producto nuevo',
      categoriaIds: ['category-tech'],
      stock: null,
      descripcion: '',
      precio: 5,
      descuento: null,
      descuentoPremium: null,
      imagen: '',
      disponible: true,
    });

    fixture.componentInstance.onSubmit();

    expect(createProduct).toHaveBeenCalledWith({
      name: 'Producto nuevo',
      categoryIds: ['category-tech'],
      price: 5,
      available: true,
    } satisfies ProductCreateRequest);
  });

  it('should surface category loading failures instead of showing fake categories', () => {
    categoriesResponse = new Observable<Category[]>((subscriber) => {
      subscriber.error(new HttpErrorResponse({ status: 503 }));
    });
    fixture.destroy();

    const errorFixture = TestBed.createComponent(ProductCreateComponent);
    errorFixture.detectChanges();
    errorFixture.detectChanges();

    const element = errorFixture.nativeElement as HTMLElement;
    expect(element.querySelectorAll('select[formControlName="categoriaIds"] option').length).toBe(0);
    expect(element.querySelector<HTMLSelectElement>('select[formControlName="categoriaIds"]')?.disabled).toBe(true);
    expect(element.querySelector('select[formControlName="categoriaIds"]')?.getAttribute('size')).toBe('1');
    expect(element.textContent).toContain('No se pudieron cargar las categorías');
  });

  it('should explain when the backend has no categories available', async () => {
    fixture.destroy();
    categoriesResponse = of([]);

    const emptyFixture = TestBed.createComponent(ProductCreateComponent);
    emptyFixture.detectChanges();
    await emptyFixture.whenStable();
    emptyFixture.detectChanges();

    const element = emptyFixture.nativeElement as HTMLElement;
    expect(element.textContent).toContain('No hay categorías disponibles en este momento.');
    expect(element.querySelector<HTMLSelectElement>('select[formControlName="categoriaIds"]')?.disabled).toBe(true);
    expect(element.querySelector<HTMLButtonElement>('button[type="submit"]')?.disabled).toBe(true);
    emptyFixture.componentInstance.onSubmit();
    expect(createProduct).not.toHaveBeenCalled();
  });
});
