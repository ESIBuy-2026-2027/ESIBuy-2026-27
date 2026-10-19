import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { describe, beforeEach, it, expect } from 'vitest';
import { ClientDashboard } from './client-dashboard';

describe('ClientDashboard', () => {
  let component: ClientDashboard;
  let fixture: ComponentFixture<ClientDashboard>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ClientDashboard],
      providers: [provideRouter([])],
    }).compileComponents();

    fixture = TestBed.createComponent(ClientDashboard);
    component = fixture.componentInstance;
    fixture.detectChanges();
    await fixture.whenStable();
  });

  it('should create and render navigation links in exact order with Mi panel active', () => {
    expect(component).toBeTruthy();
    const el: HTMLElement = fixture.nativeElement;

    expect(el.querySelector('#client-title')?.textContent).toContain('Panel de control de cliente');
    expect(el.querySelector('.placeholder-title')?.textContent).toContain('Área de cliente en desarrollo');

    const navLinks = Array.from(el.querySelectorAll('.navigation-link'));
    expect(navLinks.length).toBe(5);
    expect(navLinks[0].textContent).toContain('Mi catálogo');
    expect(navLinks[1].textContent).toContain('Mi panel');
    expect(navLinks[1].classList.contains('is-active')).toBe(true);
    expect(navLinks[2].textContent).toContain('Lista de deseos');
    expect(navLinks[3].textContent).toContain('Mis compras');
    expect(navLinks[3].classList.contains('is-disabled')).toBe(true);
    expect(navLinks[4].textContent).toContain('Mi perfil');
  });

  it('should render action to explore catalog', () => {
    const el: HTMLElement = fixture.nativeElement;
    const exploreBtn = el.querySelector('.primary-action');
    expect(exploreBtn).not.toBeNull();
    expect(exploreBtn?.textContent).toContain('Explorar catálogo');
  });
});
