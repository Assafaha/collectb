import { Component, HostListener, signal } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { AuthService } from './core/auth.service';
import { CurrentUserService } from './core/current-user.service';

@Component({
  imports: [RouterOutlet, RouterLink, RouterLinkActive],
  selector: 'app-root',
  styleUrl: './app.scss',
  templateUrl: './app.html',
})
export class App {
  readonly profileOpen = signal(false);

  constructor(
    protected readonly authService: AuthService,
    protected readonly currentUser: CurrentUserService,
  ) {
    // Eagerly (but silently) load the profile when already logged in, so the "Admin" nav link's
    // visibility is correct on first paint without requiring the user to open the profile panel.
    if (this.authService.isLoggedIn()) {
      this.currentUser.ensureLoaded().subscribe();
    }
  }

  /** Loads (once, cached) and toggles the profile info panel anchored to the top-right icon. */
  toggleProfile(): void {
    if (!this.profileOpen()) {
      this.currentUser.ensureLoaded().subscribe();
    }
    this.profileOpen.update((open) => !open);
  }

  /** Closes the profile panel when clicking anywhere outside it. */
  @HostListener('document:click', ['$event'])
  onDocumentClick(event: MouseEvent): void {
    if (!this.profileOpen()) {
      return;
    }
    const target = event.target as HTMLElement;
    if (!target.closest('.profile')) {
      this.profileOpen.set(false);
    }
  }

  async logout(): Promise<void> {
    this.currentUser.clear();
    await this.authService.logout();
    // Full page reload (not router.navigate) so a guarded route the user is already sitting on
    // - where Angular's default same-URL navigation would otherwise no-op - re-evaluates from a
    // clean slate and correctly bounces back through the login flow.
    window.location.href = '/my-brands';
  }
}
