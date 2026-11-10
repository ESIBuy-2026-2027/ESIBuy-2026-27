import { HttpErrorResponse, HttpEvent, HttpInterceptorFn, HttpResponse } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, tap, throwError } from 'rxjs';
import { AuthService } from '../services/auth.service';
import { environment } from '../../../environments/environment';

const SESSION_ENDPOINTS = new Set([
  '/api/users/login',
  '/api/users/me',
  '/api/users/logout',
]);

let currentCsrfToken: string | null = null;

export const authInterceptor: HttpInterceptorFn = (request, next) => {
  const authService = inject(AuthService);
  const router = inject(Router);
  const requestPath = request.url.split('?')[0];

  const baseApi = (typeof window !== 'undefined' && window.localStorage?.getItem('esibuy_api_url')) || environment.apiUrl;
  let modifiedRequest = request;

  if (baseApi && request.url.startsWith('/api')) {
    modifiedRequest = modifiedRequest.clone({
      url: `${baseApi}${request.url}`,
      withCredentials: true,
    });
  }

  if (currentCsrfToken && !modifiedRequest.headers.has('X-XSRF-TOKEN') && !modifiedRequest.headers.has('X-CSRF-TOKEN')) {
    modifiedRequest = modifiedRequest.clone({
      headers: modifiedRequest.headers.set('X-XSRF-TOKEN', currentCsrfToken),
    });
  }

  return next(modifiedRequest).pipe(
    tap((event: HttpEvent<unknown>) => {
      if (event instanceof HttpResponse) {
        const csrf = event.headers.get('X-CSRF-TOKEN') || event.headers.get('X-XSRF-TOKEN');
        if (csrf) {
          currentCsrfToken = csrf;
        }
      }
    }),
    catchError((error: unknown) => {
      if (error instanceof HttpErrorResponse
        && error.status === 401
        && requestPath.startsWith('/api/')
        && !SESSION_ENDPOINTS.has(requestPath)) {
        authService.clearCurrentUser();
        void router.navigate(['/'], { queryParams: { expired: 'true' } });
      }

      return throwError(() => error);
    })
  );
};
