import { Injectable, inject, signal } from '@angular/core';
import { Observable, of } from 'rxjs';
import { catchError, tap } from 'rxjs/operators';
import { BrandPortalApiService } from './api.service';
import { Me } from './models';

const PLATFORM_ADMIN_PERMISSIONS = ['PLATFORM_BRAND:MANAGE', 'PLATFORM_IDENTITY:MANAGE'];

/** Caches the logged-in user's own profile (GET /api/v1/me) for the session, so both the
 *  top-bar profile panel and the admin-section route guard/nav-link share one fetch instead of
 *  each calling the endpoint independently. */
@Injectable({ providedIn: 'root' })
export class CurrentUserService {
  private readonly api = inject(BrandPortalApiService);

  readonly me = signal<Me | null>(null);

  /** Fetches {@code /api/v1/me} once and caches it; subsequent calls return the cached value
   *  without another HTTP request until {@link clear} is called (e.g. on logout). */
  ensureLoaded(): Observable<Me | null> {
    const cached = this.me();
    if (cached) {
      return of(cached);
    }
    return this.api.getMe().pipe(
      tap((me) => this.me.set(me)),
      catchError(() => of(null)),
    );
  }

  /** True if the current user holds a platform-level admin permission (PLATFORM_BRAND:MANAGE or
   *  PLATFORM_IDENTITY:MANAGE), i.e. may reach the admin dashboard section. */
  isPlatformAdmin(): boolean {
    const me = this.me();
    return !!me && me.permissions.some((permission) => PLATFORM_ADMIN_PERMISSIONS.includes(permission));
  }

  /** True if the current user may reach the "Users" screen: a platform admin (sees every
   *  brand's users) or a BRAND_ADMIN of at least one brand (sees only their own brand(s)'
   *  users) - anyone else has nothing to manage there. */
  canManageUsers(): boolean {
    const me = this.me();
    return !!me && (this.isPlatformAdmin() || me.roles.includes('BRAND_ADMIN'));
  }

  /** True if the user may open the read-only Roles screen: a platform admin or a BRAND_EDITOR. */
  canViewRoles(): boolean {
    const me = this.me();
    return !!me && (this.canManageRolePermissions() || me.roles.includes('BRAND_EDITOR'));
  }

  /** True if the user may map permissions to roles (PLATFORM_IDENTITY:MANAGE). */
  canManageRolePermissions(): boolean {
    const me = this.me();
    return !!me && me.permissions.includes('PLATFORM_IDENTITY:MANAGE');
  }

  hasRecentStepUp(): boolean {
    const strength = this.me()?.authStrength;
    return strength === 'mfa' || strength === 'mfa_pwd';
  }

  /** Clears the cached profile, e.g. after logout so a subsequent login re-fetches fresh data. */
  clear(): void {
    this.me.set(null);
  }
}
