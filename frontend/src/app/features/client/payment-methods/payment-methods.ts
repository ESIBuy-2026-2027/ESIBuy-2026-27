
import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { PaymentMethod, PaymentMethodRequest, PaymentMethodService, PaymentMethodType, } from '../../../core/services/payment-method.service';

interface PaymentMethodForm {
    type: PaymentMethodType;
    label: string;
    cardholderName: string;
    cardBrand: string;
    lastFourDigits: string;
    expiration: string;
    paypalEmail: string;
    bizumPhone: string;
    defaultMethod: boolean;
}

@Component({
    selector: 'app-payment-methods',
    standalone: true,
    imports: [FormsModule],
    templateUrl: './payment-methods.html',
    styleUrl: './payment-methods.css',
})
export class PaymentMethods implements OnInit {
    private readonly paymentMethodService = inject(PaymentMethodService);

    readonly methods = signal<PaymentMethod[]>([]);
    readonly loading = signal(true);
    readonly editingMethodId = signal<string | null>(null);
    readonly methodToDelete = signal<PaymentMethod | null>(null);
    readonly saving = signal(false);
    readonly actionId = signal<string | null>(null);
    readonly formVisible = signal(false);
    readonly error = signal<string | null>(null);
    readonly success = signal<string | null>(null);

    readonly currentYear = new Date().getFullYear();

    readonly form = signal<PaymentMethodForm>(this.emptyForm());

    readonly cardMethods = computed(
        () => this.methods().filter(method => method.type === 'CARD'),
    );

    readonly otherMethods = computed(
        () => this.methods().filter(method => method.type !== 'CARD'),
    );

    readonly selectedType = computed(() => this.form().type);

    ngOnInit(): void {
        this.loadMethods();
    }

    private emptyForm(): PaymentMethodForm {
        return {
            type: 'CARD',
            label: '',
            cardholderName: '',
            cardBrand: 'VISA',
            lastFourDigits: '',
            expiration: '',
            paypalEmail: '',
            bizumPhone: '',
            defaultMethod: false,
        };
    }

    loadMethods(): void {
        this.loading.set(true);
        this.error.set(null);

        this.paymentMethodService.list().subscribe({
            next: methods => {
                this.methods.set(methods);
                this.loading.set(false);
            },
            error: () => {
                this.loading.set(false);
                this.error.set(
                    'No se han podido cargar los métodos de pago. Inténtalo de nuevo.',
                );
            },
        });
    }

    openForm(): void {
        this.editingMethodId.set(null);
        this.form.set({
            ...this.emptyForm(),
            defaultMethod: this.methods().length === 0,
        });
        this.error.set(null);
        this.success.set(null);
        this.formVisible.set(true);
    }

    openEdit(method: PaymentMethod): void {
        this.editingMethodId.set(method.id);

        this.form.set({
            type: method.type,
            label: method.label ?? '',
            cardholderName: method.cardholderName ?? '',
            cardBrand: method.cardBrand ?? 'VISA',
            lastFourDigits: method.lastFourDigits ?? '',
            expiration: method.expirationMonth && method.expirationYear
                ? `${String(method.expirationMonth).padStart(2, '0')}/${String(method.expirationYear).slice(-2)}`
                : '',
            paypalEmail: method.paypalEmail ?? '',
            bizumPhone: method.bizumPhone ?? '',
            defaultMethod: method.isDefault,
        });

        this.error.set(null);
        this.success.set(null);
        this.formVisible.set(true);
    }

    cancelForm(): void {
        this.formVisible.set(false);
        this.editingMethodId.set(null);
        this.form.set(this.emptyForm());
        this.error.set(null);
    }

    updateType(type: string): void {
        if (type !== 'CARD' && type !== 'PAYPAL' && type !== 'BIZUM') {
            return;
        }

        this.form.update(current => ({
            ...current,
            type,
            label: '',
            cardholderName: '',
            cardBrand: 'VISA',
            lastFourDigits: '',
            expiration: '',
            paypalEmail: '',
            bizumPhone: '',
        }));

        this.error.set(null);
    }

    updateField<K extends keyof PaymentMethodForm>(
        field: K,
        value: PaymentMethodForm[K],
    ): void {
        this.form.update(current => ({ ...current, [field]: value }));
    }


    save(): void {
        if (this.saving()) return;

        this.error.set(null);
        this.success.set(null);

        const form = this.form();
        const request = this.buildRequest(form);

        if (!request) return;

        this.saving.set(true);

        const editingId = this.editingMethodId();

        const operation = editingId !== null
            ? this.paymentMethodService.update(editingId, request)
            : this.paymentMethodService.add(request);

        operation.subscribe({
            next: method => {
                this.methods.update(methods => {
                    const updatedMethods = editingId !== null
                        ? methods.map(item =>
                            item.id === method.id ? method : item,
                        )
                        : [...methods, method];

                    // Si el método guardado es el predeterminado,
                    // desmarcar los demás.
                    if (method.isDefault) {
                        return updatedMethods.map(item => ({
                            ...item,
                            isDefault: item.id === method.id,
                        }));
                    }

                    return updatedMethods;
                });

                this.saving.set(false);
                this.formVisible.set(false);
                this.editingMethodId.set(null);
                this.form.set(this.emptyForm());

                this.success.set(
                    editingId !== null
                        ? 'El método de pago se ha actualizado correctamente.'
                        : 'El método de pago se ha guardado correctamente.',
                );

                // Sincronizar con el estado definitivo del servidor.
                this.loadMethods();
            },
            error: error => {
                this.saving.set(false);

                this.error.set(
                    error?.error?.detail
                    ?? error?.error?.message
                    ?? (
                        editingId !== null
                            ? 'No se ha podido actualizar el método de pago. Revisa los datos e inténtalo de nuevo.'
                            : 'No se ha podido guardar el método de pago. Revisa los datos e inténtalo de nuevo.'
                    ),
                );
            },
        });
    }



    private buildRequest(form: PaymentMethodForm): PaymentMethodRequest | null {
        const label = form.label.trim();

        if (label.length > 40) {
            this.error.set('El nombre identificativo no puede superar 40 caracteres.');
            return null;
        }

        const request: PaymentMethodRequest = {
            type: form.type,
            label: label || null,
            isDefault: form.defaultMethod,
        };

        switch (form.type) {
            case 'CARD':
                return this.addCardDetails(request, form);

            case 'PAYPAL':
                return this.addPayPalDetails(request, form);

            case 'BIZUM':
                return this.addBizumDetails(request, form);
        }
    }

    private addCardDetails(
        request: PaymentMethodRequest,
        form: PaymentMethodForm,
    ): PaymentMethodRequest | null {
        const cardholderName = form.cardholderName.trim();
        const lastFourDigits = form.lastFourDigits.trim();
        const expiration = this.parseExpiration(form.expiration);

        if (!expiration) {
            this.error.set('Introduce una fecha de caducidad válida.');
            return null;
        }

        if (cardholderName.length < 2 || cardholderName.length > 80) {
            this.error.set('Introduce el nombre del titular (entre 2 y 80 caracteres).');
            return null;
        }

        if (!/^\d{4}$/.test(lastFourDigits)) {
            this.error.set('Introduce las cuatro últimas cifras de la tarjeta.');
            return null;
        }

        if (!this.isValidExpiration(expiration.month, expiration.year)) {
            this.error.set('Introduce una fecha de caducidad válida.');
            return null;
        }

        request.cardholderName = cardholderName;
        request.cardBrand = form.cardBrand;
        request.lastFourDigits = lastFourDigits;
        request.expirationMonth = expiration.month;
        request.expirationYear = expiration.year;

        return request;
    }

    private isValidExpiration(month: number, year: number): boolean {
        if (
            !Number.isInteger(month) || month < 1 || month > 12
            || !Number.isInteger(year) || year < this.currentYear
        ) {
            return false;
        }

        const now = new Date();

        return year !== now.getFullYear() || month >= now.getMonth() + 1;
    }

    private addPayPalDetails(
        request: PaymentMethodRequest,
        form: PaymentMethodForm,
    ): PaymentMethodRequest | null {
        const paypalEmail = form.paypalEmail.trim();

        if (
            paypalEmail.length > 254
            || !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(paypalEmail)
        ) {
            this.error.set('Introduce un correo electrónico válido para PayPal.');
            return null;
        }

        request.paypalEmail = paypalEmail;
        return request;
    }

    private addBizumDetails(
        request: PaymentMethodRequest,
        form: PaymentMethodForm,
    ): PaymentMethodRequest | null {
        const bizumPhone = form.bizumPhone.trim();

        if (!/^\d{9}$/.test(bizumPhone)) {
            this.error.set('El teléfono de Bizum debe tener exactamente 9 cifras.');
            return null;
        }

        request.bizumPhone = bizumPhone;
        return request;
    }


    setDefault(method: PaymentMethod): void {
        if (method.isDefault || this.actionId()) return;

        this.error.set(null);
        this.success.set(null);
        this.actionId.set(method.id);

        this.paymentMethodService.setDefault(method.id).subscribe({
            next: () => {
                this.methods.update(methods =>
                    methods.map(item => ({
                        ...item,
                        isDefault: item.id === method.id,
                    })),
                );
                this.actionId.set(null);
                this.success.set('Se ha actualizado el método de pago predeterminado.');
            },
            error: () => {
                this.actionId.set(null);
                this.error.set('No se ha podido cambiar el método predeterminado.');
            },
        });
    }


    deleteMethod(method: PaymentMethod): void {
        if (this.actionId()) return;

        this.error.set(null);
        this.success.set(null);
        this.methodToDelete.set(method);
    }

    cancelDelete(): void {
        if (this.actionId()) return;

        this.methodToDelete.set(null);
    }

    confirmDelete(): void {
        const method = this.methodToDelete();

        if (!method || this.actionId()) return;

        this.error.set(null);
        this.success.set(null);
        this.actionId.set(method.id);

        this.paymentMethodService.delete(method.id).subscribe({
            next: () => {
                this.methods.update(methods =>
                    methods.filter(item => item.id !== method.id),
                );

                this.actionId.set(null);
                this.methodToDelete.set(null);
                this.success.set('El método de pago se ha eliminado.');

                // El servidor puede asignar otro método como predeterminado.
                this.loadMethods();
            },
            error: () => {
                this.actionId.set(null);
                this.methodToDelete.set(null);
                this.error.set('No se ha podido eliminar el método de pago.');
            },
        });
    }


    methodDescription(method: PaymentMethod): string {
        switch (method.type) {
            case 'CARD':
                return `${method.cardBrand ?? 'Tarjeta'} •••• ${method.lastFourDigits ?? '----'}`;
            case 'PAYPAL':
                return `PayPal (${method.paypalEmail ?? ''})`;
            case 'BIZUM':
                return `Bizum (${method.bizumPhone ?? ''})`;
        }
    }

    methodTypeLabel(type: PaymentMethodType): string {
        switch (type) {
            case 'CARD':
                return 'Tarjeta';
            case 'PAYPAL':
                return 'PayPal';
            case 'BIZUM':
                return 'Bizum';
        }
    }

    expirationLabel(method: PaymentMethod): string {
        if (!method.expirationMonth || !method.expirationYear) return '';

        return `${String(method.expirationMonth).padStart(2, '0')}/${method.expirationYear}`;
    }

    trackById(_index: number, method: PaymentMethod): string {
        return method.id;
    }

    parseNullableNumber(value: string | number | null): number | null {
        if (value === null || value === '') {
            return null;
        }

        const parsed = Number(value);

        return Number.isFinite(parsed) ? parsed : null;
    }


    private parseExpiration(value: string): { month: number; year: number } | null {
        const match = /^(0[1-9]|1[0-2])\/(\d{2})$/.exec(value.trim());

        if (!match) {
            return null;
        }

        const month = Number(match[1]);
        const year = 2000 + Number(match[2]);
        const now = new Date();

        if (
            year < now.getFullYear()
            || (year === now.getFullYear() && month < now.getMonth() + 1)
        ) {
            return null;
        }

        return { month, year };
    }

    formatExpiration(value: string): string {
        const digits = value.replace(/\D/g, '').slice(0, 4);

        if (digits.length <= 2) {
            return digits;
        }

        return `${digits.slice(0, 2)}/${digits.slice(2)}`;
    }

}
