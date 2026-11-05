import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, throwError } from 'rxjs';
import { AuthService } from './auth.service';

/** Attaches the OAuth2 access token as a Bearer header on every call to this app's own API
 *  (proxied {@code /api/**} requests only - never on requests to other origins). Also acts as a
 *  safety net for session loss: if the backend still rejects the request as unauthenticated
 *  (expired/missing/revoked token) - e.g. automatic silent refresh (see AuthService) hasn't run
 *  yet, or itself failed - redirect to the login flow instead of leaving the page sitting there
 *  silently failing every further request. */
export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const authService = inject(AuthService);
  const token = authService.getAccessToken();
  const isApiRequest = req.url.startsWith('/api/');
  if (token && isApiRequest) {
    req = req.clone({ setHeaders: { Authorization: ('Bearer ' + token) } });
  }
  return next(req).pipe(
    catchError((error: unknown) => {
      if (isApiRequest && error instanceof HttpErrorResponse && error.status === 401) {
        authService.login();
      }
      return throwError(() => error);
    }),
  );
};