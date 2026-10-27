import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute } from '@angular/router';
import { of } from 'rxjs';
import { vi } from 'vitest';
import { AuthService } from '../../../core/services/auth.service';
import { PasswordRecovery } from './password-recovery';

describe('PasswordRecovery', () => {
  let fixture: ComponentFixture<PasswordRecovery>;
  let component: PasswordRecovery;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [PasswordRecovery],
      providers: [
        {
          provide: ActivatedRoute,
          useValue: { snapshot: { queryParamMap: { get: () => null } } },
        },
        {
          provide: AuthService,
          useValue: {
            requestPasswordReset: vi.fn(() => of({ message: '' })),
            validatePasswordResetToken: vi.fn(() => of({ message: '' })),
            confirmPasswordReset: vi.fn(() => of({ message: '' })),
          },
        },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(PasswordRecovery);
    component = fixture.componentInstance;
    component.step = 'password';
  });

  it('renders the successful token message as a separate block above the form', () => {
    component.successMessage = 'El código es válido. Ya puedes establecer una nueva contraseña.';
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
});
