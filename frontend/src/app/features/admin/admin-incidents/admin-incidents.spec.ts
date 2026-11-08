import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { beforeEach, describe, expect, it } from 'vitest';
import { AdminIncidents, MOCK_INCIDENTS_PREVIEW } from './admin-incidents';

describe('AdminIncidents (Módulo en desarrollo)', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AdminIncidents],
      providers: [provideRouter([])],
    }).compileComponents();
  });

  it('should create the component and have under-development state', () => {
    const fixture = TestBed.createComponent(AdminIncidents);
    const component = fixture.componentInstance;
    fixture.detectChanges();

    expect(component).toBeTruthy();
    expect(component.isUnderDevelopment()).toBe(true);
    expect(component.totalCount()).toBe(MOCK_INCIDENTS_PREVIEW.length);
    expect(component.pendingCount()).toBe(1);
    expect(component.resolvedCount()).toBe(2);
  });

  it('should render the under-development card with title and badge', () => {
    const fixture = TestBed.createComponent(AdminIncidents);
    fixture.detectChanges();
    const el = fixture.nativeElement as HTMLElement;

    const overlay = el.querySelector('.under-development-overlay');
    expect(overlay).toBeTruthy();

    const title = el.querySelector('.development-title');
    expect(title?.textContent).toContain('Gestión de incidencias en construcción');

    const badge = el.querySelector('.development-badge');
    expect(badge?.textContent).toContain('PRÓXIMAMENTE • SPRINT FUTURO');

    const desc = el.querySelector('.development-description');
    expect(desc?.textContent).toContain('Este módulo no forma parte del sprint actual');
  });

  it('should render a link to return to /admin/usuarios', () => {
    const fixture = TestBed.createComponent(AdminIncidents);
    fixture.detectChanges();
    const el = fixture.nativeElement as HTMLElement;

    const returnLink = el.querySelector<HTMLAnchorElement>('.btn-return-admin');
    expect(returnLink).toBeTruthy();
    expect(returnLink?.getAttribute('routerLink') ?? returnLink?.getAttribute('href')).toMatch(/admin\/usuarios/);
  });

  it('should render the blurred preview container marked as non-interactive', () => {
    const fixture = TestBed.createComponent(AdminIncidents);
    fixture.detectChanges();
    const el = fixture.nativeElement as HTMLElement;

    const preview = el.querySelector('.incidents-blurred-preview');
    expect(preview).toBeTruthy();
    expect(preview?.getAttribute('aria-hidden')).toBe('true');
    expect(preview?.hasAttribute('inert')).toBe(true);
  });

  it('should display preview stat counters and mock incident rows under the blur', () => {
    const fixture = TestBed.createComponent(AdminIncidents);
    fixture.detectChanges();
    const el = fixture.nativeElement as HTMLElement;

    expect(el.querySelector('[data-testid="total-count"]')?.textContent?.trim()).toBe('3');
    expect(el.querySelector('[data-testid="pending-count"]')?.textContent?.trim()).toBe('1');
    expect(el.querySelector('[data-testid="resolved-count"]')?.textContent?.trim()).toBe('2');

    const rows = el.querySelectorAll('.incident-row');
    expect(rows.length).toBe(3);
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
