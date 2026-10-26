import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { vi } from 'vitest';
import { EmailOtpSettingsService } from '../../core/services/email-otp-settings.service';
import { EmailOtpSettingsComponent } from './email-otp-settings';

describe('EmailOtpSettingsComponent', () => {
  let fixture: ComponentFixture<EmailOtpSettingsComponent>;
  let service: {
    getSettings: ReturnType<typeof vi.fn>;
    updateEnabled: ReturnType<typeof vi.fn>;
  };

  beforeEach(async () => {
    service = {
      getSettings: vi.fn().mockReturnValue(of({
        enabled: false,
        email: 'seller@example.com',
        totpEnabled: false,
      })),
      updateEnabled: vi.fn().mockReturnValue(of({
        enabled: true,
        email: 'seller@example.com',
        message: 'Verificación por correo activada correctamente.',
      })),
    };

    await TestBed.configureTestingModule({
      imports: [EmailOtpSettingsComponent],
      providers: [{ provide: EmailOtpSettingsService, useValue: service }],
    }).compileComponents();

    fixture = TestBed.createComponent(EmailOtpSettingsComponent);
    fixture.detectChanges();
  });

  it('shows the TOTP prerequisite and prevents email OTP activation when TOTP is disabled', () => {
    const element = fixture.nativeElement as HTMLElement;
    const button = element.querySelector<HTMLButtonElement>('.primary-button');

    expect(element.querySelector('[role="alert"]')?.textContent)
      .toContain('Activa primero la verificación en dos pasos');
    expect(button?.disabled).toBe(true);

    fixture.componentInstance.enableEmailOtp();

    expect(fixture.componentInstance.error)
      .toBe('Activa primero la verificación en dos pasos con una aplicación Authenticator.');
    expect(service.updateEnabled).not.toHaveBeenCalled();
  });

  it('allows email OTP activation when TOTP is enabled', () => {
    service.getSettings.mockReturnValue(of({
      enabled: false,
      email: 'seller@example.com',
      totpEnabled: true,
    }));
    const enabledFixture = TestBed.createComponent(EmailOtpSettingsComponent);
    enabledFixture.detectChanges();

    const button = (enabledFixture.nativeElement as HTMLElement)
      .querySelector<HTMLButtonElement>('.primary-button');

    expect(button?.disabled).toBe(false);
    enabledFixture.componentInstance.enableEmailOtp();
    expect(service.updateEnabled).toHaveBeenCalledWith(true);
    enabledFixture.destroy();
  });
});
