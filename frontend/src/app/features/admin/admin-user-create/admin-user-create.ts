import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Router } from '@angular/router';

@Component({
  selector: 'app-admin-user-create',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, FormsModule],
  template: `
    <div class="admin-create-container">
      <h2>Alta de Administrador</h2>
      <form [formGroup]="form" (ngSubmit)="onSubmit()">
        <div class="form-group">
          <label>Nombre *</label>
          <input type="text" formControlName="nombre" />
          <span class="error" *ngIf="form.get('nombre')?.touched && form.get('nombre')?.hasError('required')">Nombre obligatorio</span>
        </div>

        <div class="form-group">
          <label>Apellidos *</label>
          <input type="text" formControlName="apellidos" />
          <span class="error" *ngIf="form.get('apellidos')?.touched && form.get('apellidos')?.hasError('required')">Apellidos obligatorios</span>
        </div>

        <div class="form-group">
          <label>Email (único) *</label>
          <input type="email" formControlName="email" />
          <span class="error" *ngIf="form.get('email')?.touched && form.get('email')?.hasError('required')">Email obligatorio</span>
          <span class="error" *ngIf="form.get('email')?.touched && form.get('email')?.hasError('email')">Email no válido</span>
        </div>

        <div class="form-group">
          <label>Sede</label>
          <input type="text" formControlName="sede" placeholder="Ej: Madrid, Ciudad Real..." />
        </div>

        <div class="form-group">
          <label>Fecha de incorporación * (Automática y no editable)</label>
          <input type="text" formControlName="fechaIncorporacion" readonly />
        </div>

        <div class="form-group">
          <label>Imagen de Perfil</label>
          <div class="radio-options">
            <label><input type="radio" name="imgOpt" [(ngModel)]="imageOption" [ngModelOptions]="{standalone: true}" value="default" /> Avatar por defecto</label>
            <label><input type="radio" name="imgOpt" [(ngModel)]="imageOption" [ngModelOptions]="{standalone: true}" value="upload" /> Subir imagen</label>
          </div>
          <div *ngIf="imageOption === 'upload'" style="margin-top: 0.5rem;">
            <input type="file" (change)="onFileSelected($event)" accept="image/*" />
          </div>
          <div class="avatar-preview" style="margin-top: 0.5rem;">
            <img [src]="profileImage" alt="Avatar Admin" style="width: 80px; height: 80px; border-radius: 50%; object-fit: cover;" />
          </div>
        </div>

        <button type="submit" class="btn-submit">Dar de Alta Administrador</button>
      </form>
    </div>
  `,
  styles: [`
    .admin-create-container { max-width: 500px; margin: 2rem auto; padding: 2rem; border: 1px solid #ccc; border-radius: 8px; font-family: sans-serif; }
    .form-group { display: flex; flex-direction: column; margin-bottom: 1rem; }
    .form-group label { font-weight: bold; margin-bottom: 0.3rem; }
    .form-group input { padding: 0.5rem; border: 1px solid #ccc; border-radius: 4px; }
    .form-group input[readonly] { background-color: #e9ecef; cursor: not-allowed; }
    .error { color: red; font-size: 0.85rem; }
    .btn-submit { width: 100%; padding: 0.75rem; background: #007bff; color: white; border: none; border-radius: 4px; font-weight: bold; cursor: pointer; }
  `]
})
export class AdminUserCreate {
  form: FormGroup;
  imageOption: 'default' | 'upload' = 'default';
  uploadedImageBase64: string | null = null;
  defaultAvatar = 'https://api.dicebear.com/7.x/bottts/svg?seed=admin_default';

  constructor(private fb: FormBuilder, private router: Router) {
    const todayISO = new Date().toISOString().split('T')[0];
    this.form = this.fb.group({
      nombre: ['', [Validators.required]],
      apellidos: ['', [Validators.required]],
      email: ['', [Validators.required, Validators.email]],
      sede: [''],
      fechaIncorporacion: [{ value: todayISO, disabled: false }, [Validators.required]]
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

  get profileImage(): string {
    return (this.imageOption === 'upload' && this.uploadedImageBase64) ? this.uploadedImageBase64 : this.defaultAvatar;
  }

  onSubmit() {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const payload = {
      ...this.form.getRawValue(),
      rol: 'admin',
      imagenPerfil: this.profileImage,
      activo: true // Admin por defecto activado
    };
    console.log('Alta de Administrador:', payload);
    alert('Administrador dado de alta correctamente (Activado por defecto).');
    this.router.navigate(['/']);
  }
}
