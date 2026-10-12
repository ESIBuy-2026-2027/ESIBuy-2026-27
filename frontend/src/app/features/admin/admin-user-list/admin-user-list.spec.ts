import { ComponentFixture, TestBed } from '@angular/core/testing';
import { AdminUserList } from './admin-user-list';
import { AuthService, UserAdmin } from '../../../core/services/auth.service';
import { of } from 'rxjs';
import { provideRouter } from '@angular/router';

describe('AdminUserList', () => {
  let component: AdminUserList;
  let fixture: ComponentFixture<AdminUserList>;
  let authServiceSpy: any;

  const mockUsers: UserAdmin[] = [
    {
      id: '1',
      firstName: 'Tomas',
      lastName: 'Alarma',
      email: 'tomas@example.com',
      nationalId: '12345678A',
      phone: '600111222',
      role: 'ADMIN',
      active: true,
      profileImage: 'assets/images/avatars/av_all.jpeg',
      createdAt: '2026-09-29T20:46:58.860Z',
      adminProfile: { headquarters: 'Ciudad Real', hireDate: '2026-09-29T20:46:58.86' }
    },
    {
      id: '2',
      firstName: 'Celia',
      lastName: 'Nuñez',
      email: 'celia@example.com',
      nationalId: '87654321B',
      phone: '600333444',
      role: 'SELLER',
      active: false,
      profileImage: 'https://example.com/avatar.svg',
      createdAt: '2026-09-30T08:27:30.026Z'
    }
  ];

  beforeEach(async () => {
    authServiceSpy = {
      obtenerTodosLosUsuarios: vi.fn().mockReturnValue(of(mockUsers))
    };

    await TestBed.configureTestingModule({
      imports: [AdminUserList],
      providers: [
        provideRouter([]),
        { provide: AuthService, useValue: authServiceSpy }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(AdminUserList);
    component = fixture.componentInstance;
  });

  it('should create the component', () => {
    expect(component).toBeTruthy();
  });

  it('should load and render users in table and not show loading spinner', async () => {
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();

    expect(authServiceSpy.obtenerTodosLosUsuarios).toHaveBeenCalled();
    expect(component.cargando()).toBe(false);
    expect(component.usuarios().length).toBe(2);

    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelector('.loading-spinner')).toBeNull();

    const rows = compiled.querySelectorAll('.user-table tbody tr');
    expect(rows.length).toBe(2);
    expect(compiled.textContent).toContain('Tomas Alarma');
    expect(compiled.textContent).toContain('tomas@example.com');
    expect(compiled.textContent).toContain('Celia Nuñez');
    expect(compiled.textContent).toContain('celia@example.com');
  });

  it('should have a button to navigate to /admin/altas', async () => {
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();

    const compiled = fixture.nativeElement as HTMLElement;
    const btnAlta = compiled.querySelector('#btnDarAltaAdmin') as HTMLAnchorElement;
    expect(btnAlta).not.toBeNull();
    expect(btnAlta.textContent).toContain('Dar Alta Administrador');
    expect(btnAlta.getAttribute('routerLink') || btnAlta.getAttribute('href')).toContain('/admin/altas');
  });

  it('should display success alert when mensajeExito is set', () => {
    component.mensajeExito.set('Los cambios se han guardado correctamente.');
    fixture.detectChanges();

    const compiled = fixture.nativeElement as HTMLElement;
    const alert = compiled.querySelector('.success-alert');
    expect(alert).not.toBeNull();
    expect(alert?.textContent).toContain('Los cambios se han guardado correctamente.');
  });
});
