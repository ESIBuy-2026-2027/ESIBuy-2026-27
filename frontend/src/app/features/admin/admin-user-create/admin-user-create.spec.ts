import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { of } from 'rxjs';
import { describe, it, expect, beforeEach, vi } from 'vitest';
import { AuthService } from '../../../core/services/auth.service';
import { AdminUserCreate } from './admin-user-create';

describe('AdminUserCreate', () => {
  let component: AdminUserCreate;
  let fixture: ComponentFixture<AdminUserCreate>;
  let crearAdministrador: ReturnType<typeof vi.fn>;
  let navigate: ReturnType<typeof vi.spyOn>;

  beforeEach(async () => {
    crearAdministrador = vi.fn();

    await TestBed.configureTestingModule({
      imports: [AdminUserCreate],
      providers: [
        provideRouter([]),
        { provide: AuthService, useValue: { crearAdministrador } }
      ]
    }).compileComponents();

    const router = TestBed.inject(Router);
    navigate = vi.spyOn(router, 'navigate').mockResolvedValue(true);

    fixture = TestBed.createComponent(AdminUserCreate);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('initializes the reactive form with invalid state', () => {
    expect(component.form).toBeDefined();
    expect(component.form.valid).toBe(false);
  });

  it('validates mandatory fields', () => {
    const form = component.form;
    expect(form.get('dni')?.hasError('required')).toBe(true);
    expect(form.get('nombre')?.hasError('required')).toBe(true);
    expect(form.get('apellidos')?.hasError('required')).toBe(true);
    expect(form.get('email')?.hasError('required')).toBe(true);
    expect(form.get('contrasena')?.hasError('required')).toBe(true);
    expect(form.get('repetirContrasena')?.hasError('required')).toBe(true);
  });

  it('validates password mismatch cross-validation', () => {
    component.form.patchValue({
      dni: '12345678Z',
      nombre: 'Carlos',
      apellidos: 'Gómez Pérez',
      email: 'carlos.admin@esibuy.com',
      contrasena: 'Admin123456!#',
      repetirContrasena: 'Distinta123456!#'
    });

    expect(component.form.hasError('passwordsMismatch')).toBe(true);
    expect(component.form.valid).toBe(false);
  });

  it('is valid when all required fields and formats are correct', () => {
    component.form.patchValue({
      dni: '12345678Z',
      nombre: 'Carlos',
      apellidos: 'Gómez Pérez',
      email: 'carlos.admin@esibuy.com',
      contrasena: 'Admin123456!#',
      repetirContrasena: 'Admin123456!#',
      sede: 'Ciudad Real',
      imagenPerfil: 'https://avatar.ejemplo.com/carlos.png'
    });

    expect(component.form.valid).toBe(true);
  });

  it('renders all form inputs and the primary submit button with exact text "Crear administrador"', () => {
    const rendered = fixture.nativeElement as HTMLElement;
    expect(rendered.querySelector('input[formControlName="dni"]')).toBeTruthy();
    expect(rendered.querySelector('input[formControlName="nombre"]')).toBeTruthy();
    expect(rendered.querySelector('input[formControlName="apellidos"]')).toBeTruthy();
    expect(rendered.querySelector('input[formControlName="email"]')).toBeTruthy();
    expect(rendered.querySelector('input[formControlName="contrasena"]')).toBeTruthy();
    expect(rendered.querySelector('input[formControlName="repetirContrasena"]')).toBeTruthy();
    expect(rendered.querySelector('input[formControlName="sede"]')).toBeTruthy();

    const submitBtn = rendered.querySelector('button[type="submit"]');
    expect(submitBtn).toBeTruthy();
    expect(submitBtn?.textContent?.trim()).toContain('Crear administrador');
  });

  it('calls crearAdministrador on submit when form is valid', () => {
    crearAdministrador.mockReturnValue(of({ id: 'admin-1', email: 'carlos.admin@esibuy.com' }));

    component.form.patchValue({
      dni: '12345678Z',
      nombre: 'Carlos',
      apellidos: 'Gómez Pérez',
      email: 'carlos.admin@esibuy.com',
      contrasena: 'Admin123456!#',
      repetirContrasena: 'Admin123456!#',
      sede: 'Ciudad Real',
      imagenPerfil: 'https://avatar.ejemplo.com/carlos.png'
    });

    fixture.detectChanges();
    const submitBtn = fixture.nativeElement.querySelector('button[type="submit"]') as HTMLButtonElement;
    submitBtn?.click();

    expect(crearAdministrador).toHaveBeenCalled();
  });

  it('navigates to /admin/usuarios when cancel button is clicked', () => {
    const cancelBtn = fixture.nativeElement.querySelector('button.btn-secondary') as HTMLButtonElement;
    cancelBtn?.click();

    expect(navigate).toHaveBeenCalledWith(['/admin/usuarios']);
  });
});
