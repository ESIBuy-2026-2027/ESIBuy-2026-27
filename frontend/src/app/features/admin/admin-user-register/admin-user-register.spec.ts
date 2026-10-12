import { ComponentFixture, TestBed } from '@angular/core/testing';
import { AdminUserRegister } from './admin-user-register';
import { AuthService } from '../../../core/services/auth.service';
import { ProductsService } from '../../products/products.service';
import { of, throwError } from 'rxjs';
import { provideRouter, Router } from '@angular/router';
import { describe, it, expect, beforeEach, vi } from 'vitest';

describe('AdminUserRegister', () => {
  let component: AdminUserRegister;
  let fixture: ComponentFixture<AdminUserRegister>;
  let authServiceSpy: any;
  let productsServiceSpy: any;
  let router: Router;

  const mockCategories = [
    { id: 'cat-1', name: 'Electrónica' },
    { id: 'cat-2', name: 'Hogar' },
    { id: 'cat-3', name: 'Moda' }
  ];

  beforeEach(async () => {
    authServiceSpy = {
      registerClient: vi.fn().mockReturnValue(of({ success: true })),
      registerSeller: vi.fn().mockReturnValue(of({ success: true }))
    };

    productsServiceSpy = {
      getCategories: vi.fn().mockReturnValue(of(mockCategories))
    };

    await TestBed.configureTestingModule({
      imports: [AdminUserRegister],
      providers: [
        provideRouter([]),
        { provide: AuthService, useValue: authServiceSpy },
        { provide: ProductsService, useValue: productsServiceSpy }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(AdminUserRegister);
    component = fixture.componentInstance;
    router = TestBed.inject(Router);
    fixture.detectChanges();
  });

  it('debe crear el componente correctamente', () => {
    expect(component).toBeTruthy();
  });

  describe('Selector de Rol y Restricción de Seguridad', () => {
    it('debe incluir obligatoriamente los botones para "cliente" y "vendedor"', () => {
      const compiled = fixture.nativeElement as HTMLElement;

      const btnCliente = compiled.querySelector('#btnRoleCliente');
      const btnVendedor = compiled.querySelector('#btnRoleVendedor');

      expect(btnCliente).not.toBeNull();
      expect(btnVendedor).not.toBeNull();
    });

    it('NUNCA debe permitir crear un usuario administrador desde esta vista', () => {
      // Verificación en la lista de roles permitidos del componente
      expect(component.allowedRoles).not.toContain('admin');
      expect(component.allowedRoles).not.toContain('administrador');
      expect(component.allowedRoles).toEqual(['cliente', 'vendedor']);

      // Intento de forzar un rol 'admin' mediante setRole
      component.setRole('admin');
      expect(component.selectedRole).not.toBe('admin');
      expect(component.selectedRole).toBe('cliente');
      expect(component.serverError).toContain('Rol no permitido');
    });

    it('debe cambiar de rol a "vendedor" y adaptar los campos del formulario', () => {
      component.setRole('vendedor');
      fixture.detectChanges();

      expect(component.selectedRole).toBe('vendedor');
      expect(component.form.get('businessName')).not.toBeNull();
      expect(component.form.get('defaultCategory')).not.toBeNull();
      expect(component.form.get('birthDate')).toBeNull();
      expect(component.form.get('customerType')).toBeNull();

      const compiled = fixture.nativeElement as HTMLElement;
      expect(compiled.querySelector('#businessName')).not.toBeNull();
      expect(compiled.querySelector('#defaultCategory')).not.toBeNull();
    });

    it('debe volver al rol "cliente" y reestablecer los campos de cliente', () => {
      component.setRole('vendedor');
      fixture.detectChanges();
      component.setRole('cliente');
      fixture.detectChanges();

      expect(component.selectedRole).toBe('cliente');
      expect(component.form.get('birthDate')).not.toBeNull();
      expect(component.form.get('customerType')).not.toBeNull();
      expect(component.form.get('businessName')).toBeNull();
      expect(component.form.get('defaultCategory')).toBeNull();
    });
  });

  describe('Validación y Envío del Formulario', () => {
    it('debe marcar todos los campos si el formulario está vacío al enviar', () => {
      component.onSubmit();
      fixture.detectChanges();

      expect(component.form.invalid).toBe(true);
      expect(component.form.get('firstName')?.touched).toBe(true);
      expect(component.form.get('email')?.touched).toBe(true);
      expect(authServiceSpy.registerClient).not.toHaveBeenCalled();
    });

    it('debe enviar la solicitud a registerClient cuando el rol es cliente y los datos son válidos', () => {
      component.setRole('cliente');
      component.form.patchValue({
        firstName: 'Juan',
        lastName: 'Pérez García',
        birthDate: '1995-05-10',
        nationalId: '12345678Z',
        email: 'juan.perez@example.com',
        phone: '611222333',
        customerType: 'STANDARD',
        password: 'Password123!',
        repeatedPassword: 'Password123!'
      });

      component.onSubmit();

      expect(authServiceSpy.registerClient).toHaveBeenCalled();
      const payload = authServiceSpy.registerClient.mock.calls[0][0];
      expect(payload.firstName).toBe('Juan');
      expect(payload.email).toBe('juan.perez@example.com');
      expect(payload.customerType).toBe('STANDARD');
      expect(component.successMessage).toContain('Juan Pérez García');
    });

    it('debe enviar la solicitud a registerSeller cuando el rol es vendedor y los datos son válidos', () => {
      component.setRole('vendedor');
      component.form.patchValue({
        firstName: 'Ana',
        lastName: 'López Ruiz',
        businessName: 'Tienda Ana Tech',
        defaultCategory: 'Electrónica',
        nationalId: '87654321A',
        email: 'ana.tech@example.com',
        phone: '699888777',
        password: 'Password123!',
        repeatedPassword: 'Password123!'
      });

      component.onSubmit();

      expect(authServiceSpy.registerSeller).toHaveBeenCalled();
      const payload = authServiceSpy.registerSeller.mock.calls[0][0];
      expect(payload.firstName).toBe('Ana');
      expect(payload.businessName).toBe('Tienda Ana Tech');
      expect(payload.defaultCategory).toBe('Electrónica');
      expect(component.successMessage).toContain('Tienda Ana Tech');
    });

    it('debe manejar errores del servidor al registrar', () => {
      authServiceSpy.registerClient.mockReturnValue(
        throwError(() => ({ error: { message: 'El correo electrónico ya se encuentra registrado' } }))
      );

      component.setRole('cliente');
      component.form.patchValue({
        firstName: 'Carlos',
        lastName: 'Méndez',
        birthDate: '1990-01-01',
        nationalId: '11223344B',
        email: 'carlos@example.com',
        customerType: 'STANDARD',
        password: 'Password123!',
        repeatedPassword: 'Password123!'
      });

      component.onSubmit();
      fixture.detectChanges();

      expect(component.submitting).toBe(false);
      expect(component.serverError).toBe('El correo electrónico ya se encuentra registrado');
    });

    it('debe navegar hacia /admin/usuarios al pulsar cancelar', () => {
      const navigateSpy = vi.spyOn(router, 'navigate');
      component.cancelar();
      expect(navigateSpy).toHaveBeenCalledWith(['/admin/usuarios']);
    });

    it('debe renderizar solo avatares de cliente y permitir alternar la selección', () => {
      const rendered = fixture.nativeElement as HTMLElement;
      const avatarButtons = rendered.querySelectorAll('.avatar-selector-grid .avatar-item');
      expect(avatarButtons.length).toBe(5);

      const firstAvatarBtn = avatarButtons[0] as HTMLButtonElement;
      firstAvatarBtn.click();
      fixture.detectChanges();
      expect(component.form.get('imagenPerfil')?.value).toBe(component.clientAvatars[0]);
      expect(component.selectedAvatar).toBe(component.clientAvatars[0]);

      // Al pulsar de nuevo, se deselecciona
      firstAvatarBtn.click();
      fixture.detectChanges();
      expect(component.form.get('imagenPerfil')?.value).toBe('');
      expect(component.selectedAvatar).toBeNull();
    });

    it('debe cambiar los avatares disponibles y limpiar la selección al cambiar de rol', () => {
      component.selectAvatar(component.clientAvatars[0]);
      component.setRole('vendedor');
      fixture.detectChanges();

      const rendered = fixture.nativeElement as HTMLElement;
      const avatarButtons = rendered.querySelectorAll('.avatar-selector-grid .avatar-item');
      expect(avatarButtons.length).toBe(5);
      expect(avatarButtons[0].querySelector('img')?.src).toContain('av_seller_1.jpeg');
      expect(component.selectedAvatar).toBeNull();
      expect(component.form.get('imagenPerfil')?.value).toBe('');
    });

    it('debe incluir el avatar seleccionado en el payload al enviar el formulario', () => {
      component.setRole('cliente');
      component.selectAvatar(component.clientAvatars[2]);
      component.form.patchValue({
        firstName: 'Laura',
        lastName: 'Gómez',
        birthDate: '1998-03-15',
        nationalId: '87654321X',
        email: 'laura@example.com',
        phone: '611999888',
        customerType: 'STANDARD',
        password: 'Password123!',
        repeatedPassword: 'Password123!'
      });

      component.onSubmit();

      expect(authServiceSpy.registerClient).toHaveBeenCalled();
      const payload = authServiceSpy.registerClient.mock.calls[0][0];
      expect(payload.profileImage).toBe(component.clientAvatars[2]);
      expect(payload.imagenPerfil).toBe(component.clientAvatars[2]);
    });
  });
});
