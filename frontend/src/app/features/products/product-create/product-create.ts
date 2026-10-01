import { DestroyRef, Component, OnInit, inject, signal } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { ReactiveFormsModule, FormControl, FormGroup, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
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

const PRODUCT_FORM_FIELDS: readonly ProductFormField[] = [
  'nombre',
  'categoriaIds',
  'stock',
  'descripcion',
  'precio',
  'descuento',
  'descuentoPremium',
  'imagen',
  'disponible',
];

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

  readonly form = new FormGroup<ProductFormControls>({
    nombre: new FormControl('', { nonNullable: true, validators: [
      Validators.required,
      Validators.pattern(/\S/),
      Validators.maxLength(150),
    ] }),
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
  serverFieldErrors: Partial<Record<ProductFormField, string>> = {};

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
  }

  onSubmit(): void {
    this.submitSuccess.set(false);
    this.submitError.set(null);

    if (this.form.invalid || this.categoriesLoading || this.categoriesError
      || this.categories.length === 0 || this.submitting()) {
      this.form.markAllAsTouched();
      return;
    }

    this.submitting.set(true);
    this.productsService.createProduct(this.toRequest()).subscribe({
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
    const errors = this.isRecord(response['errors']) ? response['errors'] : {};
    const unrecognizedErrors: string[] = [];

    for (const [field, fieldError] of Object.entries(errors)) {
      if (typeof fieldError !== 'string') {
        continue;
      }

      const matchingField = PRODUCT_FORM_FIELDS.find((formField) => formField === field);
      if (matchingField) {
        this.serverFieldErrors[matchingField] = fieldError;
      } else {
        unrecognizedErrors.push(`${field}: ${fieldError}`);
      }
    }

    if (message && /categor[ií]as?/i.test(message) && /no existen|no existe|no válid/i.test(message)) {
      this.serverFieldErrors.categoriaIds = message;
      this.submitError.set(message);
    } else if (unrecognizedErrors.length > 0) {
      this.submitError.set([message, ...unrecognizedErrors].filter(Boolean).join('. '));
    } else if (Object.keys(this.serverFieldErrors).length > 0) {
      this.submitError.set('Revisa los campos marcados y vuelve a intentarlo.');
    } else {
      this.submitError.set(message || this.getFallbackError(error.status));
    }
  }

  private getFallbackError(status: number): string {
    if (status === 0) {
      return 'No se pudo conectar con el servidor. Comprueba tu conexión e inténtalo de nuevo.';
    }
    if (status === 401) {
      return 'Tu sesión ha caducado. Inicia sesión de nuevo para guardar el producto.';
    }
    if (status === 403) {
      return 'No tienes permiso para crear productos.';
    }
    if (status >= 500) {
      return 'El servidor no pudo guardar el producto. Inténtalo de nuevo más tarde.';
    }
    return 'No se pudo guardar el producto. Inténtalo de nuevo.';
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
