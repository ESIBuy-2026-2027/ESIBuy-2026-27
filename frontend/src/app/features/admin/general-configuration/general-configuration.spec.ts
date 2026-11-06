import { HttpErrorResponse } from '@angular/common/http';
import { By } from '@angular/platform-browser';
import { TestBed } from '@angular/core/testing';
import { NgForm } from '@angular/forms';
import { provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import {
  GeneralConfiguration,
  GeneralConfigurationService,
} from '../../../core/services/general-configuration.service';
import { GeneralConfigurationPage } from './general-configuration';

describe('GeneralConfigurationPage', () => {
  const configuration: GeneralConfiguration = {
    ivaPorcentaje: 21,
    gastosEnvio: 4.99,
    envioGratuitoDesde: 50,
    registroHabilitado: true,
  };

  let configurationService: {
    get: ReturnType<typeof vi.fn>;
    update: ReturnType<typeof vi.fn>;
  };

  beforeEach(async () => {
    configurationService = {
      get: vi.fn(() => of(configuration)),
      update: vi.fn((value: GeneralConfiguration) => of(value)),
    };

    await TestBed.configureTestingModule({
      imports: [GeneralConfigurationPage],
      providers: [
        provideRouter([]),
        { provide: GeneralConfigurationService, useValue: configurationService },
      ],
    }).compileComponents();
  });

  it('loads persisted settings into the form', async () => {
    const fixture = TestBed.createComponent(GeneralConfigurationPage);
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();

    expect(configurationService.get).toHaveBeenCalledOnce();
    expect(fixture.componentInstance.form).toEqual(configuration);
    expect((fixture.nativeElement.querySelector('#gastosEnvio') as HTMLInputElement).value).toBe('4.99');
  });

  it('uses the administration sidebar and supports collapsing it', () => {
    const fixture = TestBed.createComponent(GeneralConfigurationPage);
    fixture.detectChanges();
    const component = fixture.componentInstance;

    expect(fixture.nativeElement.querySelector('.admin-sidebar--shared')).not.toBeNull();
    expect(fixture.nativeElement.querySelector('a[href="/admin/configuracion"]')?.textContent.trim())
      .toBe('Configuración general');
    expect(component.sidebarCollapsed()).toBe(false);

    component.toggleSidebar();
    fixture.detectChanges();

    expect(component.sidebarCollapsed()).toBe(true);
    expect(fixture.nativeElement.querySelector('.admin-layout').classList)
      .toContain('app-sidebar-collapsed');
  });

  it('submits the updated values and shows success', () => {
    const fixture = TestBed.createComponent(GeneralConfigurationPage);
    fixture.detectChanges();
    const component = fixture.componentInstance;
    const form = fixture.debugElement.query(By.css('form')).injector.get(NgForm);
    component.form = {
      ivaPorcentaje: 22,
      gastosEnvio: 5.5,
      envioGratuitoDesde: 60,
      registroHabilitado: false,
    };
    configurationService.update.mockReturnValue(of(component.form));

    component.save(form);

    expect(configurationService.update).toHaveBeenCalledWith(component.form);
    expect(component.successMessage()).toContain('actualizado correctamente');
    expect(component.saving()).toBe(false);
  });

  it('keeps entered values and displays the server error on failure', () => {
    const fixture = TestBed.createComponent(GeneralConfigurationPage);
    fixture.detectChanges();
    const component = fixture.componentInstance;
    const form = fixture.debugElement.query(By.css('form')).injector.get(NgForm);
    component.form.gastosEnvio = 7.25;
    configurationService.update.mockReturnValue(throwError(() => new HttpErrorResponse({
      status: 400,
      error: { message: 'Los gastos de envío no pueden ser negativos' },
    })));

    component.save(form);

    expect(component.form.gastosEnvio).toBe(7.25);
    expect(component.errorMessage()).toBe('Los gastos de envío no pueden ser negativos');
    expect(component.saving()).toBe(false);
  });

  it('does not send invalid numeric values to the API', () => {
    const fixture = TestBed.createComponent(GeneralConfigurationPage);
    fixture.detectChanges();
    const component = fixture.componentInstance;
    const form = fixture.debugElement.query(By.css('form')).injector.get(NgForm);
    component.form.ivaPorcentaje = -1;

    component.save(form);

    expect(configurationService.update).not.toHaveBeenCalled();
    expect(component.errorMessage()).toContain('no negativos');
  });
});
