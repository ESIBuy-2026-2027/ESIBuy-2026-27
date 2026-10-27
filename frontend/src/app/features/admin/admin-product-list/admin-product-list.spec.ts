import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';
import { HttpErrorResponse } from '@angular/common/http';
import { describe, beforeEach, it, expect, vi } from 'vitest';
import { AdminProductList } from './admin-product-list';
import {
  AdminProductService,
  AdminProductSummary,
  ADMIN_READONLY_ERROR_MESSAGE,
} from '../../../core/services/admin-product.service';
import { ProductsService, Category } from '../../../core/services/products.service';

describe('AdminProductList', () => {
  let component: AdminProductList;
  let fixture: ComponentFixture<AdminProductList>;
  let adminProductServiceMock: any;
  let productsServiceMock: any;

  const mockProducts: AdminProductSummary[] = [
    {
      id: 'prod-1',
      name: 'Monitor UltraWide 34"',
      description: 'Resolución WQHD 144Hz',
      price: 499.99,
      stock: 12,
      category: 'Monitores',
      sellerId: 'seller-abc',
      sellerName: 'Pantallas Pro',
      active: true,
    },
    {
      id: 'prod-2',
      name: 'Teclado Mecánico RGB',
      description: 'Switches ópticos lineales',
      price: 89.5,
      stock: 0,
      category: 'Periféricos',
      sellerId: 'seller-xyz',
      sellerName: 'Gaming Zone',
      active: false,
    },
  ];

  const mockCategories: Category[] = [
    { id: 'cat-1', name: 'Monitores' },
    { id: 'cat-2', name: 'Periféricos' },
  ];

  beforeEach(async () => {
    adminProductServiceMock = {
      getProducts: vi.fn(() => of(mockProducts)),
      deleteProductAsAdmin: vi.fn(() =>
        throwError(() => new HttpErrorResponse({ status: 403, statusText: 'Forbidden' }))
      ),
    };

    productsServiceMock = {
      getCategories: vi.fn(() => of(mockCategories)),
    };

    await TestBed.configureTestingModule({
      imports: [AdminProductList],
      providers: [
        provideRouter([]),
        { provide: AdminProductService, useValue: adminProductServiceMock },
        { provide: ProductsService, useValue: productsServiceMock },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(AdminProductList);
    component = fixture.componentInstance;
    fixture.detectChanges();
    await fixture.whenStable();
  });

  it('should create the component and load products & categories on init', () => {
    expect(component).toBeTruthy();
    expect(adminProductServiceMock.getProducts).toHaveBeenCalled();
    expect(productsServiceMock.getCategories).toHaveBeenCalled();
    expect(component.products().length).toBe(2);
    expect(component.categories().length).toBe(2);
  });

  it('should display the read-only supervision banner with exact text "Modo supervisión: solo lectura"', () => {
    const el: HTMLElement = fixture.nativeElement;
    const banner = el.querySelector('.readonly-banner');
    expect(banner).not.toBeNull();
    expect(banner?.textContent).toContain('Modo supervisión: solo lectura');
  });

  it('should render product table with seller name, product name, price, stock, and status', () => {
    const el: HTMLElement = fixture.nativeElement;
    const rows = el.querySelectorAll('.products-table tbody tr');
    expect(rows.length).toBe(2);

    const firstRowText = rows[0].textContent;
    expect(firstRowText).toContain('Monitor UltraWide 34"');
    expect(firstRowText).toContain('Pantallas Pro');
    expect(firstRowText).toContain('Monitores');
    expect(firstRowText).toContain('499,99');
    expect(firstRowText).toContain('12 uds.');
    expect(firstRowText).toContain('Activo');

    const secondRowText = rows[1].textContent;
    expect(secondRowText).toContain('Teclado Mecánico RGB');
    expect(secondRowText).toContain('Gaming Zone');
    expect(secondRowText).toContain('Agotado');
    expect(secondRowText).toContain('Inactivo');
  });

  it('should strictly NOT render any interactive mutation or purchase buttons', () => {
    const el: HTMLElement = fixture.nativeElement;

    // Verify there are no purchase, favorite, rating or edit buttons anywhere in the DOM
    const buttons = Array.from(el.querySelectorAll('button')).map((b) => b.textContent?.trim());
    expect(buttons.some((b) => b?.toLowerCase().includes('comprar'))).toBe(false);
    expect(buttons.some((b) => b?.toLowerCase().includes('carrito'))).toBe(false);
    expect(buttons.some((b) => b?.toLowerCase().includes('favorito'))).toBe(false);
    expect(buttons.some((b) => b?.toLowerCase().includes('editar'))).toBe(false);
    expect(buttons.some((b) => b?.toLowerCase().includes('eliminar'))).toBe(false);

    // Only "Ver detalle" read-only link should exist
    const detailLinks = el.querySelectorAll('.detail-link');
    expect(detailLinks.length).toBe(2);
  });

  it('should filter products locally by search term matching name or seller', () => {
    component.searchTerm.set('gaming');
    fixture.detectChanges();

    expect(component.filteredProducts().length).toBe(1);
    expect(component.filteredProducts()[0].name).toBe('Teclado Mecánico RGB');

    component.searchTerm.set('ultrawide');
    fixture.detectChanges();
    expect(component.filteredProducts().length).toBe(1);
    expect(component.filteredProducts()[0].sellerName).toBe('Pantallas Pro');
  });

  it('should filter products by stock status', () => {
    component.stockFilter.set('OUT_OF_STOCK');
    fixture.detectChanges();

    expect(component.filteredProducts().length).toBe(1);
    expect(component.filteredProducts()[0].stock).toBe(0);

    component.stockFilter.set('IN_STOCK');
    fixture.detectChanges();
    expect(component.filteredProducts().length).toBe(1);
    expect(component.filteredProducts()[0].stock).toBe(12);
  });

  it('should toggle sidebar collapsed state', () => {
    expect(component.sidebarCollapsed()).toBe(false);
    component.toggleSidebar();
    expect(component.sidebarCollapsed()).toBe(true);
    component.toggleSidebar();
    expect(component.sidebarCollapsed()).toBe(false);
  });

  it('should handle 403 Forbidden and display the specific user-facing message', () => {
    adminProductServiceMock.getProducts.mockReturnValue(
      throwError(() => new HttpErrorResponse({ status: 403, statusText: 'Forbidden' }))
    );

    component.loadProducts();
    fixture.detectChanges();

    expect(component.error()).toBe(ADMIN_READONLY_ERROR_MESSAGE);
    const el: HTMLElement = fixture.nativeElement;
    expect(el.querySelector('.error-alert')?.textContent).toContain(ADMIN_READONLY_ERROR_MESSAGE);
  });

  it('should display error message when attemptUnauthorizedAction encounters 403', () => {
    component.attemptUnauthorizedAction('prod-1');
    fixture.detectChanges();

    expect(component.error()).toBe(ADMIN_READONLY_ERROR_MESSAGE);
  });

  it('should cycle through available sellers when clicking the seller header', () => {
    expect(component.filteredProducts().length).toBe(2);

    // 1st click: switches to first seller ('seller-abc' / 'Pantallas Pro')
    component.cycleSellerFilter();
    fixture.detectChanges();
    expect(component.sellerFilter()).toBe('seller-abc');
    expect(component.filteredProducts().length).toBe(1);
    expect(component.filteredProducts()[0].sellerName).toBe('Pantallas Pro');

    // 2nd click: switches to second seller ('seller-xyz' / 'Gaming Zone')
    component.cycleSellerFilter();
    fixture.detectChanges();
    expect(component.sellerFilter()).toBe('seller-xyz');
    expect(component.filteredProducts().length).toBe(1);
    expect(component.filteredProducts()[0].sellerName).toBe('Gaming Zone');

    // 3rd click: resets to 'ALL' (shows all sellers)
    component.cycleSellerFilter();
    fixture.detectChanges();
    expect(component.sellerFilter()).toBe('ALL');
    expect(component.filteredProducts().length).toBe(2);
  });

  it('should filter directly by seller and allow clearing the filter', () => {
    component.setSellerFilter('seller-xyz');
    fixture.detectChanges();

    expect(component.filteredProducts().length).toBe(1);
    expect(component.filteredProducts()[0].sellerName).toBe('Gaming Zone');

    component.clearSellerFilter();
    fixture.detectChanges();

    expect(component.sellerFilter()).toBe('ALL');
    expect(component.filteredProducts().length).toBe(2);
  });
});

