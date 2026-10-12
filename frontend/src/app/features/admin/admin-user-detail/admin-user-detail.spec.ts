import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';
import { AdminUserDetail } from './admin-user-detail';
import { AuthService, ApiResponse, UserResponse } from '../../../core/services/auth.service';

describe('AdminUserDetail', () => {
  let component: AdminUserDetail;
  let fixture: ComponentFixture<AdminUserDetail>;
  let authServiceSpy: any;

  const mockUserResponse: ApiResponse<UserResponse> = {
    success: true,
    message: 'Usuario cargado',
    data: {
      id: 'user-123',
      email: 'test@example.com',
      firstName: 'Carlos',
      lastName: 'García',
      phone: '612345678',
      role: 'CLIENT',
      profileImage: 'assets/images/avatars/av_client_3.jpeg'
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

    await TestBed.configureTestingModule({
      imports: [AdminUserDetail],
      providers: [
        provideRouter([]),
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
        { provide: AuthService, useValue: authServiceSpy }
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
    expect(component.selectedAvatar).toBe('assets/images/avatars/av_client_3.jpeg');
    expect(component.form.value.firstName).toBe('Carlos');
    expect(component.form.value.lastName).toBe('García');
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
        profileImage: 'assets/images/avatars/av_seller_2.jpeg'
      }
    }));

    fixture = TestBed.createComponent(AdminUserDetail);
    component = fixture.componentInstance;
    fixture.detectChanges();

    expect(component.currentAvatarList).toEqual(component.sellerAvatars);
    expect(component.selectedAvatar).toBe('assets/images/avatars/av_seller_2.jpeg');
  });

  it('should submit form including selected avatar', () => {
    fixture.detectChanges();

    component.selectAvatar('assets/images/avatars/av_client_1.jpeg');
    component.onSubmit();

    expect(authServiceSpy.actualizarUsuarioAdmin).toHaveBeenCalledWith('user-123', {
      firstName: 'Carlos',
      lastName: 'García',
      phone: '612345678',
      profileImage: 'assets/images/avatars/av_client_1.jpeg'
    });
    expect(component.successMessage()).toBe('Usuario actualizado correctamente');
    expect(component.guardando()).toBe(false);
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
