package com.example.collectb.identity.config;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.OAuth2AuthorizationServerConfiguration;
import org.springframework.security.config.annotation.web.configurers.oauth2.server.authorization.OAuth2AuthorizationServerConfigurer;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.jackson.SecurityJacksonModules;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.authorization.JdbcOAuth2AuthorizationConsentService;
import org.springframework.security.oauth2.server.authorization.JdbcOAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationConsentService;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.client.JdbcRegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;

import com.example.collectb.identity.security.PlatformUserDetailsService;
import com.example.collectb.identity.security.PlatformUserPrincipal;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;

import tools.jackson.databind.JacksonModule;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.jsontype.BasicPolymorphicTypeValidator;

/**
 * Spring Authorization Server wiring, per design doc section 2.2 "Identity Implementation
 * Milestone" (OAuth2/OIDC-compatible access tokens) and 2.2.3 "Token and Session Design".
 * <p>
 * Two filter chains, per the standard Spring Authorization Server sample layout:
 * <ol>
 *   <li>{@code authorizationServerSecurityFilterChain} (order 1) - protects the AS endpoints
 *       themselves ({@code /oauth2/authorize}, {@code /oauth2/token}, {@code /oauth2/jwks},
 *       OIDC discovery/userinfo, etc.), redirecting unauthenticated HTML requests to
 *       {@code /login} so the authorization_code+PKCE flow can present a login form.</li>
 *   <li>{@code defaultSecurityFilterChain} (order 2) - protects everything else (the login page
 *       itself, and future resource/business API endpoints) with form login.</li>
 * </ol>
 * Registered clients, authorizations, and authorization consents are persisted via Spring
 * Authorization Server's own JDBC implementations against the {@code oauth2_*} tables from
 * {@code db/changelog/changes/004-oauth2-authorization-server.xml}.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class AuthorizationServerConfig {

	@Value("${collectb.oauth2.issuer-uri}")
	private String issuerUri;

	@Bean
	@Order(1)
	public SecurityFilterChain authorizationServerSecurityFilterChain(HttpSecurity http) throws Exception {
		http.oauth2AuthorizationServer(authorizationServer -> authorizationServer.oidc(Customizer.withDefaults()));

		OAuth2AuthorizationServerConfigurer authorizationServerConfigurer = http
				.getConfigurer(OAuth2AuthorizationServerConfigurer.class);
		http.securityMatcher(authorizationServerConfigurer.getEndpointsMatcher());

		// Without this, an anonymous request reaches the OAuth2 endpoint filters directly (they
		// require a genuinely authenticated Authentication, not just an AnonymousAuthenticationToken)
		// and fails with a generic "invalid_request: principal" OAuth2 error instead of triggering
		// the AuthenticationException our entry point below redirects to /login for.
		http.authorizeHttpRequests(authorize -> authorize.anyRequest().authenticated());

		// Redirects any unauthenticated hit on these OAuth2 endpoints (in practice, only ever
		// /oauth2/authorize - /oauth2/token authenticates via OAuth2ClientAuthenticationFilter's
		// PKCE-based client auth before AuthorizationFilter even runs, so it never reaches this
		// entry point) straight to the login page. Deliberately unconditional (no
		// MediaTypeRequestMatcher/content-negotiation gate): that gate was found to
		// intermittently fail to match the very first /oauth2/authorize request of a session,
		// silently falling back to the default Http403ForbiddenEntryPoint - which Spring Boot's
		// error handling then turns into an internal forward to /error, corrupting the
		// RequestCache's saved request (it ends up pointing at /error instead of
		// /oauth2/authorize, so after logging in the browser lands on a blank error page instead
		// of continuing the authorization-code flow).
		http.exceptionHandling(exceptions -> exceptions.authenticationEntryPoint(new LoginUrlAuthenticationEntryPoint("/login")));

		// The token endpoint is called directly by public (non-browser-session) clients using
		// PKCE instead of a client secret, so there is no CSRF token available to present; PKCE's
		// code_verifier/code_challenge pairing is itself the protection against code interception
		// and replay, matching the standard Spring Authorization Server sample configuration.
		http.csrf(csrf -> csrf.ignoringRequestMatchers(authorizationServerConfigurer.getEndpointsMatcher()));

		return http.build();
	}

	@Bean
	@Order(2)
	public SecurityFilterChain defaultSecurityFilterChain(HttpSecurity http,
			JwtAuthenticationConverter jwtAuthenticationConverter) throws Exception {
		http.authorizeHttpRequests(authorize -> authorize
				.requestMatchers("/actuator/health").permitAll()
				.requestMatchers("/api/v1/auth/register", "/api/v1/auth/login").permitAll()
				// The custom login page (GET to render, POST to submit) must be reachable by an
				// unauthenticated visitor; relying solely on formLogin(...).permitAll() below was
				// found to NOT reliably exempt GET /login from the trailing anyRequest()
				// .authenticated() rule (it caused a /login -> /login redirect loop), so it is
				// listed explicitly here too.
				.requestMatchers(HttpMethod.GET, "/login").permitAll()
				.requestMatchers(HttpMethod.POST, "/login").permitAll()
				// Brand/collection/template browsing is a public catalog directory (no
				// collector account required to look, only to own); write operations still
				// require authentication + the relevant permission via @PreAuthorize.
				.requestMatchers(HttpMethod.GET, "/api/v1/brands/**").permitAll()
				// Administrator JSON API (manage brands + brand user access), consumed by the
				// Angular admin-portal's admin/brands routes: requires the browser session login
				// (formLogin below, via the PKCE flow's /login step) and one of the privileged
				// platform permissions; the exact permission needed per action is still
				// re-checked by @PreAuthorize on BrandService/RoleAssignmentAdminService, this is
				// just the coarse "may reach the admin API at all" gate.
				.requestMatchers("/api/v1/admin/**")
				.hasAnyAuthority("PLATFORM_BRAND:MANAGE", "PLATFORM_IDENTITY:MANAGE")
				.anyRequest().authenticated());
		http.csrf(csrf -> csrf.ignoringRequestMatchers("/api/v1/auth/register", "/api/v1/auth/login"));
		// Custom-styled login page (LoginController + templates/login.html in
		// collectb-platform-api) replaces Spring Security's auto-generated default form;
		// .permitAll() opens up GET /login (the page itself) and POST /login (the processing
		// URL) so an unauthenticated visitor can actually reach/submit it.
		//
		// defaultSuccessUrl("/", false) matters for the case where
		// SavedRequestAwareAuthenticationSuccessHandler has no saved request to return to (e.g. a
		// visitor opens /login directly, or the pre-login session/request cache was lost - which
		// happens whenever this app runs as multiple instances without sticky sessions, since the
		// in-memory HttpSession holding that saved request is pinned to a single instance).
		// LoginController maps GET "/" to a plain informational response (this backend is a pure
		// API + OAuth2/OIDC Authorization Server; the real UI is the Angular admin-portal app), so
		// landing there after login is harmless rather than a 404 Whitelabel Error Page. This does
		// NOT affect the normal OAuth2/PKCE browser flow (there, a saved /oauth2/authorize request
		// is always present and takes priority over this default).
		http.formLogin(form -> form.loginPage("/login").defaultSuccessUrl("/", false).permitAll());
		// Allows a plain GET to end the session (used by the Angular Brand Portal SPA, which
		// has no way to obtain/attach a CSRF token for a POST /logout - GET is a "safe" HTTP
		// method so Spring Security's CsrfFilter does not require a token for it anyway).
		http.logout(logout -> logout
				.logoutRequestMatcher(org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher
						.withDefaults().matcher(HttpMethod.GET, "/logout"))
				.logoutSuccessHandler(new org.springframework.security.web.authentication.logout.HttpStatusReturningLogoutSuccessHandler(
						org.springframework.http.HttpStatus.NO_CONTENT)));
		// Lets Bearer JWTs issued by either the direct REST login endpoint or the OAuth2/PKCE
		// flow authenticate API requests (not just the browser session from formLogin above) -
		// required for any resource-server endpoint (e.g. Brand/Catalog) guarded by
		// @PreAuthorize/hasAuthority checks against the token's roles/permissions claims.
		http.oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter)));
		return http.build();
	}

	/**
	 * Converts a validated JWT's {@code roles}/{@code permissions} claims into
	 * {@link org.springframework.security.core.GrantedAuthority} instances via
	 * {@link JwtAuthoritiesConverter}, so {@code @PreAuthorize("hasAuthority(...)")}/
	 * {@code hasRole(...)} checks work the same way for Bearer-token requests as they do for the
	 * session-based {@link PlatformUserPrincipal} authorities.
	 */
	@Bean
	public JwtAuthenticationConverter jwtAuthenticationConverter() {
		JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
		converter.setJwtGrantedAuthoritiesConverter(new JwtAuthoritiesConverter());
		return converter;
	}

	/**
	 * Explicitly wires {@link PlatformUserDetailsService} + the argon2id {@link PasswordEncoder}
	 * into a {@link DaoAuthenticationProvider}, rather than relying on Spring Boot's implicit
	 * single-{@code UserDetailsService}-bean auto-configuration. This is the single credential
	 * check used by both:
	 * <ul>
	 *   <li>the hosted {@code /login} form ({@code formLogin} in
	 *       {@link #defaultSecurityFilterChain}), which backs the authorization_code+PKCE
	 *       browser flow through {@code /oauth2/authorize}; and</li>
	 *   <li>the direct {@code /api/v1/auth/login} REST endpoint
	 *       ({@code PlatformUserAuthenticationService}).</li>
	 * </ul>
	 * Both paths end up loading the same {@link PlatformUserPrincipal}
	 * ({@link com.example.collectb.identity.security.PlatformUserPrincipal}), so role/permission
	 * claims are populated identically regardless of which flow issued the token.
	 */
	@Bean
	public AuthenticationProvider authenticationProvider(PlatformUserDetailsService userDetailsService,
			PasswordEncoder passwordEncoder) {
		DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
		provider.setPasswordEncoder(passwordEncoder);
		return provider;
	}

	@Bean
	public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration)
			throws Exception {
		return authenticationConfiguration.getAuthenticationManager();
	}

	@Bean
	public RegisteredClientRepository registeredClientRepository(JdbcTemplate jdbcTemplate) {
		return new JdbcRegisteredClientRepository(jdbcTemplate);
	}

	@Bean
	public OAuth2AuthorizationService authorizationService(JdbcTemplate jdbcTemplate,
			RegisteredClientRepository registeredClientRepository) {
		JdbcOAuth2AuthorizationService service = new JdbcOAuth2AuthorizationService(jdbcTemplate,
				registeredClientRepository);

		JsonMapper jsonMapper = oauth2AuthorizationJsonMapper();
		service.setAuthorizationRowMapper(new JdbcOAuth2AuthorizationService.JsonMapperOAuth2AuthorizationRowMapper(
				registeredClientRepository, jsonMapper));
		service.setAuthorizationParametersMapper(
				new JdbcOAuth2AuthorizationService.JsonMapperOAuth2AuthorizationParametersMapper(jsonMapper));
		return service;
	}

	/**
	 * The default {@code JsonMapper} used by {@link JdbcOAuth2AuthorizationService} only trusts a
	 * fixed allowlist of Spring Security/Authorization Server classes when (de)serializing the
	 * {@code Authentication} stored alongside each authorization code/token (a
	 * {@link tools.jackson.databind.jsontype.PolymorphicTypeValidator} safeguard against
	 * deserializing arbitrary attacker-controlled classes). Since the hosted login form's
	 * principal is our own {@link PlatformUserPrincipal}, it must be added to that allowlist (via
	 * {@link PlatformUserPrincipalMixin}), or every authorization_code+PKCE exchange fails with
	 * "Could not resolve type id ... denied resolution" as soon as the authorization row is read
	 * back from {@code oauth2_authorization}.
	 */
	private static JsonMapper oauth2AuthorizationJsonMapper() {
		ClassLoader classLoader = AuthorizationServerConfig.class.getClassLoader();
		List<JacksonModule> securityModules = SecurityJacksonModules.getModules(classLoader,
				BasicPolymorphicTypeValidator.builder()
						.allowIfSubType("com.example.collectb.identity.")
						// JDK's Set.copyOf/List.copyOf return internal immutable collection
						// implementations (e.g. java.util.ImmutableCollections$Set12) as the
						// concrete runtime type of PlatformUserPrincipal's roleNames/
						// permissionKeys/scopeGrants collections, which also need allowing.
						.allowIfSubType("java.util."));
		return JsonMapper.builder()
				.addModules(securityModules)
				.addMixIn(PlatformUserPrincipal.class, PlatformUserPrincipalMixin.class)
				.build();
	}

	@Bean
	public OAuth2AuthorizationConsentService authorizationConsentService(JdbcTemplate jdbcTemplate,
			RegisteredClientRepository registeredClientRepository) {
		return new JdbcOAuth2AuthorizationConsentService(jdbcTemplate, registeredClientRepository);
	}

	@Bean
	public AuthorizationServerSettings authorizationServerSettings() {
		return AuthorizationServerSettings.builder().issuer(issuerUri).build();
	}

	/**
	 * Generates a fresh in-memory RSA keypair at startup for signing JWTs. Fine for local/MVP
	 * development, but NOT production-safe: keys are lost on restart (invalidating all
	 * outstanding tokens) and there is no rotation. Before production, replace with a key
	 * loaded from a managed secret store (e.g. Vault/KMS) and support key rotation via a
	 * multi-key {@link JWKSet}.
	 */
	@Bean
	public JWKSource<SecurityContext> jwkSource() {
		KeyPair keyPair = generateRsaKey();
		RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();
		RSAPrivateKey privateKey = (RSAPrivateKey) keyPair.getPrivate();
		RSAKey rsaKey = new RSAKey.Builder(publicKey)
				.privateKey(privateKey)
				.keyID(UUID.randomUUID().toString())
				.build();
		JWKSet jwkSet = new JWKSet(rsaKey);
		return new ImmutableJWKSet<>(jwkSet);
	}

	private static KeyPair generateRsaKey() {
		try {
			KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
			keyPairGenerator.initialize(2048);
			return keyPairGenerator.generateKeyPair();
		} catch (Exception ex) {
			throw new IllegalStateException("Failed to generate RSA key pair for JWT signing", ex);
		}
	}

	@Bean
	public JwtEncoder jwtEncoder(JWKSource<SecurityContext> jwkSource) {
		return new NimbusJwtEncoder(jwkSource);
	}

	@Bean
	public JwtDecoder jwtDecoder(JWKSource<SecurityContext> jwkSource) {
		return OAuth2AuthorizationServerConfiguration.jwtDecoder(jwkSource);
	}
}
