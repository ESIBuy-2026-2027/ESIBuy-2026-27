import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService, UserRole } from '../services/auth.service';

export const roleGuard = (requiredRole: UserRole | readonly UserRole[]): CanActivateFn => (_route, state) => {
  const user = inject(AuthService).currentUser();
  const router = inject(Router);

  if (!user) {
    return router.createUrlTree(['/'], {
      queryParams: { returnUrl: state.url },
    });
  }

  const allowedRoles = Array.isArray(requiredRole) ? requiredRole : [requiredRole];
  return allowedRoles.includes(user.role) ? true : router.createUrlTree(['/']);
};
