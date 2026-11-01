import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';
import { describe, expect, it, vi } from 'vitest';
import { PaymentMethod, PaymentMethodService } from '../../../core/services/payment-method.service';
import { PaymentMethods } from './payment-methods';

describe('PaymentMethods', () => {
  let fixture: ComponentFixture<PaymentMethods>;
  let component: PaymentMethods;
  let deleteMethod: ReturnType<typeof vi.fn>;

  const method: PaymentMethod = {
    id: 'method-1',
    type: 'CARD',
    label: 'Tarjeta personal',
    cardholderName: 'Ana García',
    cardBrand: 'VISA',
    lastFourDigits: '1234',
    expirationMonth: 12,
    expirationYear: 2028,
    paypalEmail: null,
    bizumPhone: null,
    isDefault: true,
  };

  beforeEach(async () => {
    deleteMethod = vi.fn(() => throwError(() => new Error('delete failed')));

    await TestBed.configureTestingModule({
      imports: [PaymentMethods],
      providers: [{
        provide: PaymentMethodService,
        useValue: {
          list: vi.fn(() => of([method])),
          delete: deleteMethod,
          add: vi.fn(),
          update: vi.fn(),
          setDefault: vi.fn(),
        },
      }],
    }).compileComponents();

    fixture = TestBed.createComponent(PaymentMethods);
    component = fixture.componentInstance;
    fixture.detectChanges();
    await fixture.whenStable();
  });

  it('keeps the confirmation modal open and shows the deletion error when deletion fails', () => {
    component.deleteMethod(method);
    component.confirmDelete();
    fixture.detectChanges();

    expect(deleteMethod).toHaveBeenCalledWith('method-1');
    expect(component.methodToDelete()).toEqual(method);
    expect(component.actionId()).toBeNull();
    expect(component.deleteError()).toBe('No se ha podido eliminar el método de pago.');
    expect(fixture.nativeElement.querySelector('[role="alert"]')?.textContent)
      .toContain('No se ha podido eliminar el método de pago.');
  });
});
