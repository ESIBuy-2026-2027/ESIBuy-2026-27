import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, provideRouter, Router } from '@angular/router';
import { of, throwError } from 'rxjs';
import { vi } from 'vitest';
import { AdminUserDetail } from './admin-user-detail';
import { AuthService, ApiResponse, UserResponse } from '../../../core/services/auth.service';
import { Category, ProductsService } from '../../products/products.service';

describe('AdminUserDetail', () => {
  let component: AdminUserDetail;
  let fixture: ComponentFixture<AdminUserDetail>;
  let authServiceSpy: any;
  let productsServiceSpy: { getCategories: ReturnType<typeof vi.fn> };
  const categories: Category[] = [
    { id: 'category-1', name: 'Electrónica' },
    { id: 'category-2', name: 'Hogar' },
    { id: 'category-3', name: 'Moda' },
  ];

  const mockUserResponse: ApiResponse<UserResponse> = {
    success: true,
    message: 'Usuario cargado',
    data: {
      id: 'user-123',
      email: 'test@example.com',
      firstName: 'Carlos',
      lastName: 'García',
      nationalId: '12345678A',
      phone: '612345678',
      role: 'CLIENT',
      profileImage: 'assets/images/avatars/av_client_3.jpeg',
      birthDate: '1995-05-15'
    },
    correlationId: 'cid-1'
  };

  beforeEach(async () => {
    authServiceSpy = {
      obtenerUsuarioAdmin: vi.fn().mockReturnValue(of(mockUserResponse)),
      actualizarUsuarioAdmin: vi.fn().mockReturnValue(of({
        success: true,
        message: 'Usuario actualizado correctamente',
        data: {
          ...mockUserResponse.data,
          profileImage: 'assets/images/avatars/av_client_1.jpeg'
        },
        correlationId: 'cid-2'
      }))
    };
    productsServiceSpy = {
      getCategories: vi.fn().mockReturnValue(of(categories)),
    };

    await TestBed.configureTestingModule({
      imports: [AdminUserDetail],
      providers: [
        provideRouter([{ path: 'admin/usuarios', component: AdminUserDetail }]),
        {
          provide: ActivatedRoute,
          useValue: {
            snapshot: {
              paramMap: {
                get: (key: string) => (key === 'id' ? 'user-123' : null)
              }
            }
          }
        },
        { provide: AuthService, useValue: authServiceSpy },
        { provide: ProductsService, useValue: productsServiceSpy }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(AdminUserDetail);
    component = fixture.componentInstance;
  });

  it('should create the component', () => {
    expect(component).toBeTruthy();
  });

  it('should load user data and initialize form and selected avatar', () => {
    fixture.detectChanges();

    expect(authServiceSpy.obtenerUsuarioAdmin).toHaveBeenCalledWith('user-123');
    expect(component.getEmail()).toBe('test@example.com');
    expect(component.getRole()).toBe('CLIENT');
    expect(component.isClient()).toBe(true);
    expect(component.selectedAvatar).toBe('assets/images/avatars/av_client_3.jpeg');
    expect(component.form.value.firstName).toBe('Carlos');
    expect(component.form.value.lastName).toBe('García');
    expect(component.form.value.nationalId).toBe('12345678A');
    expect(component.form.value.birthDate).toBe('1995-05-15');
    expect(component.cargando()).toBe(false);
  });

  it('should select a different avatar', () => {
    fixture.detectChanges();

    component.selectAvatar('assets/images/avatars/av_client_1.jpeg');
    expect(component.selectedAvatar).toBe('assets/images/avatars/av_client_1.jpeg');
  });

  it('should return seller avatars when user role is SELLER', () => {
    authServiceSpy.obtenerUsuarioAdmin.mockReturnValue(of({
      ...mockUserResponse,
      data: {
        ...mockUserResponse.data,
        role: 'SELLER',
        businessName: 'Tienda de Prueba',
        defaultCategory: 'Hogar',
        profileImage: 'assets/images/avatars/av_seller_2.jpeg'
      }
    }));

    fixture = TestBed.createComponent(AdminUserDetail);
    component = fixture.componentInstance;
    fixture.detectChanges();

    expect(component.isSeller()).toBe(true);
    expect(component.currentAvatarList).toEqual(component.sellerAvatars);
    expect(component.selectedAvatar).toBe('assets/images/avatars/av_seller_2.jpeg');
    expect(component.form.value.businessName).toBe('Tienda de Prueba');
    expect(component.form.value.defaultCategory).toBe('Hogar');
    expect(component.categories).toEqual(['Electrónica', 'Hogar', 'Moda']);
    expect(productsServiceSpy.getCategories).toHaveBeenCalled();
  });

  it('should require choosing a current category when the seller default no longer exists', () => {
    authServiceSpy.obtenerUsuarioAdmin.mockReturnValue(of({
      ...mockUserResponse,
      data: {
        ...mockUserResponse.data,
        role: 'SELLER',
        businessName: 'Tienda de Prueba',
        defaultCategory: 'Categoría antigua',
        profileImage: 'assets/images/avatars/av_seller_2.jpeg'
      }
    }));

    fixture = TestBed.createComponent(AdminUserDetail);
    component = fixture.componentInstance;
    fixture.detectChanges();

    expect(component.categories).toEqual(['Electrónica', 'Hogar', 'Moda']);
    expect(component.form.controls.defaultCategory.value).toBe('');
    expect(component.categorySelectionUnavailable()).toBe(true);
    expect(component.form.controls.defaultCategory.hasError('required')).toBe(true);
  });

  it('should submit form including selected avatar and navigate to user list with success message', () => {
    const router = TestBed.inject(Router);
    const navigateSpy = vi.spyOn(router, 'navigate');

    fixture.detectChanges();

    component.selectAvatar('assets/images/avatars/av_client_1.jpeg');
    component.onSubmit();

    expect(authServiceSpy.actualizarUsuarioAdmin).toHaveBeenCalledWith('user-123', {
      firstName: 'Carlos',
      lastName: 'García',
      nationalId: '12345678A',
      phone: '612345678',
      birthDate: '1995-05-15',
      profileImage: 'assets/images/avatars/av_client_1.jpeg'
    });
    expect(navigateSpy).toHaveBeenCalledWith(['/admin/usuarios'], {
      state: { successMessage: 'Los cambios se han guardado correctamente.' }
    });
    expect(component.guardando()).toBe(false);
  });

  it('should submit seller specific defaultCategory and businessName', () => {
    authServiceSpy.obtenerUsuarioAdmin.mockReturnValue(of({
      ...mockUserResponse,
      data: {
        ...mockUserResponse.data,
        role: 'SELLER',
        businessName: 'Mi Tienda',
        defaultCategory: 'Hogar',
        profileImage: 'assets/images/avatars/av_seller_1.jpeg'
      }
    }));

    fixture = TestBed.createComponent(AdminUserDetail);
    component = fixture.componentInstance;
    fixture.detectChanges();

    component.form.patchValue({ defaultCategory: 'Moda' });
    component.onSubmit();

    expect(authServiceSpy.actualizarUsuarioAdmin).toHaveBeenCalledWith('user-123', expect.objectContaining({
      defaultCategory: 'Moda',
      businessName: 'Mi Tienda',
      phone: '612345678'
    }));
  });

  it('should submit admin specific headquarters and omit phone', () => {
    const router = TestBed.inject(Router);
    vi.spyOn(router, 'navigate');

    authServiceSpy.obtenerUsuarioAdmin.mockReturnValue(of({
      ...mockUserResponse,
      data: {
        ...mockUserResponse.data,
        role: 'ADMIN',
        headquarters: 'Ciudad Real'
      }
    }));

    fixture = TestBed.createComponent(AdminUserDetail);
    component = fixture.componentInstance;
    fixture.detectChanges();

    component.form.patchValue({ headquarters: 'Toledo' });
    component.onSubmit();

    expect(authServiceSpy.actualizarUsuarioAdmin).toHaveBeenCalledWith('user-123', {
      firstName: 'Carlos',
      lastName: 'García',
      nationalId: '12345678A',
      headquarters: 'Toledo',
      profileImage: 'assets/images/avatars/av_client_3.jpeg'
    });
  });

  it('should invalidate client when birthDate is underage', () => {
    fixture.detectChanges();

    const youngYear = new Date().getFullYear() - 10;
    component.form.patchValue({ birthDate: `${youngYear}-01-01` });
    expect(component.form.controls.birthDate.hasError('underAge')).toBe(true);
    expect(component.form.invalid).toBe(true);
  });

  it('should invalidate form when nationalId is empty', () => {
    fixture.detectChanges();

    component.form.patchValue({ nationalId: '' });
    expect(component.form.controls.nationalId.hasError('required')).toBe(true);
    expect(component.form.invalid).toBe(true);
  });

  it('should invalidate seller form when businessName is too short or empty', () => {
    authServiceSpy.obtenerUsuarioAdmin.mockReturnValue(of({
      ...mockUserResponse,
      data: {
        ...mockUserResponse.data,
        role: 'SELLER',
        businessName: 'Mi Tienda',
        defaultCategory: 'Hogar'
      }
    }));

    fixture = TestBed.createComponent(AdminUserDetail);
    component = fixture.componentInstance;
    fixture.detectChanges();

    component.form.patchValue({ businessName: '' });
    expect(component.form.controls.businessName.hasError('required')).toBe(true);
    expect(component.form.invalid).toBe(true);

    component.form.patchValue({ businessName: 'A' });
    expect(component.form.controls.businessName.hasError('minlength')).toBe(true);
    expect(component.form.invalid).toBe(true);
  });

  it('should invalidate form when phone has invalid format', () => {
    fixture.detectChanges();

    component.form.patchValue({ phone: '123' });
    expect(component.form.controls.phone.hasError('pattern')).toBe(true);
    expect(component.form.invalid).toBe(true);
  });

  it('should display error message when loading user fails', () => {
    authServiceSpy.obtenerUsuarioAdmin.mockReturnValue(
      throwError(() => ({ status: 404 }))
    );

    fixture = TestBed.createComponent(AdminUserDetail);
    component = fixture.componentInstance;
    fixture.detectChanges();

    expect(component.errorMessage()).toBe('El usuario solicitado ya no existe.');
    expect(component.cargando()).toBe(false);
  });
});
