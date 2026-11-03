import { HttpErrorResponse } from '@angular/common/http';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute } from '@angular/router';
import { of, throwError } from 'rxjs';
import { vi } from 'vitest';
import { AuthService } from '../../../core/services/auth.service';
import { PasswordRecovery } from './password-recovery';

describe('PasswordRecovery', () => {
  let fixture: ComponentFixture<PasswordRecovery>;
  let component: PasswordRecovery;
  let confirmPasswordReset: ReturnType<typeof vi.fn>;

  beforeEach(async () => {
    confirmPasswordReset = vi.fn(() => of({ message: '' }));

    await TestBed.configureTestingModule({
      imports: [PasswordRecovery],
      providers: [
        {
          provide: ActivatedRoute,
          useValue: {
            snapshot: {
              queryParamMap: {
                get: (key: string) => key === 'token' ? 'test-token' : null,
              },
            },
          },
        },
        {
          provide: AuthService,
          useValue: {
            requestPasswordReset: vi.fn(() => of({ message: '' })),
            validatePasswordResetToken: vi.fn(() => of({ message: '' })),
            confirmPasswordReset,
          },
        },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(PasswordRecovery);
    component = fixture.componentInstance;
    component.step = 'password';
    fixture.detectChanges();
  });

  it('renders the successful token message as a separate block above the form', () => {
    component.successMessage = 'El enlace es válido. Ya puedes establecer una nueva contraseña.';
    fixture.detectChanges();

    const message = (fixture.nativeElement as HTMLElement).querySelector('.message-success');
    expect(message?.textContent?.trim()).toBe(component.successMessage);
    expect(getComputedStyle(message as HTMLElement).display).toBe('block');
  });

  it('uses the registration minimum-length password message', () => {
    const password = component.resetForm.controls.newPassword;
    password.markAsTouched();
    password.setValue('short');
    fixture.detectChanges();

    expect((fixture.nativeElement as HTMLElement).textContent).toContain('Debe tener al menos 12 caracteres');
  });

  it('uses the registration required password message', () => {
    const password = component.resetForm.controls.newPassword;
    password.markAsTouched();
    fixture.detectChanges();

    expect((fixture.nativeElement as HTMLElement).textContent).toContain('La contraseña es obligatoria');
  });

  it('uses the registration password complexity message', () => {
    const password = component.resetForm.controls.newPassword;
    password.setValue('abcdefghijkl');
    password.markAsTouched();
    fixture.detectChanges();

    expect((fixture.nativeElement as HTMLElement).textContent)
      .toContain('La contraseña debe tener mayúscula, minúscula, número y un carácter especial');
  });

  it('shows a separate validation message for passwords longer than 128 characters', () => {
    const password = component.resetForm.controls.newPassword;
    password.setValue(`Aa1!${'a'.repeat(125)}`);
    password.markAsTouched();
    fixture.detectChanges();

    expect((fixture.nativeElement as HTMLElement).textContent)
      .toContain('La contraseña no puede superar los 128 caracteres');
  });

  it.each([
    {
      status: 400,
      message: 'La contraseña elegida aparece en filtraciones conocidas. Elige otra contraseña.',
    },
    {
      status: 503,
      message: 'No se ha podido comprobar la seguridad de la contraseña. Inténtalo de nuevo más tarde.',
    },
  ])('muestra el mensaje del servidor al restablecer la contraseña con HTTP $status', ({ status, message }) => {
    confirmPasswordReset.mockReturnValue(
      throwError(() => new HttpErrorResponse({
        status,
        error: { message },
      })),
    );

    component.resetForm.patchValue({
      token: 'test-token',
      newPassword: 'Password123!',
      confirmPassword: 'Password123!',
    });

    component.confirmPasswordReset();
    fixture.detectChanges();

    const alert = fixture.nativeElement.querySelector('[role="alert"]');

    expect(component.errorMessage).toBe(message);
    expect(alert).not.toBeNull();
    expect(alert.textContent).toContain(message);
    expect(component.step).toBe('password');
    expect(component.loading).toBe(false);
  });

  it.each(['copy', 'cut', 'paste'])('blocks %s on both password fields', (eventName) => {
    const inputs = [
      fixture.nativeElement.querySelector('#new-password') as HTMLInputElement,
      fixture.nativeElement.querySelector('#confirm-password') as HTMLInputElement,
    ];

    for (const input of inputs) {
      const event = new Event(eventName, { cancelable: true });
      input.dispatchEvent(event);
      expect(event.defaultPrevented).toBe(true);
    }
  });
});