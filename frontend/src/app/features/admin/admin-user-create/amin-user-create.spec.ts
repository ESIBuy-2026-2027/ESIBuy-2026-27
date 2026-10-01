import { ComponentFixture, TestBed } from '@angular/core/testing';
import { AdminUserCreate } from './admin-user-create';
import { AuthService } from '../../../core/services/auth.service';
import { of, throwError } from 'rxjs';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';

describe('AdminUserCreate', () => {
  let component: AdminUserCreate;
  let fixture: ComponentFixture<AdminUserCreate>;
  let authServiceSpy: any;

  const mockCandidatos = [
    {
      id: 'cand-1',
      nombre: 'Celia',
      apellidos: 'Nuñez',
      email: 'c@c.es',
      rol: 'SELLER',
      fechaRegistro: '2026-09-30T08:27:30.026Z',
      imagenPerfil: 'https://example.com/avatar1.svg'
    },
    {
      id: 'cand-2',
      nombre: 'Eustaquio',
      apellidos: 'Diat',
      email: 'eustaquio@example.com',
      rol: 'CLIENT',
      fechaRegistro: '2026-09-30T09:51:21.100Z',
      imagenPerfil: 'https://example.com/avatar2.svg'
    }
  ];

  beforeEach(async () => {
    authServiceSpy = {
      getCandidatos: vi.fn().mockReturnValue(of(mockCandidatos)),
      promoverAAdmin: vi.fn()
    };

    await TestBed.configureTestingModule({
      imports: [AdminUserCreate],
      providers: [
        { provide: AuthService, useValue: authServiceSpy }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(AdminUserCreate);
    component = fixture.componentInstance;
  });

  it('should create the component', () => {
    expect(component).toBeTruthy();
  });

  it('should load candidates and render them in the dropdown', async () => {
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();

    expect(authServiceSpy.getCandidatos).toHaveBeenCalled();
    expect(component.candidatos.length).toBe(2);

    const selectEl = fixture.nativeElement.querySelector('#candidatoSelect') || fixture.nativeElement.querySelector('#selectorCandidatos');
    expect(selectEl).not.toBeNull();
    const options = selectEl.querySelectorAll('option');
    // 1 placeholder option + 2 candidates = 3 options
    expect(options.length).toBe(3);
    expect(options[1].textContent).toContain('Celia Nuñez');
    expect(options[2].textContent).toContain('Eustaquio Diat');
  });

  it('should render candidates when response arrives asynchronously without manual click', async () => {
    const { Subject } = await import('rxjs');
    const asyncSubject = new Subject<any[]>();
    authServiceSpy.getCandidatos.mockReturnValue(asyncSubject.asObservable());

    const asyncFixture = TestBed.createComponent(AdminUserCreate);
    asyncFixture.detectChanges(); // initial render (loading)

    // Emit asynchronously (component calls cdr.detectChanges() in next callback)
    asyncSubject.next(mockCandidatos);
    asyncSubject.complete();

    const selectEl = asyncFixture.nativeElement.querySelector('#selectorCandidatos') || asyncFixture.nativeElement.querySelector('#candidatoSelect');
    const options = selectEl.querySelectorAll('option');
    expect(options.length).toBe(3);
    expect(options[1].textContent).toContain('Celia Nuñez');
    expect(options[2].textContent).toContain('Eustaquio Diat');
  });

  it('should select candidate and populate form fields and original registration date', async () => {
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();

    const selectEl = fixture.nativeElement.querySelector('#selectorCandidatos') || fixture.nativeElement.querySelector('#candidatoSelect');
    selectEl.value = 'cand-1';
    selectEl.dispatchEvent(new Event('change'));
    fixture.detectChanges();

    expect(component.candidatoSeleccionado).not.toBeNull();
    expect(component.candidatoSeleccionado?.id).toBe('cand-1');
    expect(component.form.get('nombre')?.value).toBe('Celia');
    expect(component.form.get('apellidos')?.value).toBe('Nuñez');
    expect(component.form.get('email')?.value).toBe('c@c.es');
    expect(component.form.get('fechaIncorporacion')?.value).toBe('2026-09-30');
  });
});