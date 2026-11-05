import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { map } from 'rxjs/operators';
import { AuthService } from './auth.service';
import { CurrentUserService } from './current-user.service';

/** Restricts the admin dashboard section to callers holding PLATFORM_BRAND:MANAGE or
 *  PLATFORM_IDENTITY:MANAGE (per GET /api/v1/me). Assumes authGuard already ran first to
 *  guarantee a valid access token; redirects a non-admin back to "My Brands" rather than
 *  bouncing them into a login loop, since they may simply be a brand-scoped user. */
export const adminGuard: CanActivateFn = () => {
  const authService = inject(AuthService);
  const currentUser = inject(CurrentUserService);
  const router = inject(Router);

  if (!authService.hasValidAccessToken()) {
    authService.login();
    return false;
  }

  return currentUser
    .ensureLoaded()
    .pipe(map(() => (currentUser.isPlatformAdmin() ? true : router.parseUrl('/my-brands'))));
};
