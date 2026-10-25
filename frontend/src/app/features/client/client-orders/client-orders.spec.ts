import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { describe, beforeEach, it, expect } from 'vitest';
import { ClientOrders } from './client-orders';
import { CartService } from '../../../core/services/cart.service';

describe('ClientOrders', () => {
  let component: ClientOrders;
  let fixture: ComponentFixture<ClientOrders>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ClientOrders],
      providers: [
        provideRouter([]),
        { provide: CartService, useValue: { itemCount: () => 0 } },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(ClientOrders);
    component = fixture.componentInstance;
    fixture.detectChanges();
    await fixture.whenStable();
  });

  it('should create and render navigation order in sidebar with Mis compras active', () => {
    expect(component).toBeTruthy();
    const el: HTMLElement = fixture.nativeElement;

    expect(el.querySelector('#orders-title')?.textContent).toContain('Mis compras');

    const navLinks = Array.from(el.querySelectorAll('.navigation-link'));
    expect(navLinks.length).toBe(5);
    expect(navLinks[0].textContent).toContain('Catálogo');
    expect(navLinks[1].textContent).toContain('Lista de deseos');
    expect(navLinks[2].textContent).toContain('Mis compras');
    expect(navLinks[2].classList.contains('is-active')).toBe(true);
    expect(navLinks[3].textContent).toContain('Carrito');
    expect(navLinks[4].textContent).toContain('Perfil');
  });

  it('should toggle sidebar collapsed state', () => {
    expect(component.sidebarCollapsed()).toBe(false);
    component.toggleSidebar();
    expect(component.sidebarCollapsed()).toBe(true);
    component.toggleSidebar();
    expect(component.sidebarCollapsed()).toBe(false);
  });
});
