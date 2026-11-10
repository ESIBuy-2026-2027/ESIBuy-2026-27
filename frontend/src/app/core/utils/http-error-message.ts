import { HttpErrorResponse } from '@angular/common/http';

/**
 * Returns a shared message for HTTP statuses whose meaning is consistent across the app.
 * Callers can override a message when the operation needs more specific wording.
 * Operation-specific statuses (for example, a product or address not found) stay local.
 */
export function httpErrorMessage(
  error: unknown,
  overrides: Partial<Record<number, string>> = {},
): string | null {
  const commonMessages: Record<number, string> = {
    0: 'No se pudo conectar con el servidor. Comprueba tu conexión e inténtalo de nuevo.',
    401: 'Tu sesión ha caducado. Inicia sesión de nuevo.',
  };

  let status: number | null = null;
  if (typeof error === 'number') {
    status = error;
  } else if (error instanceof HttpErrorResponse) {
    status = error.status;
  }

  if (status === null) return null;
  return overrides[status] ?? commonMessages[status] ?? null;
}
