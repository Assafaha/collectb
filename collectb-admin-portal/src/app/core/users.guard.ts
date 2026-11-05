import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { map } from 'rxjs/operators';
import { AuthService } from './auth.service';
import { CurrentUserService } from './current-user.service';

/** Restricts the "Users" screen to callers who have someone to manage: a platform admin (every
 *  brand) or a BRAND_ADMIN of at least one brand (their own). Mirrors adminGuard's shape, but
 *  redirects a caller with neither capability back to "My Brands" instead of a login loop. */
export const usersGuard: CanActivateFn = () => {
  const authService = inject(AuthService);
  const currentUser = inject(CurrentUserService);
  const router = inject(Router);

  if (!authService.hasValidAccessToken()) {
    authService.login();
    return false;
  }

  return currentUser
    .ensureLoaded()
    .pipe(map(() => (currentUser.canManageUsers() ? true : router.parseUrl('/my-brands'))));
};
