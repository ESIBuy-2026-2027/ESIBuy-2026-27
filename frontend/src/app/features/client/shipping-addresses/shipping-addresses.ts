import { HttpErrorResponse } from '@angular/common/http';
import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';
import { ShippingAddress, ShippingAddressRequest, ShippingAddressService } from '../../../core/services/shipping-address.service';

export const MAX_ADDRESSES = 10;

export type AddressField =
  | 'recipientName'
  | 'street'
  | 'additionalInfo'
  | 'postalCode'
  | 'city'
  | 'province'
  | 'phone';

export type AddressFieldErrors = Partial<Record<AddressField, string>>;

export interface AddressForm {
  recipientName: string;
  street: string;
  additionalInfo: string;
  postalCode: string;
  city: string;
  province: string;
  phone: string;
  isDefault: boolean;
}

const POSTAL_CODE_PATTERN = /^\d{5}$/;
const PHONE_PATTERN = /^\d{9}$/;

function emptyForm(isDefault: boolean): AddressForm {
  return {
    recipientName: '',
    street: '',
    additionalInfo: '',
    postalCode: '',
    city: '',
    province: '',
    phone: '',
    isDefault,
  };
}

/** Validación de formato en cliente. El servidor repite las mismas reglas. */
export function validateAddress(form: AddressForm): AddressFieldErrors {
  const errors: AddressFieldErrors = {};
  const recipientName = form.recipientName.trim();
  const street = form.street.trim();
  const city = form.city.trim();
  const province = form.province.trim();
  const phone = form.phone.trim();

  if (recipientName.length < 2 || recipientName.length > 80) {
    errors.recipientName = 'El nombre del destinatario debe tener entre 2 y 80 caracteres.';
  }
  if (street.length < 3 || street.length > 100) {
    errors.street = 'La calle y número deben tener entre 3 y 100 caracteres.';
  }
  if (form.additionalInfo.trim().length > 60) {
    errors.additionalInfo = 'La información adicional no puede superar los 60 caracteres.';
  }
  if (!POSTAL_CODE_PATTERN.test(form.postalCode.trim())) {
    errors.postalCode = 'El código postal debe tener exactamente 5 cifras.';
  }
  if (city.length < 2 || city.length > 50) {
    errors.city = 'La ciudad debe tener entre 2 y 50 caracteres.';
  }
  if (province.length < 2 || province.length > 50) {
    errors.province = 'La provincia debe tener entre 2 y 50 caracteres.';
  }
  if (phone && !PHONE_PATTERN.test(phone)) {
    errors.phone = 'El teléfono debe tener exactamente 9 cifras.';
  }
  return errors;
}

@Component({
  selector: 'app-shipping-addresses',
  standalone: true,
  imports: [FormsModule],
  templateUrl: './shipping-addresses.html',
  styleUrl: './shipping-addresses.css',
})
export class ShippingAddresses implements OnInit {
  private readonly addressService = inject(ShippingAddressService);
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);

  readonly maxAddresses = MAX_ADDRESSES;

  readonly addresses = signal<ShippingAddress[]>([]);
  readonly loading = signal(true);
  editing = signal<ShippingAddress | null>(null);
  readonly deletingAddressId = signal<string | null>(null);
  readonly addressPendingDeletion = signal<ShippingAddress | null>(null);
  readonly deleteError = signal<string | null>(null);
  readonly loadError = signal<string | null>(null);

  readonly formOpen = signal(false);
  readonly saving = signal(false);
  readonly submitted = signal(false);
  readonly fieldErrors = signal<AddressFieldErrors>({});
  readonly formError = signal<string | null>(null);
  readonly successMessage = signal<string | null>(null);

  readonly limitReached = computed(() => this.addresses().length >= MAX_ADDRESSES);

  form: AddressForm = emptyForm(true);

  ngOnInit(): void {
    this.loadAddresses();
  }

  loadAddresses(): void {
    this.loading.set(true);
    this.loadError.set(null);
    this.addressService.list().subscribe({
      next: (addresses) => {
        this.addresses.set(addresses);
        this.loading.set(false);
      },
      error: (error: unknown) => {
        this.loading.set(false);
        if (this.handleExpiredSession(error)) {
          return;
        }
        this.loadError.set(this.loadErrorMessage(error));
      },
    });
  }

  openForm(): void {
    if (this.limitReached()) {
      return;
    }
    // La primera dirección del cliente se propone como predeterminada.
    this.form = emptyForm(this.addresses().length === 0);
    this.fieldErrors.set({});
    this.formError.set(null);
    this.successMessage.set(null);
    this.submitted.set(false);
    this.editing.set(null);
    this.formOpen.set(true);
  }

  cancelForm(): void {
    if (this.saving()) return;
    this.editing.set(null);   // ← añadir
    this.formOpen.set(false);
    this.fieldErrors.set({});
    this.formError.set(null);
  }

  openEdit(address: ShippingAddress): void {
    this.editing.set(address);
    this.form = {
      recipientName: address.recipientName,
      street: address.street,
      additionalInfo: address.additionalInfo ?? '',
      postalCode: address.postalCode,
      city: address.city,
      province: address.province,
      phone: address.phone ?? '',
      isDefault: address.isDefault,
    };
    this.formOpen.set(true);
    this.submitted.set(false);
    this.fieldErrors.set({});
    this.formError.set(null);
    this.successMessage.set(null);
  }

  /** Tras el primer intento de envío, los errores se actualizan mientras se escribe. */
  onFieldChange(): void {
    if (this.submitted()) {
      this.fieldErrors.set(validateAddress(this.form));
    }
  }


  confirmDeleteAddress(address: ShippingAddress): void {
    if (this.saving() || this.deletingAddressId() !== null) {
      return;
    }

    this.deleteError.set(null);
    this.addressPendingDeletion.set(address);
  }

  cancelDeleteAddress(): void {
    if (this.deletingAddressId() !== null) {
      return;
    }

    this.addressPendingDeletion.set(null);
  }

  deleteAddress(): void {
    const address = this.addressPendingDeletion();

    if (!address || this.saving() || this.deletingAddressId() !== null) {
      return;
    }

    this.deletingAddressId.set(address.id);
    this.deleteError.set(null);
    this.successMessage.set(null);

    this.addressService.delete(address.id).subscribe({
      next: () => {
        this.addresses.update((current) => {
          const remaining = current.filter(
            (item) => item.id !== address.id,
          );

          if (!address.isDefault || remaining.length === 0) {
            return remaining;
          }

          return remaining.map((item, index) => ({
            ...item,
            isDefault: index === 0,
          }));
        });

        this.addressPendingDeletion.set(null);
        this.deletingAddressId.set(null);
        this.successMessage.set(
          'La dirección se ha eliminado correctamente.',
        );
      },
      error: (error: unknown) => {
        this.deletingAddressId.set(null);

        if (this.handleExpiredSession(error)) {
          return;
        }

        this.deleteError.set(this.deleteErrorMessage(error));
        this.addressPendingDeletion.set(null);
      },
    });
  }


  save(): void {
    if (this.saving()) {
      return;
    }
    this.submitted.set(true);
    this.formError.set(null);
    this.successMessage.set(null);

    const errors = validateAddress(this.form);
    this.fieldErrors.set(errors);
    if (Object.keys(errors).length > 0) {
      return;
    }

    const request: ShippingAddressRequest = {
      recipientName: this.form.recipientName.trim(),
      street: this.form.street.trim(),
      additionalInfo: this.form.additionalInfo.trim() || null,
      postalCode: this.form.postalCode.trim(),
      city: this.form.city.trim(),
      province: this.form.province.trim(),
      phone: this.form.phone.trim() || null,
      isDefault: this.form.isDefault,
    };

    this.saving.set(true);
    const editTarget = this.editing();
    const request$ = editTarget
      ? this.addressService.update(editTarget.id, request)
      : this.addressService.add(request);

    request$.subscribe({
      next: (saved) => {
        if (editTarget) {
          this.addresses.update((current) =>
            current.map((address) => {
              if (address.id === saved.id) {
                return saved;
              }

              if (saved.isDefault) {
                return { ...address, isDefault: false };
              }

              return address;
            }),
          );

          this.successMessage.set(
            'La dirección se ha actualizado correctamente.',
          );
        } else {
          this.addresses.update((current) => [
            ...(saved.isDefault
              ? current.map((address) => ({
                ...address,
                isDefault: false,
              }))
              : current),
            saved,
          ]);

          this.successMessage.set(
            'La dirección se ha añadido correctamente.',
          );
        }

        this.editing.set(null);
        this.saving.set(false);
        this.formOpen.set(false);
      },
      error: (error: unknown) => {
        this.saving.set(false);
        if (this.handleExpiredSession(error)) return;
        this.applySaveError(error);
      },
    });
  }

  private applySaveError(error: unknown): void {
    if (!(error instanceof HttpErrorResponse)) {
      this.formError.set('No se ha podido guardar la dirección. Inténtalo de nuevo.');
      return;
    }
    const body = error.error as { message?: string; errors?: Record<string, string> } | null;

    if (error.status === 400 && body?.errors) {
      const known: AddressFieldErrors = {};
      const unknown: string[] = [];
      for (const [field, message] of Object.entries(body.errors)) {
        if (field in emptyForm(false) && field !== 'isDefault') {
          known[field as AddressField] = message;
        } else {
          unknown.push(message);
        }
      }
      this.fieldErrors.set(known);
      this.formError.set(
        unknown.length > 0 ? unknown.join('. ') : 'Revisa los datos marcados en el formulario.',
      );
      return;
    }

    this.formError.set(this.saveErrorMessage(error, body?.message));
  }

  private saveErrorMessage(error: HttpErrorResponse, serverMessage?: string): string {
    switch (error.status) {
      case 0:
        return 'No se pudo conectar con el servidor. Comprueba tu conexión e inténtalo de nuevo.';
      case 400:
        return 'Revisa los datos introducidos e inténtalo de nuevo.';
      case 403:
        return 'No tienes permisos para gestionar direcciones de envío.';
      case 404:
        return 'No hemos encontrado tu cuenta. Vuelve a iniciar sesión.';
      case 409:
        return serverMessage ?? `Has alcanzado el máximo de ${MAX_ADDRESSES} direcciones.`;
      default:
        return 'No se ha podido guardar la dirección. Inténtalo de nuevo más tarde.';
    }
  }

  private loadErrorMessage(error: unknown): string {
    if (error instanceof HttpErrorResponse) {
      if (error.status === 0) {
        return 'No se pudo conectar con el servidor. Comprueba tu conexión e inténtalo de nuevo.';
      }
      if (error.status === 403) {
        return 'No tienes permisos para consultar tus direcciones.';
      }
    }
    return 'No se pudieron cargar tus direcciones. Inténtalo de nuevo más tarde.';
  }

  private deleteErrorMessage(error: unknown): string {
    if (!(error instanceof HttpErrorResponse)) {
      return 'No se ha podido eliminar la dirección. Inténtalo de nuevo.';
    }

    switch (error.status) {
      case 0:
        return 'No se pudo conectar con el servidor. Comprueba tu conexión e inténtalo de nuevo.';
      case 403:
        return 'No tienes permisos para eliminar direcciones de envío.';
      case 404:
        return 'No hemos encontrado esa dirección. Actualiza la lista e inténtalo de nuevo.';
      default:
        return 'No se ha podido eliminar la dirección. Inténtalo de nuevo más tarde.';
    }
  }

  private handleExpiredSession(error: unknown): boolean {
    if (error instanceof HttpErrorResponse && error.status === 401) {
      this.authService.clearCurrentUser();
      void this.router.navigate(['/'], { queryParams: { expired: 'true' } });
      return true;
    }
    return false;
  }
}
