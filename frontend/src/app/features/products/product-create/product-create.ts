import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Router } from '@angular/router';

@Component({
  selector: 'app-product-create',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, FormsModule],
  template: `
    <div class="product-create-container">
      <h2>Alta de Producto</h2>
      <form [formGroup]="form" (ngSubmit)="onSubmit()">
        <div class="form-group">
          <label>Nombre del Producto *</label>
          <input type="text" formControlName="nombre" />
          <span class="error" *ngIf="form.get('nombre')?.touched && form.get('nombre')?.hasError('required')">El nombre es obligatorio</span>
        </div>

        <div class="form-group">
          <label>Categoría *</label>
          <select formControlName="categoria">
            <option *ngFor="let cat of categories" [value]="cat">{{ cat }}</option>
          </select>
          <span class="error" *ngIf="form.get('categoria')?.touched && form.get('categoria')?.hasError('required')">La categoría es obligatoria</span>
        </div>

        <div class="form-group">
          <label>Stock</label>
          <input type="number" formControlName="stock" min="0" />
        </div>

        <div class="form-group">
          <label>Descripción</label>
          <textarea formControlName="descripcion" rows="4"></textarea>
        </div>

        <div class="form-group">
          <label>Precio (€) *</label>
          <input type="number" step="0.01" formControlName="precio" min="0" />
          <span class="error" *ngIf="form.get('precio')?.touched && form.get('precio')?.hasError('required')">El precio es obligatorio</span>
        </div>

        <div class="form-group">
          <label>Descuento General (%)</label>
          <input type="number" formControlName="descuento" min="0" max="100" />
        </div>

        <div class="form-group">
          <label>Descuento Premium (%) (no acumulable con general)</label>
          <input type="number" formControlName="descuentoPremium" min="0" max="100" />
        </div>

        <div class="form-group">
          <label>Imagen del Producto</label>
          <div class="radio-options">
            <label><input type="radio" name="imgOpt" [(ngModel)]="imageOption" [ngModelOptions]="{standalone: true}" value="default" /> Imagen por defecto</label>
            <label><input type="radio" name="imgOpt" [(ngModel)]="imageOption" [ngModelOptions]="{standalone: true}" value="upload" /> Subir imagen</label>
          </div>
          <div *ngIf="imageOption === 'upload'" style="margin-top: 0.5rem;">
            <input type="file" (change)="onFileSelected($event)" accept="image/*" />
          </div>
          <div class="avatar-preview" style="margin-top: 0.5rem;">
            <img [src]="productImage" alt="Imagen del producto" style="width: 100px; height: 100px; object-fit: cover; border: 1px solid #ccc;" />
          </div>
        </div>

        <div class="form-group checkbox-group">
          <label>
            <input type="checkbox" formControlName="disponible" />
            Disponible / Visible *
          </label>
        </div>

        <button type="submit" class="btn-submit">Crear Producto</button>
      </form>
    </div>
  `,
  styles: [`
    .product-create-container { max-width: 600px; margin: 2rem auto; padding: 2rem; border: 1px solid #ccc; border-radius: 8px; font-family: sans-serif; }
    .form-group { display: flex; flex-direction: column; margin-bottom: 1rem; }
    .checkbox-group { flex-direction: row; align-items: center; gap: 0.5rem; }
    .form-group label { font-weight: bold; margin-bottom: 0.3rem; }
    .form-group input, .form-group select, .form-group textarea { padding: 0.5rem; border: 1px solid #ccc; border-radius: 4px; }
    .error { color: red; font-size: 0.85rem; }
    .btn-submit { width: 100%; padding: 0.75rem; background: #28a745; color: white; border: none; border-radius: 4px; font-weight: bold; cursor: pointer; }
  `]
})
export class ProductCreate {
  form: FormGroup;
  categories = ['Electrónica', 'Hogar', 'Moda', 'Deportes', 'Libros', 'Otros'];
  imageOption: 'default' | 'upload' = 'default';
  uploadedImageBase64: string | null = null;
  defaultProductImage = 'https://via.placeholder.com/150?text=Producto';

  constructor(private fb: FormBuilder, private router: Router) {
    this.form = this.fb.group({
      nombre: ['', [Validators.required]],
      categoria: [this.categories[0], [Validators.required]],
      stock: [0, [Validators.min(0)]],
      descripcion: [''],
      precio: [null, [Validators.required, Validators.min(0)]],
      descuento: [0, [Validators.min(0), Validators.max(100)]],
      descuentoPremium: [0, [Validators.min(0), Validators.max(100)]],
      disponible: [true, [Validators.required]]
    });
  }

  onFileSelected(event: any) {
    const file = event.target.files[0];
    if (file) {
      const reader = new FileReader();
      reader.onload = () => { this.uploadedImageBase64 = reader.result as string; };
      reader.readAsDataURL(file);
    }
  }

  get productImage(): string {
    return (this.imageOption === 'upload' && this.uploadedImageBase64) ? this.uploadedImageBase64 : this.defaultProductImage;
  }

  onSubmit() {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const payload = {
      ...this.form.value,
      imagen: this.productImage
    };
    console.log('Alta de Producto:', payload);
    alert('Producto creado exitosamente.');
    this.router.navigate(['/']);
  }
}
