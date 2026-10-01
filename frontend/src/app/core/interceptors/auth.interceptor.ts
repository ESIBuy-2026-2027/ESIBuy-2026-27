import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';
import { AuthService } from '../services/auth.service';

const SESSION_ENDPOINTS = new Set([
  '/api/users/login',
  '/api/users/me',
  '/api/users/logout',
]);

export const authInterceptor: HttpInterceptorFn = (request, next) => {
  const authService = inject(AuthService);
  const router = inject(Router);
  const requestPath = request.url.split('?')[0];

  return next(request).pipe(
    catchError((error: unknown) => {
      if (error instanceof HttpErrorResponse
        && error.status === 401
        && requestPath.startsWith('/api/')
        && !SESSION_ENDPOINTS.has(requestPath)) {
        authService.clearCurrentUser();
        void router.navigate(['/login'], { queryParams: { expired: 'true' } });
      }

      return throwError(() => error);
    })
  );
};
