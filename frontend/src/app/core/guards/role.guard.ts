import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../services/auth.service';
import { Role } from '../models/user.model';

export function roleGuard(...roles: Role[]): CanActivateFn {
  return () => {
    const authService = inject(AuthService);
    const router = inject(Router);
    if (authService.hasAnyRole(...roles)) {
      return true;
    }
    return router.createUrlTree(['/designs']);
  };
}
