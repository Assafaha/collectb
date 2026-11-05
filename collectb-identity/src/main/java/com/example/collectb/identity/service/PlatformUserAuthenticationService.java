package com.example.collectb.identity.service;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.collectb.identity.entity.PlatformUser;
import com.example.collectb.identity.repository.PlatformUserRepository;
import com.example.collectb.identity.security.PlatformUserPrincipal;

@Service
public class PlatformUserAuthenticationService {

	private static final Logger log = LoggerFactory.getLogger(PlatformUserAuthenticationService.class);

	private final AuthenticationManager authenticationManager;
	private final PlatformUserRepository platformUserRepository;
	private final JwtEncoder jwtEncoder;

	@Value("${collectb.oauth2.issuer-uri}")
	private String issuerUri;

	public PlatformUserAuthenticationService(AuthenticationManager authenticationManager,
			PlatformUserRepository platformUserRepository, JwtEncoder jwtEncoder) {
		this.authenticationManager = authenticationManager;
		this.platformUserRepository = platformUserRepository;
		this.jwtEncoder = jwtEncoder;
	}

	@Transactional
	public PlatformUserPrincipal authenticate(String email, String rawPassword) {
		log.debug("Authenticating user email={}", email);
		Authentication authentication;
		try {
			authentication = authenticationManager.authenticate(
					new UsernamePasswordAuthenticationToken(email, rawPassword));
		} catch (RuntimeException ex) {
			// Never log rawPassword; the exception type/message alone (e.g. "Bad credentials",
			// "User is disabled") is enough to diagnose why a login attempt was rejected.
			log.warn("Authentication failed for email={}: {}: {}", email, ex.getClass().getSimpleName(),
					ex.getMessage());
			throw ex;
		}
		PlatformUserPrincipal principal = (PlatformUserPrincipal) authentication.getPrincipal();
		PlatformUser user = platformUserRepository.findByEmail(email)
				.orElseThrow(() -> new IllegalStateException("No platform_user for email " + email));
		user.setLastLoginAt(Instant.now());
		platformUserRepository.save(user);
		log.info("Authenticated user email={} userId={} roles={}", email, principal.getUserId(),
				principal.getRoleNames());
		return principal;
	}

	public String issueJwt(PlatformUserPrincipal principal) {
		Instant now = Instant.now();
		List<Map<String, Object>> scopeGrants = principal.getScopeGrants().stream()
				.map(grant -> Map.<String, Object>of(
						"role", grant.roleName(),
						"scope_type", grant.scopeType(),
						"scope_id", grant.scopeId() != null ? grant.scopeId().toString() : ""))
				.toList();
		JwtClaimsSet claims = JwtClaimsSet.builder()
				.issuer(issuerUri)
				.subject(principal.getUserId().toString())
				.issuedAt(now)
				.expiresAt(now.plusSeconds(900))
				.claim("user_id", principal.getUserId().toString())
				.claim("actor_type", principal.getActorType().name())
				.claim("roles", principal.getRoleNames())
				.claim("permissions", principal.getPermissionKeys())
				.claim("auth_strength", "pwd")
				.claim("mfa_required", principal.isMfaEnabled())
				.claim("scopes", scopeGrants)
				.build();

		JwsHeader header = JwsHeader.with(SignatureAlgorithm.RS256).build();
		String token = jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
		log.debug("Issued JWT for userId={} expiresAt={}", principal.getUserId(), claims.getExpiresAt());
		return token;
	}
}
