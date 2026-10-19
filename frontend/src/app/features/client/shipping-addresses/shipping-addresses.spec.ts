import { HttpErrorResponse } from '@angular/common/http';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { of, throwError } from 'rxjs';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { AuthService } from '../../../core/services/auth.service';
import { ShippingAddress, ShippingAddressService } from '../../../core/services/shipping-address.service';
import { MAX_ADDRESSES, ShippingAddresses } from './shipping-addresses';

describe('ShippingAddresses', () => {
  const home: ShippingAddress = {
    id: 'a1',
    recipientName: 'Ana García',
    street: 'Calle Mayor, 12',
    additionalInfo: '2º B',
    postalCode: '13071',
    city: 'Ciudad Real',
    province: 'Ciudad Real',
    phone: '600123123',
    isDefault: true,
  };

  const list = vi.fn();
  const add = vi.fn();
  const clearCurrentUser = vi.fn();
  let fixture: ComponentFixture<ShippingAddresses>;
  let el: HTMLElement;

  async function render(): Promise<void> {
    await TestBed.configureTestingModule({
      imports: [ShippingAddresses],
      providers: [
        provideRouter([]),
        { provide: AuthService, useValue: { clearCurrentUser } },
        { provide: ShippingAddressService, useValue: { list, add } },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(ShippingAddresses);
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();
    el = fixture.nativeElement as HTMLElement;
  }

  async function type(id: string, value: string): Promise<void> {
    const input = el.querySelector(`#${id}`) as HTMLInputElement | null;
    if (input) {
      input.value = value;
      input.dispatchEvent(new Event('input'));
    }
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();
  }

  async function click(selector: string): Promise<void> {
    const target = el.querySelector(selector) as HTMLElement | null;
    target?.click();
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();
  }

  async function fillValidForm(): Promise<void> {
    await type('recipientName', '  Luis Pérez ');
    await type('street', 'Avenida del Mar, 5');
    await type('postalCode', '28001');
    await type('city', 'Madrid');
    await type('province', 'Madrid');
  }

  async function submit(): Promise<void> {
    const form = el.querySelector('form') as HTMLFormElement | null;
    form?.dispatchEvent(new Event('submit'));
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();
  }

  beforeEach(() => {
    list.mockReset();
    add.mockReset();
    clearCurrentUser.mockClear();
  });

  it('muestra las direcciones y marca la predeterminada', async () => {
    list.mockReturnValue(of([home]));
    await render();

    expect(el.querySelectorAll('.address-card').length).toBe(1);
    expect(el.querySelector('.address-card')?.textContent).toContain('Calle Mayor, 12, 2º B');
    expect(el.querySelector('.default-badge')?.textContent).toContain('Predeterminada');
  });

  it('muestra un estado vacío cuando no hay direcciones', async () => {
    list.mockReturnValue(of([]));
    await render();

    expect(el.querySelector('.empty-state')).not.toBeNull();
  });

  it('muestra el error de carga y permite reintentar', async () => {
    list.mockReturnValueOnce(throwError(() => new HttpErrorResponse({ status: 500 })));
    await render();

    expect(el.querySelector('[role="alert"]')?.textContent).toContain('No se pudieron cargar');

    list.mockReturnValueOnce(of([home]));
    await click('.primary-button');

    expect(el.querySelector('[role="alert"]')).toBeNull();
    expect(el.querySelectorAll('.address-card').length).toBe(1);
  });

  it('cierra la sesión si la sesión ha caducado al cargar (401)', async () => {
    list.mockReturnValue(throwError(() => new HttpErrorResponse({ status: 401 })));
    const navigate = vi.spyOn(Router.prototype, 'navigate').mockResolvedValue(true);
    await render();

    expect(clearCurrentUser).toHaveBeenCalled();
    expect(navigate).toHaveBeenCalledWith(['/'], { queryParams: { expired: 'true' } });
  });

  it('propone la primera dirección como predeterminada', async () => {
    list.mockReturnValue(of([]));
    await render();
    await click('[data-testid="add-address"]');

    expect((el.querySelector('#isDefault') as HTMLInputElement).checked).toBe(true);
  });

  it('no propone como predeterminada una dirección posterior', async () => {
    list.mockReturnValue(of([home]));
    await render();
    await click('[data-testid="add-address"]');

    expect((el.querySelector('#isDefault') as HTMLInputElement).checked).toBe(false);
  });

  it('valida los campos obligatorios sin llamar al servidor', async () => {
    list.mockReturnValue(of([]));
    await render();
    await click('[data-testid="add-address"]');
    await submit();

    expect(add).not.toHaveBeenCalled();
    expect(el.querySelectorAll('.field-error').length).toBe(5);
    expect((el.querySelector('#street') as HTMLInputElement).getAttribute('aria-invalid')).toBe('true');
  });

  it('valida el formato del código postal y del teléfono', async () => {
    list.mockReturnValue(of([]));
    await render();
    await click('[data-testid="add-address"]');
    await fillValidForm();
    await type('postalCode', '1234');
    await type('phone', '12345');
    await submit();

    expect(add).not.toHaveBeenCalled();
    expect(el.querySelector('#postalCode-error')?.textContent).toContain('5 cifras');
    expect(el.querySelector('#phone-error')?.textContent).toContain('9 cifras');
  });

  it('guarda la dirección con los datos recortados y la añade a la lista', async () => {
    list.mockReturnValue(of([]));
    add.mockImplementation((request) => of({ ...request, id: 'new' }));
    await render();
    await click('[data-testid="add-address"]');
    await fillValidForm();
    await submit();

    expect(add).toHaveBeenCalledWith({
      recipientName: 'Luis Pérez',
      street: 'Avenida del Mar, 5',
      additionalInfo: null,
      postalCode: '28001',
      city: 'Madrid',
      province: 'Madrid',
      phone: null,
      isDefault: true,
    });
    expect(el.querySelector('form')).toBeNull();
    expect(el.querySelectorAll('.address-card').length).toBe(1);
    expect(el.querySelector('.success-message')?.textContent).toContain('añadido correctamente');
  });

  it('desmarca la anterior predeterminada cuando la nueva lo es', async () => {
    list.mockReturnValue(of([home]));
    add.mockImplementation((request) => of({ ...request, id: 'new' }));
    await render();
    await click('[data-testid="add-address"]');
    await fillValidForm();
    await click('#isDefault');
    await submit();

    expect(el.querySelectorAll('.default-badge').length).toBe(1);
    expect(el.querySelectorAll('.address-card')[1].classList).toContain('is-default');
  });

  it('muestra bajo cada campo los errores de validación del servidor (400)', async () => {
    list.mockReturnValue(of([]));
    add.mockReturnValue(
      throwError(
        () => new HttpErrorResponse({ status: 400, error: { errors: { postalCode: 'Código postal no válido' } } }),
      ),
    );
    await render();
    await click('[data-testid="add-address"]');
    await fillValidForm();
    await submit();

    expect(el.querySelector('#postalCode-error')?.textContent).toContain('Código postal no válido');
    expect(el.querySelector('form')).not.toBeNull();
  });

  it('muestra el mensaje del servidor cuando se alcanza el límite (409)', async () => {
    list.mockReturnValue(of([]));
    add.mockReturnValue(
      throwError(() => new HttpErrorResponse({ status: 409, error: { message: 'Límite alcanzado' } })),
    );
    await render();
    await click('[data-testid="add-address"]');
    await fillValidForm();
    await submit();

    expect(el.querySelector('form [role="alert"]')?.textContent).toContain('Límite alcanzado');
  });

  it('muestra un mensaje de permisos ante un 403 al guardar', async () => {
    list.mockReturnValue(of([]));
    add.mockReturnValue(throwError(() => new HttpErrorResponse({ status: 403 })));
    await render();
    await click('[data-testid="add-address"]');
    await fillValidForm();
    await submit();

    expect(el.querySelector('form [role="alert"]')?.textContent).toContain('No tienes permisos');
  });

  it('cierra la sesión si caduca al guardar (401)', async () => {
    list.mockReturnValue(of([]));
    add.mockReturnValue(throwError(() => new HttpErrorResponse({ status: 401 })));
    const navigate = vi.spyOn(Router.prototype, 'navigate').mockResolvedValue(true);
    await render();
    await click('[data-testid="add-address"]');
    await fillValidForm();
    await submit();

    expect(clearCurrentUser).toHaveBeenCalled();
    expect(navigate).toHaveBeenCalledWith(['/'], { queryParams: { expired: 'true' } });
  });

  it('deshabilita el botón al alcanzar el máximo de direcciones', async () => {
    const many = Array.from({ length: MAX_ADDRESSES }, (_, i) => ({ ...home, id: `a${i}`, isDefault: i === 0 }));
    list.mockReturnValue(of(many));
    await render();

    expect((el.querySelector('[data-testid="add-address"]') as HTMLButtonElement).disabled).toBe(true);
    expect(el.querySelector('.limit-note')).not.toBeNull();
  });

  it('cancela el formulario sin guardar', async () => {
    list.mockReturnValue(of([]));
    await render();
    await click('[data-testid="add-address"]');
    await click('.secondary-button');

    expect(el.querySelector('form')).toBeNull();
    expect(add).not.toHaveBeenCalled();
  });
});
