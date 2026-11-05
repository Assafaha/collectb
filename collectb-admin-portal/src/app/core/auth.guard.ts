import { inject } from '@angular/core';
import { CanActivateFn } from '@angular/router';
import { AuthService } from './auth.service';

/** Redirects to the OAuth2 authorization endpoint if the user has no valid access token. */
export const authGuard: CanActivateFn = () => {
  const authService = inject(AuthService);
  if (authService.hasValidAccessToken()) {
    return true;
  }
  authService.login();
  return false;
};
