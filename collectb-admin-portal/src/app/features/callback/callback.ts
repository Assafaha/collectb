import { Component, OnInit, inject } from '@angular/core';
import { Router } from '@angular/router';

/**
 * Landing point for the OAuth2 redirect_uri (http://127.0.0.1:3000/callback). By the time the
 * router activates this route, {@code AuthService.init()} (an app initializer) has already
 * finished exchanging the authorization code + PKCE verifier for an access token via
 * {@code OAuthService.tryLoginCodeFlow()} - this component only needs to send the user on to the
 * actual landing page.
 */
@Component({
  selector: 'app-callback',
  template: `<div class="callback"><p>Signing you in&hellip;</p></div>`,
  styles: [
    `
      .callback {
        display: flex;
        align-items: center;
        justify-content: center;
        min-height: 100vh;
        color: var(--cb-text-muted, #94a3b8);
        font-size: 0.95rem;
      }
    `,
  ],
})
export class Callback implements OnInit {
  private readonly router = inject(Router);

  ngOnInit(): void {
    this.router.navigateByUrl('/my-brands', { replaceUrl: true });
  }
}
