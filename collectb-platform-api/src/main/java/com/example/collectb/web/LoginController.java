package com.example.collectb.web;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * Serves the custom-styled login page for the OAuth2/PKCE authorization-code flow (used by the
 * Angular admin-portal/brand-portal apps' session login step), replacing Spring Security's
 * auto-generated default login form. Wired in as the {@code loginPage} in
 * {@code AuthorizationServerConfig#defaultSecurityFilterChain}.
 * <p>
 * The view itself ({@code templates/login.html}) reads Spring Security's standard {@code error}
 * and {@code logout} query parameters to show a "bad credentials" or "you have been logged out"
 * message, matching the behaviour of the page it replaces.
 */
@Controller
public class LoginController {

	@GetMapping("/login")
	public String login() {
		return "login";
	}

	/**
	 * Plain informational response for the app's bare root. This backend is a pure JSON API +
	 * OAuth2/OIDC Authorization Server with no server-rendered UI of its own - the real UI is the
	 * separate Angular admin-portal/brand-portal apps. Without this mapping, a visitor who opens
	 * {@code /} directly (rather than going through one of the Angular apps) has that URL saved by
	 * Spring Security's {@code RequestCache} and is sent right back to it after a successful
	 * login - since {@code /} would otherwise have no controller mapping, that produces a 404
	 * rendered as the Whitelabel Error Page immediately after login (see
	 * {@code AuthorizationServerConfig#defaultSecurityFilterChain}'s
	 * {@code defaultSuccessUrl("/", false)}). This mapping requires authentication like the rest
	 * of {@code anyRequest().authenticated()}, so an unauthenticated visitor is still redirected
	 * to {@code /login} first, same as before.
	 */
	@GetMapping(value = "/", produces = MediaType.TEXT_PLAIN_VALUE)
	@ResponseBody
	public String root() {
		return "Collectb Platform API is running. This backend has no UI of its own - "
				+ "use the Angular admin-portal or brand-portal application.";
	}

}
