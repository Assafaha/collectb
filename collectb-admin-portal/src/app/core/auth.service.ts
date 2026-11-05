import { HttpClient } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { AuthConfig, OAuthService } from 'angular-oauth2-oidc';

/**
 * OAuth2 Authorization Code + PKCE configuration for the {@code collectb-web} public client
 * already seeded by {@code Oauth2ClientSeeder} (no client secret, PKCE required). All endpoint
 * URLs are deliberately *relative* (not the discovery document's absolute URLs) so every request
 * - including the initial full-page redirect to {@code /oauth2/authorize} and the code+verifier
 * exchange at {@code /oauth2/token} - goes through this app's own dev-server proxy
 * (proxy.conf.json) to the backend, avoiding any cross-origin/CORS configuration entirely.
 * {@code oidc: false} means only a plain OAuth2 access token is requested (no id_token/nonce
 * handling) - the portal only needs a Bearer token for the REST API, not browser-side identity
 * claims.
 */
const authConfig: AuthConfig = {
  issuer: 'http://localhost:8080',
  loginUrl: '/oauth2/authorize',
  tokenEndpoint: '/oauth2/token',
  redirectUri: window.location.origin + '/callback',
  clientId: 'collectb-web',
  responseType: 'code',
  scope: 'collectb.read collectb.write',
  oidc: false,
  requireHttps: false,
  showDebugInformation: true,
  // The access token is short-lived (15 min, see Oauth2ClientSeeder); without this the SPA has
  // no way to notice/renew it, so an open tab silently starts getting 401s on every API call
  // once it expires even though the user never "logged out". timeoutFactor controls how early
  // (as a fraction of the token's lifetime) the refresh fires - default 0.75 i.e. ~11 min in.
  timeoutFactor: 0.75,
};

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);

  readonly isLoggedIn = signal(false);

  constructor(private readonly oauthService: OAuthService) {
    this.oauthService.configure(authConfig);
    // For the code flow (responseType: 'code'), this transparently exchanges the refresh_token
    // (issued alongside the access token - see Oauth2ClientSeeder's REFRESH_TOKEN grant) for a
    // new access token shortly before expiry, instead of an OIDC iframe-based silent refresh.
    this.oauthService.setupAutomaticSilentRefresh();
    this.oauthService.events.subscribe((event) => {
      if (event.type === 'token_received' || event.type === 'silently_refreshed') {
        this.isLoggedIn.set(this.oauthService.hasValidAccessToken());
      }
      if (event.type === 'silent_refresh_error' || event.type === 'token_refresh_error') {
        // The refresh token itself is gone/expired/revoked (e.g. the 30-day refresh token TTL
        // lapsed, or the backend was restarted and lost its in-memory grants) - there is no way
        // to recover without a fresh login, so reflect that in isLoggedIn immediately and send
        // the user straight back through the login flow rather than leaving them on a page that
        // will just keep failing every subsequent API call.
        this.isLoggedIn.set(false);
        this.login();
      }
    });
  }

  /** Completes a pending code-flow redirect (if any) and refreshes {@link isLoggedIn}. Must be
   *  awaited before the router activates any route, so guards see accurate login state. */
  async init(): Promise<void> {
    await this.oauthService.tryLoginCodeFlow();
    this.isLoggedIn.set(this.oauthService.hasValidAccessToken());
  }

  /** Starts the Authorization Code + PKCE flow via a full-page redirect to {@code /oauth2/authorize}. */
  login(): void {
    this.oauthService.initCodeFlow();
  }

  /** Clears the SPA's local access token AND ends the backend's browser session (via
   *  {@code GET /logout}) - without the latter, a subsequent {@link login} would silently
   *  re-authenticate through the still-live session cookie instead of prompting for
   *  credentials again. */
  async logout(): Promise<void> {
    this.oauthService.logOut();
    try {
      await firstValueFrom(this.http.get('/logout', { responseType: 'text' }));
    } catch {
      // best-effort: local tokens are already cleared even if the backend call fails
    }
    this.isLoggedIn.set(false);
  }

  getAccessToken(): string | null {
    return this.oauthService.getAccessToken();
  }

  hasValidAccessToken(): boolean {
    return this.oauthService.hasValidAccessToken();
  }
}
