import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService, Role } from './auth.service';

// Wrong role or not logged in: send the user to their own home page (or /login).
export const roleGuard = (role: Role): CanActivateFn => () => {
  const auth = inject(AuthService);
  return auth.role() === role || inject(Router).parseUrl(auth.homeUrl());
};
