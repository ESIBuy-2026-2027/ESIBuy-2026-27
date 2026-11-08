import { signal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { NEVER, of } from 'rxjs';
import { describe, beforeEach, afterEach, it, expect, vi } from 'vitest';
import { ClientWishlist } from './client-wishlist';
import { WishlistService } from '../../../core/services/wishlist.service';
import { ToastService } from '../../../core/services/toast.service';
import { CartService } from '../../../core/services/cart.service';
import { ProductItem } from '../../../core/services/products.service';

describe('ClientWishlist', () => {
  let component: ClientWishlist;
  let fixture: ComponentFixture<ClientWishlist>;
  let wishlistServiceMock: any;
  let toastServiceMock: any;
  let cartServiceMock: any;

  const mockWishlists = [
    { id: 'list-1', name: 'Mi lista de deseos', itemCount: 2, productIds: ['p-1', 'p-2'] },
    { id: 'list-2', name: 'Regalos', itemCount: 0, productIds: [] },
  ];

  const mockWishlistProducts: ProductItem[] = [
    {
      id: 'p-1',
      name: 'Monitor Gaming',
      categoryIds: ['cat-1'],
      stock: 3,
      description: '144Hz IPS',
      price: 199.99,
      available: true,
      sellerId: 's-1',
    },
    {
      id: 'p-2',
      name: 'Teclado Mecánico (Agotado/Pausado)',
      categoryIds: ['cat-1'],
      stock: 0,
      description: 'Switches rojos',
      price: 59.99,
      available: false,
      sellerId: 's-2',
    },
  ];

  beforeEach(async () => {
    localStorage.clear();
    wishlistServiceMock = {
      wishlists: vi.fn(() => mockWishlists),
      getWishlists: vi.fn(() => of(mockWishlists)),
      getWishlist: vi.fn(() => of(mockWishlistProducts)),
      createWishlist: vi.fn((name: string) =>
        of({ id: 'list-new', name, itemCount: 0, productIds: [] }),
      ),
      updateWishlistName: vi.fn((id: string, name: string) =>
        of({ id, name, itemCount: 2, productIds: ['p-1', 'p-2'] }),
      ),
      deleteWishlist: vi.fn(() => of({ message: 'Lista eliminada' })),
      removeFromWishlist: vi.fn(() => of({ message: 'Producto eliminado', inWishlist: false })),
    };

    const toastSignal = signal<any>(null);
    toastServiceMock = {
      toast: toastSignal,
      show: vi.fn((text, type = 'success') => toastSignal.set({ text, type })),
      clear: vi.fn(() => toastSignal.set(null)),
    };

    cartServiceMock = {
      addItem: vi.fn(() => of({})),
      itemCount: vi.fn(() => 0),
    };

    await TestBed.configureTestingModule({
      imports: [ClientWishlist],
      providers: [
        provideRouter([]),
        { provide: WishlistService, useValue: wishlistServiceMock },
        { provide: ToastService, useValue: toastServiceMock },
        { provide: CartService, useValue: cartServiceMock },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(ClientWishlist);
    component = fixture.componentInstance;
    fixture.detectChanges();
    await fixture.whenStable();
  });

  afterEach(() => {
    localStorage.clear();
  });

  it('should create and render navigation order in sidebar and wishlist heading', () => {
    expect(component).toBeTruthy();
    const el: HTMLElement = fixture.nativeElement;

    expect(el.querySelector('#wishlist-title')?.textContent).toContain('Listas de deseos');

    const navLinks = Array.from(el.querySelectorAll('.navigation-link'));
    expect(navLinks.length).toBe(4);
    expect(navLinks[0].textContent).toContain('Catálogo');
    expect(navLinks[1].textContent).toContain('Lista de deseos');
    expect(navLinks[1].classList.contains('is-active')).toBe(true);
    expect(navLinks[2].textContent).toContain('Mis pedidos');
    expect(navLinks[3].textContent).toContain('Carrito');
  });

  it('should render wishlists tabs and select the first one', () => {
    expect(component.selectedWishlist()?.id).toBe('list-1');
    const el: HTMLElement = fixture.nativeElement;
    const tabs = el.querySelectorAll('.wishlist-tab');
    expect(tabs.length).toBe(2);
    expect(tabs[0].textContent).toContain('Mi lista de deseos');
    expect(tabs[1].textContent).toContain('Regalos');
  });

  it('should switch selected wishlist when clicking a tab', () => {
    component.selectWishlist(mockWishlists[1]);
    fixture.detectChanges();

    expect(component.selectedWishlist()?.id).toBe('list-2');
    expect(wishlistServiceMock.getWishlist).toHaveBeenCalledWith('list-2');
  });

  it('should open create dialog and create a new wishlist', () => {
    component.openCreateDialog();
    expect(component.createDialogOpen()).toBe(true);

    component.newListName.set('Coleccionables');
    component.saveNewList();

    expect(wishlistServiceMock.createWishlist).toHaveBeenCalledWith('Coleccionables');
    expect(toastServiceMock.show).toHaveBeenCalledWith(
      'Lista "Coleccionables" creada con éxito',
      'success',
    );
    expect(component.createDialogOpen()).toBe(false);
  });

  it('should reject duplicate name when creating wishlist', () => {
    component.openCreateDialog();
    component.newListName.set('Regalos');
    component.saveNewList();

    expect(component.createError()).toBe('Ya tienes una lista de deseos con ese nombre.');
    expect(wishlistServiceMock.createWishlist).not.toHaveBeenCalled();
  });

  it('should open edit dialog and update list name', () => {
    component.openEditDialog();
    expect(component.editDialogOpen()).toBe(true);
    expect(component.editListName()).toBe('Mi lista de deseos');

    component.editListName.set('Favoritos Actualizados');
    component.saveEditedName();

    expect(wishlistServiceMock.updateWishlistName).toHaveBeenCalledWith(
      'list-1',
      'Favoritos Actualizados',
    );
    expect(toastServiceMock.show).toHaveBeenCalledWith('Nombre de la lista actualizado', 'success');
    expect(component.editDialogOpen()).toBe(false);
  });

  it('should open delete dialog and confirm deletion', () => {
    component.openDeleteDialog();
    expect(component.deleteDialogOpen()).toBe(true);
    fixture.detectChanges();

    const noticeBox = fixture.nativeElement.querySelector('.dialog-notice-box');
    expect(noticeBox).toBeTruthy();
    expect(noticeBox.textContent).toContain(
      'Esta acción quitará de favoritos todos los deseos de esta lista, salvo si un producto está en otra lista diferente agregado',
    );

    component.confirmDeleteList();

    expect(wishlistServiceMock.deleteWishlist).toHaveBeenCalledWith('list-1');
    expect(toastServiceMock.show).toHaveBeenCalledWith(
      'Lista "Mi lista de deseos" eliminada',
      'success',
    );
    expect(component.deleteDialogOpen()).toBe(false);
  });

  it('should toggle sidebar collapsed state', () => {
    expect(component.sidebarCollapsed()).toBe(false);
    component.toggleSidebar();
    expect(component.sidebarCollapsed()).toBe(true);
    component.toggleSidebar();
    expect(component.sidebarCollapsed()).toBe(false);
  });

  it('should add item to cart with selected quantity', () => {
    component.setQuantity('p-1', 2, 3);
    fixture.detectChanges();

    const el: HTMLElement = fixture.nativeElement;
    const buyBtn = el.querySelector('.buy-action-btn') as HTMLButtonElement;
    expect(buyBtn.textContent).toContain('Añadir al carrito');
    buyBtn.click();

    expect(cartServiceMock.addItem).toHaveBeenCalledWith('p-1', 2);
    expect(toastServiceMock.show).toHaveBeenCalledWith('Producto añadido al carrito', 'success');
  });

  it('should ignore repeated add-to-cart clicks while the request is pending', () => {
    cartServiceMock.addItem.mockReturnValue(NEVER);

    component.addToCart(mockWishlistProducts[0]);
    component.addToCart(mockWishlistProducts[0]);

    expect(cartServiceMock.addItem).toHaveBeenCalledTimes(1);
    expect(component.addingProducts()['p-1']).toBe(true);
  });

  it('should render items in wishlist and indicate unavailable products', () => {
    fixture.detectChanges();
    const el: HTMLElement = fixture.nativeElement;

    const cards = el.querySelectorAll('.wishlist-card');
    expect(cards.length).toBe(2);

    // Second product is unavailable
    const secondCard = cards[1];
    expect(secondCard.classList.contains('is-unavailable')).toBe(true);
    expect(secondCard.querySelector('.blocked-badge')?.textContent).toContain(
      'No disponible para compra',
    );
    expect((secondCard.querySelector('.buy-action-btn') as HTMLButtonElement)?.disabled).toBe(true);
  });

  it('should remove item from specific list when delete button is clicked', () => {
    fixture.detectChanges();
    const el: HTMLElement = fixture.nativeElement;

    const removeBtn = el.querySelector('.remove-btn') as HTMLButtonElement;
    expect(removeBtn).not.toBeNull();
    removeBtn?.click();

    expect(wishlistServiceMock.removeFromWishlist).toHaveBeenCalledWith('p-1', 'list-1');
    expect(toastServiceMock.show).toHaveBeenCalledWith('Producto eliminado', 'success');
  });
});
