import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { map } from 'rxjs/operators';
import { AuthService } from './auth.service';
import { CurrentUserService } from './current-user.service';

function capabilityGuard(check: (user: CurrentUserService) => boolean): CanActivateFn {
  return () => {
    const authService = inject(AuthService);
    const currentUser = inject(CurrentUserService);
    const router = inject(Router);

    if (!authService.hasValidAccessToken()) {
      authService.login();
      return false;
    }
    return currentUser
      .ensureLoaded()
      .pipe(map(() => (check(currentUser) ? true : router.parseUrl('/my-brands'))));
  };
}

/** Roles screen: PLATFORM_ADMIN or BRAND_EDITOR. */
export const rolesGuard = capabilityGuard((u) => u.canViewRoles());

/** Role permissions screen: requires PLATFORM_IDENTITY:MANAGE. */
export const rolePermissionsGuard = capabilityGuard((u) => u.canManageRolePermissions());
