package com.example.collectb.identity.config;

import java.time.Duration;
import java.util.UUID;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.oidc.OidcScopes;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;

/**
 * Seeds the first-party PKCE public client ({@code collectb-web}) into
 * {@code oauth2_registered_client} on startup, if it does not already exist.
 * <p>
 * Registered programmatically (rather than as a raw INSERT in insert.sql) because
 * {@code client_settings}/{@code token_settings} are Spring-internal JSON documents whose exact
 * shape is an implementation detail of {@link JdbcRegisteredClientRepository} - letting
 * {@link RegisteredClientRepository#save} produce them avoids hand-maintaining that format.
 * <p>
 * Per the earlier "Authorization Code + PKCE with hosted login form" decision: no client
 * secret, PKCE required, authorization_code + refresh_token grants only.
 */
@Configuration
public class Oauth2ClientSeeder {

	private static final String CLIENT_ID = "collectb-web";

	@Bean
	public CommandLineRunner seedOauth2Client(RegisteredClientRepository registeredClientRepository) {
		return args -> {
			if (registeredClientRepository.findByClientId(CLIENT_ID) != null) {
				return;
			}

			RegisteredClient client = RegisteredClient.withId(UUID.randomUUID().toString())
					.clientId(CLIENT_ID)
					.clientAuthenticationMethod(ClientAuthenticationMethod.NONE)
					.authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
					.authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
					.redirectUri("http://127.0.0.1:8080/login/oauth2/code/collectb-web")
					.redirectUri("http://127.0.0.1:3000/callback")
					// The Angular app's redirect_uri is built from window.location.origin, which
					// is "http://localhost:3000" when the browser is pointed at localhost rather
					// than 127.0.0.1 - both hostnames resolve to the same dev server, but OAuth2
					// redirect_uri matching is an exact string comparison, so both variants must
					// be registered or authorization requests from one of the two hostnames fail
					// with "invalid_request: redirect_uri" before the user ever reaches /login.
					.redirectUri("http://localhost:3000/callback")
					.postLogoutRedirectUri("http://127.0.0.1:3000/")
					.postLogoutRedirectUri("http://localhost:3000/")
					.scope(OidcScopes.OPENID)
					.scope(OidcScopes.PROFILE)
					.scope("collectb.read")
					.scope("collectb.write")
					.clientSettings(ClientSettings.builder()
							.requireAuthorizationConsent(false)
							.requireProofKey(true)
							.build())
					.tokenSettings(TokenSettings.builder()
							.accessTokenTimeToLive(Duration.ofMinutes(15))
							.refreshTokenTimeToLive(Duration.ofDays(30))
							.reuseRefreshTokens(false)
							.build())
					.build();

			registeredClientRepository.save(client);
		};
	}
}
