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
});

