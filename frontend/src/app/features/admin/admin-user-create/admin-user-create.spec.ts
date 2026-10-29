import { DatePipe } from '@angular/common';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { of } from 'rxjs';
import { describe, it, expect, beforeEach, vi } from 'vitest';
import { AuthService } from '../../../core/services/auth.service';
import { AdminUserCreate } from './admin-user-create';

describe('AdminUserCreate', () => {
  let component: AdminUserCreate;
  let fixture: ComponentFixture<AdminUserCreate>;
  let createAdmin: ReturnType<typeof vi.fn>;
  let navigate: ReturnType<typeof vi.spyOn>;

  beforeEach(async () => {
    createAdmin = vi.fn();

    await TestBed.configureTestingModule({
      imports: [AdminUserCreate],
      providers: [
        provideRouter([]),
        {
          provide: AuthService,
          useValue: { createAdmin },
        },
      ],
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
    expect(form.get('nationalId')?.hasError('required')).toBe(true);
    expect(form.get('firstName')?.hasError('required')).toBe(true);
    expect(form.get('lastName')?.hasError('required')).toBe(true);
    expect(form.get('email')?.hasError('required')).toBe(true);
    expect(form.get('password')?.hasError('required')).toBe(true);
    expect(form.get('confirmPassword')?.hasError('required')).toBe(true);
  });

  it('validates password mismatch cross-validation', () => {
    component.form.patchValue({
      nationalId: '12345678Z',
      firstName: 'Carlos',
      lastName: 'Gómez Pérez',
      email: 'carlos.admin@esibuy.com',
      password: 'Admin123456!#',
      confirmPassword: 'Distinta123456!#'
    });

    expect(component.form.hasError('passwordsMismatch')).toBe(true);
    expect(component.form.valid).toBe(false);
  });

  it('is valid when all required fields and formats are correct', () => {
    component.form.patchValue({
      nationalId: '12345678Z',
      firstName: 'Carlos',
      lastName: 'Gómez Pérez',
      email: 'carlos.admin@esibuy.com',
      password: 'Admin123456!#',
      confirmPassword: 'Admin123456!#',
      headquarters: 'Ciudad Real',
      profileImage: 'https://avatar.ejemplo.com/carlos.png'
    });

    expect(component.form.valid).toBe(true);
  });

  it('renders all form inputs and the primary submit button with exact text "Crear administrador"', () => {
    const rendered = fixture.nativeElement as HTMLElement;
    expect(rendered.querySelector('input[formControlName="nationalId"]')).toBeTruthy();
    expect(rendered.querySelector('input[formControlName="firstName"]')).toBeTruthy();
    expect(rendered.querySelector('input[formControlName="lastName"]')).toBeTruthy();
    expect(rendered.querySelector('input[formControlName="email"]')).toBeTruthy();
    expect(rendered.querySelector('input[formControlName="password"]')).toBeTruthy();
    expect(rendered.querySelector('input[formControlName="confirmPassword"]')).toBeTruthy();
    expect(rendered.querySelector('input[formControlName="headquarters"]')).toBeTruthy();

    const submitBtn = rendered.querySelector('button[type="submit"]');
    expect(submitBtn).toBeTruthy();
    expect(submitBtn?.textContent?.trim()).toContain('Dar de alta Administrador');
  });

  it('calls createAdmin on submit when form is valid', () => {
    createAdmin.mockReturnValue(
      of({
        success: true,
        message: 'Administrador creado correctamente',
        data: null,
        correlationId: null,
      }),
    );

    component.form.patchValue({
      nationalId: '12345678Z',
      firstName: 'Carlos',
      lastName: 'Gómez Pérez',
      email: 'carlos.admin@esibuy.com',
      password: 'Admin123456!#',
      confirmPassword: 'Admin123456!#',
      headquarters: 'Ciudad Real',
      profileImage: component.avatars[0],
    });

    fixture.detectChanges();

    const submitButton = fixture.nativeElement.querySelector(
      'button[type="submit"]',
    ) as HTMLButtonElement;

    submitButton.click();

    expect(createAdmin).toHaveBeenCalled();
  });

  it('navigates to /admin/usuarios when cancel button is clicked', () => {
    const cancelBtn = fixture.nativeElement.querySelector('button.btn-secondary') as HTMLButtonElement;
    cancelBtn?.click();

    expect(navigate).toHaveBeenCalledWith(['/admin/usuarios']);
  });

  it('renders non-modifiable creation date input with current date', () => {
    const rendered = fixture.nativeElement as HTMLElement;
    const fechaInput = rendered.querySelector<HTMLInputElement>('input#creationDate');

    expect(fechaInput).toBeTruthy();
    expect(fechaInput?.readOnly).toBe(true);
    const datePipe = new DatePipe('en-US');
    expect(fechaInput?.value).toBe(datePipe.transform(component.creationDate, 'dd/MM/yyyy HH:mm:ss'));
  });


  it('renders 10 registration avatars and selects the first one by default', () => {
    const rendered = fixture.nativeElement as HTMLElement;
    const avatarButtons = rendered.querySelectorAll<HTMLButtonElement>(
      '.avatar-selector-grid .avatar-item',
    );

    expect(avatarButtons.length).toBe(10);
    expect(component.form.get('profileImage')?.value).toBe(component.avatars[0]);
    expect(avatarButtons[0].classList.contains('selected')).toBe(true);

    avatarButtons[1].click();
    fixture.detectChanges();

    expect(component.form.get('profileImage')?.value).toBe(component.avatars[1]);
    expect(avatarButtons[1].classList.contains('selected')).toBe(true);

    avatarButtons[1].click();
    fixture.detectChanges();

    expect(component.form.get('profileImage')?.value).toBe(component.avatars[1]);
  });


  it('does not display any "Opcional" text or badges in the template', () => {
    const text = fixture.nativeElement.textContent || '';
    expect(text.toLowerCase()).not.toContain('opcional');
  });

  it('has back link pointing to /admin/usuarios', () => {
    const backLink = fixture.nativeElement.querySelector('a[routerLink="/admin/usuarios"]') as HTMLAnchorElement;
    expect(backLink).toBeTruthy();
    expect(backLink.getAttribute('routerLink')).toBe('/admin/usuarios');
  });
});
