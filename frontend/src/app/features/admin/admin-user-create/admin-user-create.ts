import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-admin-user-create',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, FormsModule],
  template: `
    <div class="admin-create-container">
      <h2>Zona de Administración: Alta de Administrador</h2>
      <p class="subtitle">Seleccione un usuario registrado (Cliente o Vendedor) para promoverlo a Administrador.</p>

      <!-- Selector de Candidatos -->
      <div class="candidato-card">
        <label for="candidatoSelect"><strong>Paso 1: Seleccionar Usuario Elegible *</strong></label>
        <select id="candidatoSelect" class="form-select" (change)="onCandidatoChange($event)">
          <option value="">-- Seleccionar usuario candidato --</option>
          <option *ngFor="let c of candidatos" [value]="c.id">
            {{ c.nombre }} {{ c.apellidos }} ({{ c.email }}) - Rol: {{ c.rol }}
          </option>
        </select>
        <div *ngIf="candidatos.length === 0 && !loadingCandidatos" class="info-empty">
          No hay usuarios (Clientes o Vendedores) pendientes de promoción.
        </div>
      </div>

      <!-- Alertas de Errores de Validación -->
      <div *ngIf="serverErrors.length > 0" class="alert-errors">
        <strong>Se encontraron los siguientes errores:</strong>
        <ul>
          <li *ngFor="let err of serverErrors">{{ err }}</li>
        </ul>
      </div>

      <!-- Alerta de Éxito -->
      <div *ngIf="successMessage" class="alert-success">
        {{ successMessage }}
      </div>

      <!-- Formulario de Promoción -->
      <form *ngIf="candidatoSeleccionado" [formGroup]="form" (ngSubmit)="onSubmit()">
        <h3>Paso 2: Datos del Nuevo Administrador</h3>

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
          <label>Email de Cuenta * (Protegido / No editable)</label>
          <input type="email" formControlName="email" readonly class="input-locked" />
          <small class="hint">El email no puede modificarse para preservar la integridad de la cuenta.</small>
        </div>

        <div class="form-group">
          <label>Sede (Exclusivo Administrador)</label>
          <input type="text" formControlName="sede" placeholder="Ej: Ciudad Real, Toledo, Madrid..." />
        </div>

        <div class="form-group">
          <label>Fecha de incorporación * (Automática y no editable)</label>
          <input type="text" formControlName="fechaIncorporacion" readonly class="input-locked" />
          <small class="hint">Fecha asignada automáticamente hoy: {{ form.get('fechaIncorporacion')?.value }}</small>
        </div>

        <div class="form-group">
          <label>Avatar de Administrador (Selecciona un avatar) *</label>
          <div class="avatar-grid">
            <button
              type="button"
              *ngFor="let av of avatars"
              class="avatar-item"
              [class.selected]="selectedAvatarUrl === av.url"
              (click)="selectAvatar(av.url)"
              [title]="av.label"
            >
              <img [src]="av.url" [alt]="av.label" />
              <span class="avatar-check" *ngIf="selectedAvatarUrl === av.url">✓</span>
            </button>
          </div>
          <small class="hint">Haz clic sobre un avatar para asignarlo al perfil del administrador.</small>
        </div>

        <button type="submit" class="btn-submit" [disabled]="submitting">
          {{ submitting ? 'Guardando...' : 'Promover a Administrador' }}
        </button>
      </form>
    </div>
  `,
  styles: [`
    .admin-create-container { max-width: 600px; margin: 2rem auto; padding: 2rem; border: 1px solid #e2e8f0; border-radius: 12px; font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif; background: #fff; box-shadow: 0 4px 6px -1px rgba(0,0,0,0.1); }
    .subtitle { color: #64748b; font-size: 0.9rem; margin-bottom: 1.5rem; }
    .candidato-card { background: #f8fafc; border: 1px solid #cbd5e1; border-radius: 8px; padding: 1rem; margin-bottom: 1.5rem; }
    .form-select { width: 100%; padding: 0.6rem; border: 1px solid #94a3b8; border-radius: 6px; margin-top: 0.5rem; font-size: 0.95rem; }
    .info-empty { color: #64748b; font-size: 0.85rem; margin-top: 0.5rem; }
    .alert-errors { background: #fef2f2; border: 1px solid #fecaca; color: #dc2626; padding: 1rem; border-radius: 8px; margin-bottom: 1.5rem; }
    .alert-errors ul { margin: 0.5rem 0 0 1.25rem; font-size: 0.9rem; }
    .alert-success { background: #f0fdf4; border: 1px solid #bbf7d0; color: #16a34a; padding: 1rem; border-radius: 8px; margin-bottom: 1.5rem; font-weight: 500; }
    .form-group { display: flex; flex-direction: column; margin-bottom: 1.25rem; }
    .form-group label { font-weight: 600; margin-bottom: 0.3rem; font-size: 0.9rem; color: #334155; }
    .form-group input { padding: 0.6rem; border: 1.5px solid #cbd5e1; border-radius: 6px; font-size: 0.95rem; }
    .form-group input:focus { outline: none; border-color: #2563eb; }
    .input-locked { background-color: #f1f5f9 !important; color: #64748b !important; cursor: not-allowed; border-color: #cbd5e1 !important; }
    .hint { color: #64748b; font-size: 0.8rem; margin-top: 0.25rem; }
    .avatar-grid { display: flex; gap: 1rem; flex-wrap: wrap; margin-top: 0.5rem; }
    .avatar-item { position: relative; width: 68px; height: 68px; border-radius: 50%; border: 3px solid #cbd5e1; background: #f8fafc; cursor: pointer; padding: 0; overflow: hidden; transition: all 0.2s ease; display: flex; align-items: center; justify-content: center; }
    .avatar-item:hover { border-color: #93c5fd; transform: translateY(-2px); }
    .avatar-item.selected { border-color: #2563eb; box-shadow: 0 0 0 3px rgba(37, 99, 235, 0.25); transform: scale(1.05); }
    .avatar-item img { width: 100%; height: 100%; object-fit: cover; }
    .avatar-check { position: absolute; bottom: 2px; right: 2px; background: #2563eb; color: white; font-size: 11px; font-weight: bold; border-radius: 50%; width: 18px; height: 18px; display: flex; align-items: center; justify-content: center; border: 1.5px solid white; }
    .error { color: #dc2626; font-size: 0.82rem; margin-top: 0.25rem; }
    .btn-submit { width: 100%; padding: 0.8rem; background: #2563eb; color: white; border: none; border-radius: 6px; font-weight: 600; cursor: pointer; transition: background 0.2s; font-size: 1rem; }
    .btn-submit:hover:not(:disabled) { background: #1d4ed8; }
    .btn-submit:disabled { opacity: 0.6; cursor: not-allowed; }
  `]
})
export class AdminUserCreate implements OnInit {
  form: FormGroup;
  candidatos: any[] = [];
  candidatoSeleccionado: any = null;
  loadingCandidatos = false;
  submitting = false;
  serverErrors: string[] = [];
  successMessage: string | null = null;

  avatars = [
    { id: 'admin1', label: 'Admin Clásico', url: 'https://api.dicebear.com/7.x/bottts/svg?seed=admin_default' },
    { id: 'admin2', label: 'Admin Guardián', url: 'https://api.dicebear.com/7.x/bottts/svg?seed=admin_shield' },
    { id: 'admin3', label: 'Admin Operaciones', url: 'https://api.dicebear.com/7.x/bottts/svg?seed=admin_gear' },
    { id: 'admin4', label: 'Admin Seguridad', url: 'https://api.dicebear.com/7.x/bottts/svg?seed=admin_security' },
    { id: 'admin5', label: 'Admin Estrella', url: 'https://api.dicebear.com/7.x/bottts/svg?seed=admin_star' }
  ];
  selectedAvatarUrl: string = 'https://api.dicebear.com/7.x/bottts/svg?seed=admin_default';

  constructor(
    private fb: FormBuilder,
    private router: Router,
    private authService: AuthService
  ) {
    const todayISO = new Date().toISOString().split('T')[0];
    this.form = this.fb.group({
      nombre: ['', [Validators.required]],
      apellidos: ['', [Validators.required]],
      email: [{ value: '', disabled: false }, [Validators.required]],
      sede: [''],
      fechaIncorporacion: [{ value: todayISO, disabled: false }]
    });
  }

  ngOnInit(): void {
    this.cargarCandidatos();
  }

  cargarCandidatos(): void {
    this.loadingCandidatos = true;
    this.authService.getCandidatos().subscribe({
      next: (data) => {
        this.candidatos = data || [];
        this.loadingCandidatos = false;
      },
      error: (err) => {
        console.error('Error cargando candidatos:', err);
        this.loadingCandidatos = false;
      }
    });
  }

  onCandidatoChange(event: any): void {
    const id = event.target.value;
    this.serverErrors = [];
    this.successMessage = null;

    if (!id) {
      this.candidatoSeleccionado = null;
      this.form.reset();
      return;
    }

    const todayISO = new Date().toISOString().split('T')[0];
    this.candidatoSeleccionado = this.candidatos.find(c => c.id === id);
    if (this.candidatoSeleccionado) {
      this.form.patchValue({
        nombre: this.candidatoSeleccionado.nombre || '',
        apellidos: this.candidatoSeleccionado.apellidos || '',
        email: this.candidatoSeleccionado.email || '',
        sede: this.candidatoSeleccionado.sede || '',
        fechaIncorporacion: todayISO
      });
      this.selectedAvatarUrl = this.candidatoSeleccionado.imagenPerfil || this.avatars[0].url;
    }
  }

  selectAvatar(url: string): void {
    this.selectedAvatarUrl = url;
  }

  onSubmit(): void {
    this.serverErrors = [];
    this.successMessage = null;

    if (!this.candidatoSeleccionado) {
      this.serverErrors = ['Debe seleccionar un usuario candidato elegible.'];
      return;
    }

    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.submitting = true;
    const payload = {
      nombre: this.form.value.nombre,
      apellidos: this.form.value.apellidos,
      email: this.form.value.email,
      sede: this.form.value.sede || null,
      imagenPerfil: this.selectedAvatarUrl
    };

    this.authService.promoverAAdmin(this.candidatoSeleccionado.id, payload).subscribe({
      next: (res) => {
        this.submitting = false;
        this.successMessage = `¡Usuario "${payload.nombre} ${payload.apellidos}" promovido exitosamente a Administrador y activado!`;
        this.candidatoSeleccionado = null;
        this.form.reset();
        this.cargarCandidatos();
      },
      error: (err) => {
        this.submitting = false;
        if (err?.error?.errors && typeof err.error.errors === 'object') {
          this.serverErrors = Object.entries(err.error.errors).map(([field, msg]) => `${field.toUpperCase()}: ${msg}`);
        } else if (err?.error?.message) {
          this.serverErrors = [err.error.message];
        } else {
          this.serverErrors = ['Error al procesar la solicitud en el servidor.'];
        }
      }
    });
  }
}
