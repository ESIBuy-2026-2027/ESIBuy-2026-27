import { ComponentFixture, TestBed } from '@angular/core/testing';
import { WishlistSelectModalComponent } from './wishlist-select-modal';
import { WishlistService } from '../../core/services/wishlist.service';
import { ToastService } from '../../core/services/toast.service';
import { of, throwError } from 'rxjs';

describe('WishlistSelectModalComponent', () => {
  let component: WishlistSelectModalComponent;
  let fixture: ComponentFixture<WishlistSelectModalComponent>;
  let wishlistServiceMock: any;
  let toastServiceMock: any;

  beforeEach(async () => {
    wishlistServiceMock = {
      selectModalOpen: () => true,
      wishlists: () => [
        { id: 'list-1', name: 'Favoritos', itemCount: 2, productIds: ['p1', 'p2'] },
        { id: 'list-2', name: 'Cumpleaños', itemCount: 0, productIds: [] },
      ],
      targetProductId: () => 'product-1',
      targetProductName: () => 'Smartphone X',
      closeWishlistSelector: vi.fn(),
      addToWishlist: vi.fn().mockReturnValue(of({ message: 'OK', inWishlist: true })),
      createWishlist: vi
        .fn()
        .mockReturnValue(of({ id: 'list-3', name: 'Nueva', itemCount: 0, productIds: [] })),
    };

    toastServiceMock = {
      show: vi.fn(),
      toast: () => null,
      clear: vi.fn(),
    };

    await TestBed.configureTestingModule({
      imports: [WishlistSelectModalComponent],
      providers: [
        { provide: WishlistService, useValue: wishlistServiceMock },
        { provide: ToastService, useValue: toastServiceMock },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(WishlistSelectModalComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create and select first wishlist by default', () => {
    expect(component).toBeTruthy();
    expect(component.selectedWishlistId()).toBe('list-1');
  });

  it('should switch selected wishlist', () => {
    component.selectWishlist('list-2');
    expect(component.selectedWishlistId()).toBe('list-2');
    expect(component.creatingNew()).toBe(false);
  });

  it('should toggle new list input and validate length', () => {
    component.toggleCreatingNew();
    expect(component.creatingNew()).toBe(true);

    component.newListName.set('a');
    component.save();
    expect(component.errorMessage()).toBe('El nombre debe tener entre 2 y 50 caracteres.');
    expect(wishlistServiceMock.createWishlist).not.toHaveBeenCalled();
  });

  it('should reject duplicate list name when creating new list', () => {
    component.toggleCreatingNew();
    component.newListName.set('Favoritos');
    component.save();
    expect(component.errorMessage()).toBe('Ya tienes una lista de deseos con ese nombre.');
  });

  it('should create new list and add product when valid', () => {
    component.toggleCreatingNew();
    component.newListName.set('Regalos');
    component.save();

    expect(wishlistServiceMock.createWishlist).toHaveBeenCalledWith('Regalos');
    expect(wishlistServiceMock.addToWishlist).toHaveBeenCalledWith('product-1', 'list-3');
    expect(toastServiceMock.show).toHaveBeenCalled();
  });

  it('should save to selected existing wishlist', () => {
    component.selectWishlist('list-2');
    component.save();

    expect(wishlistServiceMock.addToWishlist).toHaveBeenCalledWith('product-1', 'list-2');
    expect(wishlistServiceMock.closeWishlistSelector).toHaveBeenCalled();
  });

  it('should close selector on cancel', () => {
    component.close();
    expect(wishlistServiceMock.closeWishlistSelector).toHaveBeenCalled();
  });
});
