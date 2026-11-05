package com.example.collectb.identity.web;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.example.collectb.identity.entity.PlatformUser;
import com.example.collectb.identity.repository.PlatformUserRepository;
import com.example.collectb.identity.security.PlatformUserPrincipal;
import com.example.collectb.identity.service.PlatformUserAuthenticationService;
import com.example.collectb.identity.service.PlatformUserRegistrationRequest;
import com.example.collectb.identity.service.PlatformUserRegistrationService;

/**
 * MVP email/password registration and login endpoints for collector accounts, per design doc
 * section 2.2.1 "Support collector registration and login using email/password for the MVP.".
 * <p>
 * Both endpoints are permitted without authentication (see
 * {@code AuthorizationServerConfig#defaultSecurityFilterChain}). On success they return a
 * short-lived signed JWT access token (issued directly via {@link PlatformUserAuthenticationService},
 * not through the full OAuth2 authorization_code flow) suitable for calling other resource-server
 * endpoints with an {@code Authorization: Bearer <token>} header.
 */
@RestController
public class AuthController {

	private static final Logger log = LoggerFactory.getLogger(AuthController.class);

	private final PlatformUserRegistrationService registrationService;
	private final PlatformUserAuthenticationService authenticationService;
	private final PlatformUserRepository platformUserRepository;

	public AuthController(PlatformUserRegistrationService registrationService,
			PlatformUserAuthenticationService authenticationService, PlatformUserRepository platformUserRepository) {
		this.registrationService = registrationService;
		this.authenticationService = authenticationService;
		this.platformUserRepository = platformUserRepository;
	}

	@PostMapping("/api/v1/auth/register")
	public ResponseEntity<AuthResponse> register(@RequestBody RegisterRequest request) {
		PlatformUser user = registrationService.registerCollector(
				new PlatformUserRegistrationRequest(request.email(), request.displayName(), request.password()));

		PlatformUserPrincipal principal = authenticationService.authenticate(user.getEmail(), request.password());
		String accessToken = authenticationService.issueJwt(principal);

		return ResponseEntity.status(HttpStatus.CREATED).body(AuthResponse.ofToken(
				principal.getUserId(), user.getEmail(), user.getDisplayName(), principal.getActorType().name(),
				principal.getRoleNames(), accessToken));
	}

	@PostMapping("/api/v1/auth/login")
	public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest request) {
		PlatformUserPrincipal principal = authenticationService.authenticate(request.email(), request.password());
		String accessToken = authenticationService.issueJwt(principal);
		String displayName = platformUserRepository.findById(principal.getUserId())
				.map(PlatformUser::getDisplayName)
				.orElse(null);

		return ResponseEntity.ok(AuthResponse.ofToken(
				principal.getUserId(), principal.getUsername(), displayName, principal.getActorType().name(),
				principal.getRoleNames(), accessToken));
	}

	@ExceptionHandler({BadCredentialsException.class})
	public ResponseEntity<String> handleBadCredentials(BadCredentialsException ex) {
		log.warn("Login rejected: {}", ex.getMessage());
		return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid email or password");
	}

	@ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
	public ResponseEntity<String> handleBadRequest(RuntimeException ex) {
		log.warn("Request rejected: {}: {}", ex.getClass().getSimpleName(), ex.getMessage());
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex.getMessage());
	}
}
