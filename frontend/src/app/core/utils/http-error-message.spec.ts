import { HttpErrorResponse } from '@angular/common/http';
import { describe, expect, it } from 'vitest';
import { httpErrorMessage } from './http-error-message';

describe('httpErrorMessage', () => {
  it('returns the shared network error message', () => {
    const error = new HttpErrorResponse({ status: 0 });
    expect(httpErrorMessage(error)).toBe(
      'No se pudo conectar con el servidor. Comprueba tu conexión e inténtalo de nuevo.',
    );
  });

  it('returns the shared expired-session message', () => {
    const error = new HttpErrorResponse({ status: 401 });
    expect(httpErrorMessage(error)).toBe('Tu sesión ha caducado. Inicia sesión de nuevo.');
  });

  it('allows operation-specific wording to override a shared status', () => {
    const error = new HttpErrorResponse({ status: 401 });
    expect(httpErrorMessage(error, { 401: 'Inicia sesión como administrador.' })).toBe(
      'Inicia sesión como administrador.',
    );
  });

  it('accepts a status code when the caller already extracted it', () => {
    expect(httpErrorMessage(0)).toBe(
      'No se pudo conectar con el servidor. Comprueba tu conexión e inténtalo de nuevo.',
    );
  });

  it('returns null for statuses whose meaning is operation-specific', () => {
    expect(httpErrorMessage(new HttpErrorResponse({ status: 404 }))).toBeNull();
    expect(httpErrorMessage(new Error('failure'))).toBeNull();
  });
});
