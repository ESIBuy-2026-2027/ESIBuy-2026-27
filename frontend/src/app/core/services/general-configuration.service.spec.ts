import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { afterEach, beforeEach, describe, expect, it } from 'vitest';
import { GeneralConfigurationService } from './general-configuration.service';

describe('GeneralConfigurationService', () => {
  let service: GeneralConfigurationService;
  let httpTesting: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(GeneralConfigurationService);
    httpTesting = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpTesting.verify());

  it('loads the current general configuration', () => {
    let response: unknown;
    service.get().subscribe((configuration) => response = configuration);

    const request = httpTesting.expectOne('/api/configuracion');
    expect(request.request.method).toBe('GET');
    expect(request.request.withCredentials).toBe(true);
    request.flush({
      ivaPorcentaje: 21,
      gastosEnvio: 4.99,
      envioGratuitoDesde: 50,
      registroHabilitado: true,
    });

    expect(response).toEqual({
      ivaPorcentaje: 21,
      gastosEnvio: 4.99,
      envioGratuitoDesde: 50,
      registroHabilitado: true,
    });
  });

  it('persists changes using PUT', () => {
    const configuration = {
      ivaPorcentaje: 22,
      gastosEnvio: 5.5,
      envioGratuitoDesde: 60,
      registroHabilitado: false,
    };
    let response: unknown;
    service.update(configuration).subscribe((updated) => response = updated);

    const request = httpTesting.expectOne('/api/configuracion');
    expect(request.request.method).toBe('PUT');
    expect(request.request.body).toEqual(configuration);
    expect(request.request.withCredentials).toBe(true);
    request.flush(configuration);

    expect(response).toEqual(configuration);
  });
});
