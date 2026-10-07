import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { describe, it, expect, beforeEach, vi } from 'vitest';
import { AuthService } from '../../../core/services/auth.service';
import { AdminUserCreate } from './admin-user-create';

describe('AdminUserCreate', () => {
  let component: AdminUserCreate;
  let fixture: ComponentFixture<AdminUserCreate>;
  let crearAdministrador: ReturnType<typeof vi.fn>;
  let navigate: ReturnType<typeof vi.fn>;

  beforeEach(async () => {
    crearAdministrador = vi.fn();
    navigate = vi.fn().mockResolvedValue(true);

    await TestBed.configureTestingModule({
      imports: [AdminUserCreate],
      providers: [
        { provide: Router, useValue: { navigate } },
        { provide: AuthService, useValue: { crearAdministrador } }
      ]
    }).compileComponents();

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
});
