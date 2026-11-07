import { ChangeDetectorRef, Component } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { finalize, timeout } from 'rxjs';
import { CommonModule } from '@angular/common';
import {
  ReactiveFormsModule,
  FormBuilder,
  FormGroup,
  Validators,
  AbstractControl,
  ValidationErrors
} from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';
import { Category, ProductsService } from '../../../core/services/products.service';
import { CLIENT_AVATARS, SELLER_AVATARS } from '../../../core/constants/avatars';

export function isAdultValidator(control: AbstractControl): ValidationErrors | null {
  if (!control.value) return null;

  const birthDate = new Date(control.value);
  const today = new Date();

  let age = today.getFullYear() - birthDate.getFullYear();

  const monthDiff = today.getMonth() - birthDate.getMonth();

  if (
    monthDiff < 0 ||
    (monthDiff === 0 && today.getDate() < birthDate.getDate())
  ) {
    age--;
  }

  return age >= 18 ? null : { underAge: true };
}

export function passwordMatchValidator(
  group: AbstractControl
): ValidationErrors | null {
  const password = group.get('password')?.value;
  const repeatedPassword = group.get('repeatedPassword')?.value;

  if (!password || !repeatedPassword) {
    return null;
  }

  return password === repeatedPassword
    ? null
    : { passwordMismatch: true };
}

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './register.html',
  styleUrl: './register.css'
})
export class Register {
  blockClipboardAction($event: ClipboardEvent) {
    $event.preventDefault();
  }

  userRole: 'cliente' | 'vendedor' = 'cliente';

  form: FormGroup;

  serverError: string | null = null;
  loading = false;
  registrationSuccess = false;
  showCancelConfirmation = false;
  showPassword = false;
  get passwordChecks() {
    const pwdControl = this.form?.get('password');
    const pwd = pwdControl?.value || '';
    const showErrorStates = this.submitAttempted || pwd.length > 0;

    return [
      { label: 'Mínimo de 12 caracteres', met: pwd.length >= 12, showErrors: showErrorStates },
      { label: 'Al menos una letra mayúscula', met: /[A-Z]/.test(pwd), showErrors: showErrorStates },
      { label: 'Al menos una letra minúscula', met: /[a-z]/.test(pwd), showErrors: showErrorStates },
      { label: 'Al menos un número', met: /\d/.test(pwd), showErrors: showErrorStates },
      { label: 'Al menos un carácter especial', met: /[^A-Za-z0-9]/.test(pwd), showErrors: showErrorStates },
    ];
  }
  submitAttempted = false;

  clientAvatars = CLIENT_AVATARS.map((path) => path.replace(/^\/+/, ''));
  sellerAvatars = SELLER_AVATARS.map((path) => path.replace(/^\/+/, ''));
  selectedAvatar = this.clientAvatars[0];

  categories: Category[] = [];
  categoriesLoading = true;
  categoriesError = false;

  constructor(
    private readonly fb: FormBuilder,
    private readonly router: Router,
    private readonly authService: AuthService,
    private readonly productsService: ProductsService,
    private readonly changeDetector: ChangeDetectorRef
  ) {
    this.form = this.createForm();
    this.loadCategories();
  }

  private loadCategories(): void {
    this.productsService.getCategories()
      .pipe(takeUntilDestroyed())
      .subscribe({
        next: (categories) => {
          this.categories = [...categories].sort((first, second) => {
            const firstIsOther = first.name.trim().toLocaleLowerCase() === 'otros';
            const secondIsOther = second.name.trim().toLocaleLowerCase() === 'otros';
            return Number(firstIsOther) - Number(secondIsOther);
          });
          this.categoriesLoading = false;
          this.form.get('defaultCategory')?.setValue(this.categories[0]?.name ?? '');
        },
        error: () => {
          this.categoriesLoading = false;
          this.categoriesError = true;
        }
      });
  }

  setRole(role: 'cliente' | 'vendedor'): void {
    this.userRole = role;

    this.serverError = null;

    this.selectedAvatar =
      role === 'cliente'
        ? this.clientAvatars[0]
        : this.sellerAvatars[0];

    this.form = this.createForm();
  }

  selectAvatar(avatarPath: string): void {
    this.selectedAvatar = avatarPath;
  }

  get currentAvatarList(): string[] {
    return this.userRole === 'cliente'
      ? this.clientAvatars
      : this.sellerAvatars;
  }

  createForm(): FormGroup {
    const phonePattern = /^\d{9}$/;

    const strongPasswordPattern =
      /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[^A-Za-z0-9])\S+$/;

    if (this.userRole === 'cliente') {

      return this.fb.group({
        firstName: [
          '',
          [
            Validators.required,
            Validators.minLength(2),
            Validators.maxLength(25)
          ]
        ],

        lastName: [
          '',
          [
            Validators.required,
            Validators.minLength(2),
            Validators.maxLength(50)
          ]
        ],

        birthDate: [
          '',
          [
            Validators.required,
            isAdultValidator
          ]
        ],

        nationalId: [
          '',
          [
            Validators.required,
            Validators.maxLength(20)
          ]
        ],

        email: [
          '',
          [
            Validators.required,
            Validators.email,
            Validators.maxLength(120)
          ]
        ],

        phone: [
          '',
          [
            Validators.pattern(phonePattern)
          ]
        ],

        customerType: [
          'STANDARD',
          [
            Validators.required
          ]
        ],

        password: [
          '',
          [
            Validators.required,
            Validators.minLength(12),
            Validators.maxLength(128),
            Validators.pattern(strongPasswordPattern)
          ]
        ],

        repeatedPassword: [
          '',
          [
            Validators.required
          ]
        ]

      }, {
        validators: passwordMatchValidator
      });

    } else {

      return this.fb.group({
        firstName: [
          '',
          [
            Validators.required,
            Validators.minLength(2),
            Validators.maxLength(25)
          ]
        ],

        lastName: [
          '',
          [
            Validators.required,
            Validators.minLength(2),
            Validators.maxLength(50)
          ]
        ],

        defaultCategory: [
          this.categories[0]?.name ?? '',
          [
            Validators.required
          ]
        ],

        businessName: [
          '',
          [
            Validators.required,
            Validators.minLength(2),
            Validators.maxLength(100)
          ]
        ],

        nationalId: [
          '',
          [
            Validators.required,
            Validators.maxLength(20)
          ]
        ],

        email: [
          '',
          [
            Validators.required,
            Validators.email,
            Validators.maxLength(120)
          ]
        ],

        phone: [
          '',
          [
            Validators.pattern(phonePattern)
          ]
        ],

        password: [
          '',
          [
            Validators.required,
            Validators.minLength(12),
            Validators.maxLength(128),
            Validators.pattern(strongPasswordPattern)
          ]
        ],

        repeatedPassword: [
          '',
          [
            Validators.required
          ]
        ]

      }, {
        validators: passwordMatchValidator
      });
    }
  }

  onSubmit(): void {
    this.submitAttempted = true;
    this.serverError = null;

    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.loading = true;

    if (this.userRole === 'cliente') {

      const payload = {
        firstName: this.form.value.firstName,
        lastName: this.form.value.lastName,
        birthDate: this.form.value.birthDate,
        nationalId: this.form.value.nationalId,
        email: this.form.value.email,
        phone: this.form.value.phone || null,
        customerType: this.form.value.customerType,
        profileImage: this.selectedAvatar,
        password: this.form.value.password,
        repeatedPassword: this.form.value.repeatedPassword
      };

      this.authService.registerClient(payload)
        .pipe(
          timeout({ each: 10000 }),
          finalize(() => this.finishRegistrationRequest())
        )
        .subscribe({
          next: () => {
            this.registrationSuccess = true;
          },
          error: (err) => this.showRegistrationError(err)
        });

    } else {

      const payload = {
        firstName: this.form.value.firstName,
        lastName: this.form.value.lastName,
        defaultCategory: this.form.value.defaultCategory,
        businessName: this.form.value.businessName,
        nationalId: this.form.value.nationalId,
        email: this.form.value.email,
        phone: this.form.value.phone || null,
        profileImage: this.selectedAvatar,
        password: this.form.value.password,
        repeatedPassword: this.form.value.repeatedPassword
      };

      this.authService.registerSeller(payload)
        .pipe(
          timeout({ each: 10000 }),
          finalize(() => this.finishRegistrationRequest())
        )
        .subscribe({
          next: () => {
            this.registrationSuccess = true;
          },
          error: (err) => this.showRegistrationError(err)
        });
    }
  }

  private showRegistrationError(error: unknown): void {
    this.serverError = this.getRegistrationErrorMessage(error);
    this.changeDetector.markForCheck();
    this.scrollToServerError();
  }

  private finishRegistrationRequest(): void {
    this.loading = false;
    this.changeDetector.markForCheck();
  }

  get isPasswordSecurityError(): boolean {
    const message = this.serverError?.toLocaleLowerCase() ?? '';
    return message.includes('contraseña') &&
      (message.includes('filtraciones') || message.includes('comprobar la seguridad'));
  }

  cancelRegistration(): void {
    if (this.loading) {
      return;
    }

    this.showCancelConfirmation = true;
    this.changeDetector.markForCheck();
  }

  dismissCancelConfirmation(): void {
    this.showCancelConfirmation = false;
    this.changeDetector.markForCheck();
  }

  confirmCancelRegistration(): void {
    this.showCancelConfirmation = false;

    void this.router.navigate(['/login'], {
      state: {
        registrationCancelled: true
      }
    });
  }

  finishRegistration(): void {
    void this.router.navigate(['/']);
  }

  private getRegistrationErrorMessage(error: unknown): string {
    if (error instanceof Error && error.name === 'TimeoutError') {
      return 'No se ha podido comprobar la seguridad de la contraseña. Inténtalo de nuevo más tarde.';
    }

    if (error instanceof HttpErrorResponse) {
      const serverMessage = this.getServerMessage(error.error);
      if (serverMessage) {
        return serverMessage;
      }

      if (error.status === 409) {
        return 'No se ha podido completar el registro. Comprueba los datos introducidos e inténtalo de nuevo.';
      }

      return 'No se ha podido completar el registro. Inténtalo de nuevo más tarde.';
    }

    return 'No se ha podido completar el registro. Inténtalo de nuevo más tarde.';
  }

  private getServerMessage(errorBody: unknown): string | null {
    if (typeof errorBody === 'object' && errorBody !== null && 'message' in errorBody) {
      const message = errorBody.message;
      return typeof message === 'string' && message.trim() ? message : null;
    }

    return typeof errorBody === 'string' && errorBody.trim() ? errorBody : null;
  }

  private getEmailRegistrationError(error: unknown): string | null {
    if (!(error instanceof HttpErrorResponse) || error.status !== 409) {
      return null;
    }

    const message = this.getServerMessage(error.error);
    return message?.toLowerCase().includes('email')
      ? message
      : 'No se ha podido completar el registro. Comprueba los datos introducidos e inténtalo de nuevo';
  }

  private scrollToServerError(): void {
    setTimeout(() => {
      const selector = this.isPasswordSecurityError
        ? '#password-server-error'
        : '.server-error-banner';

      document.querySelector<HTMLElement>(selector)?.scrollIntoView({
        behavior: 'smooth',
        block: 'center'
      });
    });
  }
}
