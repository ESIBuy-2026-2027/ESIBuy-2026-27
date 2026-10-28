import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of } from 'rxjs';
import { PedidoService, SellerOrder } from '../../../core/services/pedido.service';
import { SellerOrders } from './seller-orders';

describe('SellerOrders', () => {
  let fixture: ComponentFixture<SellerOrders>;
  let component: SellerOrders;
  const mockOrders: SellerOrder[] = [
    {
      pedidoId: 'PED-101',
      fechaCreacion: '2026-10-15T10:30:00Z',
      clienteNombre: 'Juan Pérez',
      clienteEmail: 'juan@example.com',
      direccionEnvio: 'Calle Mayor 1, Madrid',
      items: [
        {
          productoId: 'prod-1',
          nombre: 'Camiseta de algodón',
          cantidad: 2,
          precioUnitario: 15.0,
          subtotal: 30.0,
          imageUrl: null,
          categoryIds: [],
        },
      ],
      total: 30.0,
      estado: 'PENDIENTE',
    },
    {
      pedidoId: 'PED-102',
      fechaCreacion: '2026-10-20T14:00:00Z',
      clienteNombre: 'María López',
      clienteEmail: 'maria@example.com',
      direccionEnvio: 'Avenida América 5, Barcelona',
      items: [
        {
          productoId: 'prod-2',
          nombre: 'Zapatos de cuero',
          cantidad: 1,
          precioUnitario: 50.0,
          subtotal: 50.0,
          imageUrl: null,
          categoryIds: [],
        },
      ],
      total: 50.0,
      estado: 'ENVIADO',
    },
  ];

  const pedidoServiceMock = {
    obtenerPedidosDelVendedor: () => of(mockOrders),
    actualizarEstadoPedido: () => of(void 0),
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [SellerOrders],
      providers: [
        provideRouter([]),
        { provide: PedidoService, useValue: pedidoServiceMock },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(SellerOrders);
    component = fixture.componentInstance;
    fixture.detectChanges();
    await fixture.whenStable();
  });

  it('should create the component', () => {
    expect(component).toBeTruthy();
  });

  it('should render standard filters bar with search field and filter controls', () => {
    const el = fixture.nativeElement as HTMLElement;

    const filtersBar = el.querySelector('.filters-bar');
    expect(filtersBar).toBeTruthy();

    const searchField = filtersBar?.querySelector('.search-field');
    expect(searchField).toBeTruthy();
    const searchInput = searchField?.querySelector('input[type="search"]') as HTMLInputElement;
    expect(searchInput).toBeTruthy();
    expect(searchInput.placeholder).toContain('Buscar por ID, cliente o producto...');

    const filterControls = filtersBar?.querySelector('.filter-controls');
    expect(filterControls).toBeTruthy();

    const statusSelect = filterControls?.querySelector('#order-status-filter') as HTMLSelectElement;
    expect(statusSelect).toBeTruthy();

    const dateGroup = filterControls?.querySelector('.date-filter-group');
    expect(dateGroup).toBeTruthy();
    expect(dateGroup?.querySelector('#order-date-from')).toBeTruthy();
    expect(dateGroup?.querySelector('#order-date-to')).toBeTruthy();

    const orderCount = filterControls?.querySelector('.order-count');
    expect(orderCount?.textContent).toContain('2 pedidos');
  });

  it('should filter orders when searching by customer, ID or product', () => {
    component.orderSearch.set('Juan');
    fixture.detectChanges();

    expect(component.visibleOrders().length).toBe(1);
    expect(component.visibleOrders()[0].id).toBe('PED-101');

    component.orderSearch.set('Zapatos');
    fixture.detectChanges();

    expect(component.visibleOrders().length).toBe(1);
    expect(component.visibleOrders()[0].id).toBe('PED-102');

    component.orderSearch.set('inexistente');
    fixture.detectChanges();

    expect(component.visibleOrders().length).toBe(0);
  });

  it('should filter orders by status', () => {
    component.selectedStatus.set('Pendiente');
    fixture.detectChanges();

    expect(component.visibleOrders().length).toBe(1);
    expect(component.visibleOrders()[0].id).toBe('PED-101');

    component.selectedStatus.set('Enviado');
    fixture.detectChanges();

    expect(component.visibleOrders().length).toBe(1);
    expect(component.visibleOrders()[0].id).toBe('PED-102');

    component.selectedStatus.set('Cancelado');
    fixture.detectChanges();

    expect(component.visibleOrders().length).toBe(0);
  });

  it('should filter orders by date range', () => {
    component.dateFrom.set('2026-10-18');
    fixture.detectChanges();

    expect(component.visibleOrders().length).toBe(1);
    expect(component.visibleOrders()[0].id).toBe('PED-102');

    component.dateFrom.set('2026-10-01');
    component.dateTo.set('2026-10-16');
    fixture.detectChanges();

    expect(component.visibleOrders().length).toBe(1);
    expect(component.visibleOrders()[0].id).toBe('PED-101');
  });

  it('should display reset filter button when filters are active and clear them on click', () => {
    const el = fixture.nativeElement as HTMLElement;

    expect(component.hasActiveFilters()).toBe(false);
    expect(el.querySelector('.reset-filter-btn')).toBeNull();

    component.selectedStatus.set('Pendiente');
    fixture.detectChanges();

    expect(component.hasActiveFilters()).toBe(true);
    const resetBtn = el.querySelector('.reset-filter-btn') as HTMLButtonElement;
    expect(resetBtn).toBeTruthy();
    expect(resetBtn.textContent?.trim()).toBe('Limpiar filtros');

    resetBtn.click();
    fixture.detectChanges();

    expect(component.selectedStatus()).toBe('Todos');
    expect(component.orderSearch()).toBe('');
    expect(component.dateFrom()).toBe('');
    expect(component.dateTo()).toBe('');
    expect(component.hasActiveFilters()).toBe(false);
  });
});

