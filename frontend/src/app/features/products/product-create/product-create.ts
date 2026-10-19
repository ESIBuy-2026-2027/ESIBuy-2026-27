import { DestroyRef, Component, OnInit, inject, signal } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { ReactiveFormsModule, FormControl, FormGroup, Validators } from '@angular/forms';
import { ActivatedRoute, RouterLink, Router } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Category, ProductCreateRequest, ProductsService } from '../products.service';

interface ProductFormControls {
  nombre: FormControl<string>;
  categoriaIds: FormControl<string[]>;
  stock: FormControl<number | null>;
  descripcion: FormControl<string>;
  precio: FormControl<number | null>;
  descuento: FormControl<number | null>;
  descuentoPremium: FormControl<number | null>;
  imagen: FormControl<string>;
  disponible: FormControl<boolean | null>;
}

type ProductFormField = keyof ProductFormControls;

const SERVER_FIELD_TO_FORM_FIELD: Readonly<Partial<Record<string, ProductFormField>>> = {
  name: 'nombre',
  nombre: 'nombre',
  categoryIds: 'categoriaIds',
  categoriaIds: 'categoriaIds',
  stock: 'stock',
  description: 'descripcion',
  descripcion: 'descripcion',
  price: 'precio',
  precio: 'precio',
  discount: 'descuento',
  descuento: 'descuento',
  premiumDiscount: 'descuentoPremium',
  descuentoPremium: 'descuentoPremium',
  imageUrl: 'imagen',
  imagen: 'imagen',
  available: 'disponible',
  disponible: 'disponible',
};

@Component({
  selector: 'app-product-create',
  standalone: true,
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './product-create.html',
  styleUrl: './product-create.css',
})
export class ProductCreate implements OnInit {
  private readonly productsService = inject(ProductsService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  readonly productId = this.route.snapshot.paramMap.get('id');

  readonly form = new FormGroup<ProductFormControls>({
    nombre: new FormControl('', {
      nonNullable: true, validators: [
        Validators.required,
        Validators.pattern(/\S/),
        Validators.maxLength(150),
      ]
    }),
    categoriaIds: new FormControl<string[]>({ value: [], disabled: true }, {
      nonNullable: true,
      validators: [Validators.required],
    }),
    stock: new FormControl<number | null>(null, [
      Validators.min(0),
      Validators.pattern(/^\d+$/),
    ]),
    descripcion: new FormControl('', {
      nonNullable: true,
      validators: [Validators.maxLength(2000)],
    }),
    precio: new FormControl<number | null>(null, [
      Validators.required,
      Validators.min(Number.MIN_VALUE),
    ]),
    descuento: new FormControl<number | null>(null, [
      Validators.min(0),
      Validators.max(100),
    ]),
    descuentoPremium: new FormControl<number | null>(null, [
      Validators.min(0),
      Validators.max(100),
    ]),
    imagen: new FormControl('', {
      nonNullable: true,
      validators: [
        Validators.maxLength(2000),
        Validators.pattern(/^$|^https?:\/\/\S+$/i),
      ],
    }),
    disponible: new FormControl<boolean | null>(true, [Validators.required]),
  });

  categories: Category[] = [];
  categoriesLoading = true;
  categoriesError = false;
  submitting = signal(false);
  submitSuccess = signal(false);
  submitError = signal<string | null>(null);
  productLoading = signal(false);
  productLoadError = signal<string | null>(null);
  serverFieldErrors: Partial<Record<ProductFormField, string>> = {};

  imageError = signal(false);
  readonly sidebarCollapsed = signal(false);

  toggleSidebar(): void {
    this.sidebarCollapsed.update((collapsed) => !collapsed);
  }

  get previewUrl(): string {
    const control = this.form.controls.imagen;
    const value = (control.value ?? '').trim();
    return value && !control.hasError('pattern') ? value : '';
  }

  ngOnInit(): void {
    this.form.valueChanges.pipe(takeUntilDestroyed(this.destroyRef)).subscribe(() => {
      this.submitSuccess.set(false);
      this.serverFieldErrors = {};
      this.submitError.set(null);
    });

    this.productsService.getCategories().subscribe({
      next: (categories) => {
        this.categories = categories;
        this.categoriesLoading = false;
        if (categories.length > 0) {
          this.form.controls.categoriaIds.enable();
        }
      },
      error: () => {
        this.categoriesError = true;
        this.categoriesLoading = false;
      },
    });

    if (this.productId) {
      this.loadProduct();
    }
  }

  loadProduct(): void {
    if (!this.productId) {
      return;
    }
    this.productLoading.set(true);
    this.productLoadError.set(null);
    this.productsService.getProduct(this.productId).subscribe({
      next: (product) => {
        this.form.patchValue({
          nombre: product.name,
          categoriaIds: product.categoryIds,
          stock: product.stock,
          descripcion: product.description ?? '',
          precio: product.price,
          descuento: product.discount,
          descuentoPremium: product.premiumDiscount,
          imagen: product.imageUrl ?? '',
          disponible: product.available,
        });
        this.productLoading.set(false);
      },
      error: (error: unknown) => {
        this.productLoading.set(false);
        this.productLoadError.set(this.getLoadErrorMessage(error));
      },
    });
  }

  onSubmit(): void {
    this.submitSuccess.set(false);
    this.submitError.set(null);

    if (
      this.form.invalid ||
      this.productLoading() ||
      this.productLoadError() ||
      this.categoriesLoading ||
      this.categoriesError ||
      this.categories.length === 0 ||
      this.submitting()
    ) {
      this.form.markAllAsTouched();
      return;
    }

    this.submitting.set(true);

    const saveRequest = this.productId
      ? this.productsService.updateProduct(this.productId, this.toRequest())
      : this.productsService.createProduct(this.toRequest());

    saveRequest
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => {
          this.submitting.set(false);
          this.submitSuccess.set(true);
        },
        error: (error: unknown) => {
          this.submitting.set(false);
          this.handleSubmitError(error);
        },
      });
  }

  private handleSubmitError(error: unknown): void {
    this.serverFieldErrors = {};

    if (!(error instanceof HttpErrorResponse) || !this.isRecord(error.error)) {
      this.submitError.set('No se pudo guardar el producto. Inténtalo de nuevo.');
      return;
    }

    const response = error.error;
    const message = typeof response['message'] === 'string' ? response['message'] : null;
    const unrecognizedErrors = this.parseFieldErrors(response['errors']);

    if (this.isCategoryErrorMessage(message)) {
      this.serverFieldErrors.categoriaIds = message;
      this.submitError.set(message);
      return;
    }

    this.submitError.set(this.resolveSubmitErrorMessage(message, unrecognizedErrors, error.status));
  }

  private parseFieldErrors(errorsPayload: unknown): string[] {
    if (!this.isRecord(errorsPayload)) {
      return [];
    }

    const unrecognizedErrors: string[] = [];
    for (const [field, fieldError] of Object.entries(errorsPayload)) {
      if (typeof fieldError === 'string') {
        this.assignFieldError(field, fieldError, unrecognizedErrors);
      }
    }

    return unrecognizedErrors;
  }

  private assignFieldError(field: string, error: string, unrecognizedErrors: string[]): void {
    const matchingField = SERVER_FIELD_TO_FORM_FIELD[field];
    if (matchingField) {
      this.serverFieldErrors[matchingField] = error;
    } else {
      unrecognizedErrors.push(`${field}: ${error}`);
    }
  }

  private isCategoryErrorMessage(message: string | null): message is string {
    return Boolean(
      message &&
      /categor[ií]as?/i.test(message) &&
      /no existen|no existe|no válid/i.test(message),
    );
  }

  private resolveSubmitErrorMessage(
    message: string | null,
    unrecognizedErrors: string[],
    status: number,
  ): string {
    if (unrecognizedErrors.length > 0) {
      return [message, ...unrecognizedErrors].filter(Boolean).join('. ');
    }
    if (Object.keys(this.serverFieldErrors).length > 0) {
      return 'Revisa los campos marcados y vuelve a intentarlo.';
    }
    return message || this.getFallbackError(status);
  }

  private getFallbackError(status: number): string {
    if (status === 0) {
      return 'No se pudo conectar con el servidor. Comprueba tu conexión e inténtalo de nuevo.';
    }
    if (status === 401) {
      return 'Tu sesión ha caducado. Inicia sesión de nuevo para guardar el producto.';
    }
    if (status === 403) {
      return 'No tienes permiso para guardar este producto.';
    }
    if (status >= 500) {
      return 'El servidor no pudo guardar el producto. Inténtalo de nuevo más tarde.';
    }
    return 'No se pudo guardar el producto. Inténtalo de nuevo.';
  }

  private getLoadErrorMessage(error: unknown): string {
    if (error instanceof HttpErrorResponse) {
      if (error.status === 401) {
        return 'Tu sesión ha caducado. Inicia sesión de nuevo para editar productos.';
      }
      if (error.status === 403) {
        return 'No tienes permiso para editar productos.';
      }
      if (error.status === 404) {
        return 'El producto ya no existe o no tienes permiso para editarlo.';
      }
      if (error.status === 0) {
        return 'No se pudo conectar con el servidor. Comprueba tu conexión e inténtalo de nuevo.';
      }
    }
    return 'No se pudo cargar el producto. Inténtalo de nuevo.';
  }

  private isRecord(value: unknown): value is Record<string, unknown> {
    return typeof value === 'object' && value !== null && !Array.isArray(value);
  }

  private toRequest(): ProductCreateRequest {
    const value = this.form.getRawValue();
    const request: ProductCreateRequest = {
      name: value.nombre.trim(),
      categoryIds: value.categoriaIds,
      price: value.precio ?? 0,
      available: value.disponible ?? false,
    };

    if (value.stock !== null) {
      request.stock = value.stock;
    }
    if (value.descripcion.trim()) {
      request.description = value.descripcion.trim();
    }
    if (value.descuento !== null) {
      request.discount = value.descuento;
    }
    if (value.descuentoPremium !== null) {
      request.premiumDiscount = value.descuentoPremium;
    }
    if (value.imagen.trim()) {
      request.imageUrl = value.imagen.trim();
    }

    return request;
  }
}
