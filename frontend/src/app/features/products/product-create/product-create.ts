import { Component, OnInit, inject } from '@angular/core';
import { ReactiveFormsModule, FormControl, FormGroup, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
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

@Component({
  selector: 'app-product-create',
  standalone: true,
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './product-create.html',
  styleUrl: './product-create.css',
})
export class ProductCreate implements OnInit {
  private readonly productsService = inject(ProductsService);
  private readonly router = inject(Router);

  readonly form = new FormGroup<ProductFormControls>({
    nombre: new FormControl('', { nonNullable: true, validators: [
      Validators.required,
      Validators.pattern(/\S/),
      Validators.maxLength(150),
    ] }),
    categoriaIds: new FormControl<string[]>([], {
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
  submitting = false;
  submitError: string | null = null;

  ngOnInit(): void {
    this.productsService.getCategories().subscribe({
      next: (categories) => {
        this.categories = categories;
        this.categoriesLoading = false;
      },
      error: () => {
        this.categoriesError = true;
        this.categoriesLoading = false;
      },
    });
  }

  onSubmit(): void {
    this.submitError = null;

    if (this.form.invalid || this.categoriesError || this.submitting) {
      this.form.markAllAsTouched();
      return;
    }

    this.submitting = true;
    this.productsService.createProduct(this.toRequest()).subscribe({
      next: () => {
        this.submitting = false;
        void this.router.navigate(['/seller/catalog']);
      },
      error: () => {
        this.submitting = false;
        this.submitError = 'No se pudo guardar el producto. Inténtalo de nuevo.';
      },
    });
  }

  private toRequest(): ProductCreateRequest {
    const value = this.form.getRawValue();
    const request: ProductCreateRequest = {
      nombre: value.nombre.trim(),
      categoriaIds: value.categoriaIds,
      precio: value.precio ?? 0,
      disponible: value.disponible ?? false,
    };

    if (value.stock !== null) {
      request.stock = value.stock;
    }
    if (value.descripcion.trim()) {
      request.descripcion = value.descripcion.trim();
    }
    if (value.descuento !== null) {
      request.descuento = value.descuento;
    }
    if (value.descuentoPremium !== null) {
      request.descuentoPremium = value.descuentoPremium;
    }
    if (value.imagen.trim()) {
      request.imagen = value.imagen.trim();
    }

    return request;
  }
}
