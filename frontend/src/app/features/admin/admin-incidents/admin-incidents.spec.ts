import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { AdminIncidentService, Incident } from '../../../core/services/admin-incident.service';
import { AdminIncidents } from './admin-incidents';

describe('AdminIncidents', () => {
  let incidentService: {
    create: ReturnType<typeof vi.fn>;
    getAll: ReturnType<typeof vi.fn>;
    resolve: ReturnType<typeof vi.fn>;
  };

  const sampleIncident: Incident = {
    id: 'inc-99',
    subject: 'Error de prueba',
    category: 'PEDIDO',
    reference: 'PED-111',
    description: 'Descripción de prueba detallada',
    status: 'PENDIENTE',
    createdBy: 'admin@esibuy.com',
    createdAt: '2026-10-28T12:00:00Z',
  };

  beforeEach(async () => {
    incidentService = {
      create: vi.fn().mockReturnValue(of(sampleIncident)),
      getAll: vi.fn().mockReturnValue(of([])),
      resolve: vi.fn(),
    };

    await TestBed.configureTestingModule({
      imports: [AdminIncidents],
      providers: [
        provideRouter([]),
        { provide: AdminIncidentService, useValue: incidentService },
      ],
    }).compileComponents();
  });

  it('should create the component and have initial clean state', () => {
    const fixture = TestBed.createComponent(AdminIncidents);
    const component = fixture.componentInstance;
    fixture.detectChanges();

    expect(component).toBeTruthy();
    expect(component.subject()).toBe('');
    expect(component.category()).toBe('');
    expect(component.reference()).toBe('');
    expect(component.description()).toBe('');
    expect(component.totalCount()).toBe(0);
    expect(component.pendingCount()).toBe(0);
    expect(component.resolvedCount()).toBe(0);
  });

  it('should validate required fields before submission', () => {
    const fixture = TestBed.createComponent(AdminIncidents);
    const component = fixture.componentInstance;
    fixture.detectChanges();

    // 1. Missing subject
    component.onSubmit();
    expect(component.mensajeError()).toContain('indica el asunto');
    expect(incidentService.create).not.toHaveBeenCalled();

    // 2. Missing category
    component.subject.set('Problema con stock');
    component.onSubmit();
    expect(component.mensajeError()).toContain('selecciona una categoría');
    expect(incidentService.create).not.toHaveBeenCalled();

    // 3. Missing description
    component.category.set('PRODUCTO');
    component.onSubmit();
    expect(component.mensajeError()).toContain('describe con detalle');
    expect(incidentService.create).not.toHaveBeenCalled();
  });

  it('should register incident successfully, clear the form, and show success message', () => {
    const fixture = TestBed.createComponent(AdminIncidents);
    const component = fixture.componentInstance;
    fixture.detectChanges();

    component.subject.set('Error en pasarela');
    component.category.set('PAGO');
    component.reference.set('PED-999');
    component.description.set('Cobro fallido');

    component.onSubmit();

    expect(incidentService.create).toHaveBeenCalledWith({
      subject: 'Error en pasarela',
      category: 'PAGO',
      reference: 'PED-999',
      description: 'Cobro fallido',
    });

    expect(component.mensajeExito()).toContain('registrada correctamente');
    expect(component.mensajeError()).toBe('');
    expect(component.subject()).toBe('');
    expect(component.category()).toBe('');
    expect(component.reference()).toBe('');
    expect(component.description()).toBe('');
    expect(component.incidents()).toContainEqual(sampleIncident);
    expect(component.totalCount()).toBe(1);
    expect(component.pendingCount()).toBe(1);
    expect(component.resolvedCount()).toBe(0);
  });

  it('should display error message if incident creation fails', () => {
    const fixture = TestBed.createComponent(AdminIncidents);
    const component = fixture.componentInstance;
    fixture.detectChanges();

    incidentService.create.mockReturnValue(
      throwError(() => ({
        error: { message: 'No se pudo registrar la incidencia en el servidor.' },
      }))
    );

    component.subject.set('Fallo de servidor');
    component.category.set('SISTEMA');
    component.description.set('El servidor responde 500');

    component.onSubmit();

    expect(component.mensajeError()).toBe('No se pudo registrar la incidencia en el servidor.');
    expect(component.submitting()).toBe(false);
  });

  it('should toggle sidebar correctly', () => {
    const fixture = TestBed.createComponent(AdminIncidents);
    const component = fixture.componentInstance;

    expect(component.sidebarCollapsed()).toBe(false);
    component.toggleSidebar();
    expect(component.sidebarCollapsed()).toBe(true);
    component.toggleSidebar();
    expect(component.sidebarCollapsed()).toBe(false);
  });

  it('does not render a duplicate brand logo in the sidebar', () => {
    const fixture = TestBed.createComponent(AdminIncidents);
    fixture.detectChanges();
    const rendered = fixture.nativeElement as HTMLElement;
    expect(rendered.querySelector('.admin-brand')).toBeNull();
  });

  it('should load incidents on init and render rows and calculate summary cards correctly', () => {
    const list: Incident[] = [
      {
        id: 'inc-101',
        subject: 'Fallo de inventario',
        category: 'PRODUCTO',
        reference: 'PROD-5',
        description: 'Stock desincronizado',
        status: 'PENDIENTE',
        createdBy: 'admin@esibuy.com',
        createdAt: '2026-10-28T09:00:00Z',
      },
      {
        id: 'inc-102',
        subject: 'Error de acceso',
        category: 'USUARIO',
        description: 'Bloqueo temporal',
        status: 'RESUELTA',
        createdBy: 'admin@esibuy.com',
        createdAt: '2026-10-28T08:00:00Z',
        resolvedAt: '2026-10-28T08:30:00Z',
      },
    ];

    incidentService.getAll.mockReturnValue(of(list));

    const fixture = TestBed.createComponent(AdminIncidents);
    fixture.detectChanges();
    const component = fixture.componentInstance;

    expect(incidentService.getAll).toHaveBeenCalled();
    expect(component.incidents()).toEqual(list);
    expect(component.totalCount()).toBe(2);
    expect(component.pendingCount()).toBe(1);
    expect(component.resolvedCount()).toBe(1);

    const el = fixture.nativeElement as HTMLElement;
    expect(el.querySelector('[data-testid="total-count"]')?.textContent?.trim()).toBe('2');
    expect(el.querySelector('[data-testid="pending-count"]')?.textContent?.trim()).toBe('1');
    expect(el.querySelector('[data-testid="resolved-count"]')?.textContent?.trim()).toBe('1');

    const rows = el.querySelectorAll('.incident-row');
    expect(rows.length).toBe(2);
    expect(rows[0].textContent).toContain('Fallo de inventario');
    expect(rows[0].textContent).toContain('PENDIENTE');
    expect(rows[1].textContent).toContain('Error de acceso');
    expect(rows[1].textContent).toContain('RESUELTA');
  });

  it('should render empty state when no incidents exist', () => {
    incidentService.getAll.mockReturnValue(of([]));

    const fixture = TestBed.createComponent(AdminIncidents);
    fixture.detectChanges();
    const el = fixture.nativeElement as HTMLElement;

    expect(el.querySelector('[data-testid="empty-incidents-state"]')).toBeTruthy();
    expect(el.textContent).toContain('No hay incidencias registradas');
  });

  it('should show error banner when loading incidents fails', () => {
    incidentService.getAll.mockReturnValue(throwError(() => new Error('Server error')));

    const fixture = TestBed.createComponent(AdminIncidents);
    fixture.detectChanges();
    const component = fixture.componentInstance;
    const el = fixture.nativeElement as HTMLElement;

    expect(component.loadError()).toBe(true);
    expect(el.querySelector('[data-testid="load-error-alert"]')).toBeTruthy();
    expect(el.textContent).toContain('No se pudieron cargar las incidencias desde el servidor');
  });

  it('should resolve an incident on button click, update row and counters, and show success message', () => {
    const pendingItem: Incident = {
      id: 'inc-300',
      subject: 'Problema de acceso admin',
      category: 'USUARIO',
      description: 'El admin no puede entrar',
      status: 'PENDIENTE',
      createdBy: 'admin@esibuy.com',
      createdAt: '2026-10-28T14:00:00Z',
    };

    const resolvedItem: Incident = {
      ...pendingItem,
      status: 'RESUELTA',
      resolvedAt: '2026-10-28T14:30:00Z',
    };

    incidentService.getAll.mockReturnValue(of([pendingItem]));
    incidentService.resolve.mockReturnValue(of(resolvedItem));

    const fixture = TestBed.createComponent(AdminIncidents);
    fixture.detectChanges();
    const component = fixture.componentInstance;
    const el = fixture.nativeElement as HTMLElement;

    expect(component.pendingCount()).toBe(1);
    expect(component.resolvedCount()).toBe(0);

    const resolveBtn = el.querySelector<HTMLButtonElement>('.btn-resolve');
    expect(resolveBtn).toBeTruthy();
    expect(resolveBtn?.textContent).toContain('Marcar como resuelta');

    resolveBtn?.click();
    fixture.detectChanges();

    expect(incidentService.resolve).toHaveBeenCalledWith('inc-300');
    expect(component.incidents()[0].status).toBe('RESUELTA');
    expect(component.pendingCount()).toBe(0);
    expect(component.resolvedCount()).toBe(1);
    expect(component.mensajeExito()).toContain('marcada como resuelta');
    expect(el.querySelector('.resolved-cell')?.textContent).toContain('Resuelta');
  });

  it('should show error message if resolving an incident fails', () => {
    const pendingItem: Incident = {
      id: 'inc-400',
      subject: 'Fallo pasarela',
      category: 'PAGO',
      description: 'Error en webhook',
      status: 'PENDIENTE',
      createdBy: 'admin@esibuy.com',
      createdAt: '2026-10-28T14:00:00Z',
    };

    incidentService.getAll.mockReturnValue(of([pendingItem]));
    incidentService.resolve.mockReturnValue(
      throwError(() => ({
        error: { message: 'No se pudo resolver la incidencia en el servidor.' },
      }))
    );

    const fixture = TestBed.createComponent(AdminIncidents);
    fixture.detectChanges();
    const component = fixture.componentInstance;

    component.resolveIncident(pendingItem);
    fixture.detectChanges();

    expect(component.mensajeError()).toBe('No se pudo resolver la incidencia en el servidor.');
    expect(component.incidents()[0].status).toBe('PENDIENTE');
    expect(component.resolvingId()).toBeNull();
  });
});

